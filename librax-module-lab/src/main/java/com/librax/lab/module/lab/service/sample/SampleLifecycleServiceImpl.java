package com.librax.lab.module.lab.service.sample;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.librax.lab.framework.common.util.idgenerator.LabIdGenerator;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.enums.StepTypeEnum;
import com.librax.lab.module.lab.dal.dataobject.sample.*;
import com.librax.lab.module.lab.dal.mysql.sample.*;
import com.librax.lab.module.lab.dal.vo.SampleSplitReqVO;
import com.librax.lab.module.lab.dal.vo.SampleSplitResultVO;
import com.librax.lab.module.lab.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleLifecycleServiceImpl implements SampleLifecycleService {

    private final SampleInfoMapper sampleInfoMapper;
    private final SampleStepMapper sampleStepMapper;
    private final SampleEventMapper sampleEventMapper;
    private final SampleRelationMapper sampleRelationMapper;
    private final PipelineGraphCache graphCache;
    private final LabIdGenerator idGenerator;

    // ================================================================
    // 流程集成
    // ================================================================

    /**
     * 样本进入流程
     * - 更新 current_execution_id
     * - 状态 REGISTERED → LOADED
     * - 记录 LOADED 事件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onSampleLoaded(String sampleId, String executionId) {
        SampleInfoDO sample = getSample(sampleId);
        if (sample == null) {
            log.warn("[SampleLifecycle] 样本不存在，跳过 sampleId={}", sampleId);
            return;
        }

        String fromStatus = sample.getStatus();

        // 更新样本状态
        sampleInfoMapper.updateStatusBySampleId(
                sampleId,
                SampleStatusEnum.LOADED.name(),
                executionId, null);

        // 记录事件
        recordEvent(sampleId, executionId, null,
                SampleEventTypeEnum.LOADED,
                fromStatus, SampleStatusEnum.LOADED.name(),
                null, null, null, null, null, null);

        log.info("[SampleLifecycle] 样本进入流程 sampleId={} executionId={}",
                sampleId, executionId);
    }

    /**
     * 预绑定：根据流程定义，把样本绑到所有 INSTRUMENT 类型的步骤上
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void preBindSteps(List<String> sampleIds, String executionId,
                             String pipelineKey, int pipelineVersion) {
        PipelineGraph graph = graphCache.get(pipelineKey, pipelineVersion);

        // 找出所有需要样本的步骤（INSTRUMENT 类型）
        List<StepNode> instrumentSteps = graph.getSteps().stream()
                .filter(n -> n.getStepType() == StepTypeEnum.INSTRUMENT)
                .toList();

        if (instrumentSteps.isEmpty()) {
            log.debug("[SampleLifecycle] 流程无INSTRUMENT步骤，跳过预绑定 executionId={}",
                    executionId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int totalBound = 0;

        for (StepNode step : instrumentSteps) {
            int seqNo = 1;
            for (String sampleId : sampleIds) {
                SampleStepDO existing = sampleStepMapper.selectBySampleAndStep(
                        sampleId, executionId, step.getNodeId(), 1);
                if (existing != null) continue;

                SampleStepDO bind = new SampleStepDO();
                bind.setSampleId(sampleId);
                bind.setExecutionId(executionId);
                bind.setNodeId(step.getNodeId());
                bind.setAttempt(1);
                bind.setStepKey(step.getStepKey());
                bind.setStepType(step.getStepType().name());
                bind.setRole("INPUT");
                bind.setBindType(SampleBindTypeEnum.AUTO.name());
                bind.setStatus(SampleStepStatusEnum.BOUND.name());
                bind.setSeqNo(seqNo++);
                bind.setBoundAt(now);
                sampleStepMapper.insert(bind);
                totalBound++;
            }
        }

        log.info("[SampleLifecycle] 预绑定完成 executionId={} samples={} steps={} totalBound={}",
                executionId, sampleIds.size(), instrumentSteps.size(), totalBound);
    }

    /**
     * 步骤开始：样本状态 BOUND → PROCESSING
     */
    @Override
    public void onStepStarted(String sampleId, String executionId,
                              String nodeId, int attempt) {
        SampleStepDO stepBind = sampleStepMapper.selectBySampleAndStep(
                sampleId, executionId, nodeId, attempt);

        if (stepBind == null) {
            // 预绑定还没完成，跳过，不做即时绑定插入
            // 预绑定完成后记录自然就在了，步骤成功回调时 onStepCompleted 会处理
            log.info("[SampleLifecycle] 绑定记录尚未就绪，跳过 sampleId={} nodeId={}",
                    sampleId, nodeId);
            return;
        }

        // 预绑定已存在，更新状态 BOUND → PROCESSING
        sampleStepMapper.updateStatus(
                sampleId, executionId, nodeId, attempt,
                SampleStepStatusEnum.PROCESSING.name());

        SampleStepDO update = new SampleStepDO();
        update.setId(stepBind.getId());
        update.setStartedAt(LocalDateTime.now());
        sampleStepMapper.updateById(update);

        log.info("[SampleLifecycle] 样本开始处理 sampleId={} nodeId={}", sampleId, nodeId);

        // 更新样本主表
        sampleInfoMapper.updateStatusBySampleId(
                sampleId, SampleStatusEnum.IN_PROCESS.name(),
                executionId, nodeId);

        recordEvent(sampleId, executionId, nodeId,
                SampleEventTypeEnum.PROCESSING,
                null, SampleStatusEnum.IN_PROCESS.name(),
                null, null, null, null, null, null);
    }

    /**
     * 步骤成功：记录结果，更新状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onStepCompleted(String sampleId, String executionId,
                                String nodeId, int attempt,
                                Map<String, Object> outputs) {
        // 更新 sample_step 状态 → COMPLETED
        sampleStepMapper.updateStatus(
                sampleId, executionId, nodeId, attempt,
                SampleStepStatusEnum.COMPLETED.name());

        // 写入结果数据
        if (outputs != null && !outputs.isEmpty()) {
            SampleStepDO stepBind = sampleStepMapper.selectBySampleAndStep(
                    sampleId, executionId, nodeId, attempt);
            if (stepBind != null) {
                SampleStepDO update = new SampleStepDO();
                update.setId(stepBind.getId());
                update.setResultData(JSON.toJSONString(outputs));
                update.setFinishedAt(LocalDateTime.now());
                sampleStepMapper.updateById(update);
            }
        }

        // 记录事件
        recordEvent(sampleId, executionId, nodeId,
                SampleEventTypeEnum.STEP_COMPLETED,
                null, null,
                null, null, null, null, null,
                outputs != null ? JSON.toJSONString(outputs) : null);

        log.info("[SampleLifecycle] 样本步骤完成 sampleId={} nodeId={}",
                sampleId, nodeId);
    }

    /**
     * 步骤失败
     */
    @Override
    public void onStepFailed(String sampleId, String executionId,
                             String nodeId, int attempt) {
        sampleStepMapper.updateStatus(
                sampleId, executionId, nodeId, attempt,
                SampleStepStatusEnum.FAILED.name());

        // 更新 finished_at
        SampleStepDO stepBind = sampleStepMapper.selectBySampleAndStep(
                sampleId, executionId, nodeId, attempt);
        if (stepBind != null) {
            SampleStepDO update = new SampleStepDO();
            update.setId(stepBind.getId());
            update.setFinishedAt(LocalDateTime.now());
            sampleStepMapper.updateById(update);
        }

        recordEvent(sampleId, executionId, nodeId,
                SampleEventTypeEnum.STEP_FAILED,
                null, null,
                null, null, null, null, null, null);

        log.warn("[SampleLifecycle] 样本步骤失败 sampleId={} nodeId={}",
                sampleId, nodeId);
    }

    /**
     * 流程结束：样本结算
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onExecutionCompleted(String sampleId, String executionId,
                                     boolean success) {
        SampleInfoDO sample = getSample(sampleId);
        if (sample == null) return;

        String fromStatus = sample.getStatus();
        String toStatus = success
                ? SampleStatusEnum.COMPLETED.name()
                : SampleStatusEnum.REJECTED.name();

        // 更新样本状态，清理流程关联
        sampleInfoMapper.updateStatusBySampleId(
                sampleId, toStatus, null, null);

        // 记录事件
        recordEvent(sampleId, executionId, null,
                SampleEventTypeEnum.COMPLETED,
                fromStatus, toStatus,
                null, null, null, null, null, null);

        log.info("[SampleLifecycle] 样本流程结算 sampleId={} executionId={} " +
                        "success={} status={}",
                sampleId, executionId, success, toStatus);
    }

    // ================================================================
    // 样本操作
    // ================================================================

    /**
     * 登记新样本
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String registerSample(SampleRegisterReqVO req) {
        // 生成样本ID（如果没有传入）
        String sampleId = req.getSampleId() != null
                ? req.getSampleId()
                : idGenerator.nextSampleId();

        SampleInfoDO sample = new SampleInfoDO();
        sample.setSampleId(sampleId);
        sample.setSampleType(req.getSampleType());
        sample.setSampleName(req.getSampleName());
        sample.setContainerType(req.getContainerType());
        sample.setContainerCode(req.getContainerCode());
        sample.setVolumeUl(req.getVolumeUl());
        sample.setInitialVolumeUl(req.getVolumeUl()); // 初始体积=当前体积
        sample.setConcentration(req.getConcentration());
        sample.setStatus(SampleStatusEnum.REGISTERED.name());
        sample.setDeriveType("ORIGINAL");
        sample.setGeneration(0);
        sample.setRootSampleId(sampleId); // 原始样本 root = 自己
        sample.setBatchNo(req.getBatchNo());
        sample.setOrderNo(req.getOrderNo());
        sample.setPriority(req.getPriority() != null ? req.getPriority() : 0);
        sample.setSource(req.getSource());
        sample.setExternalId(req.getExternalId());
        sample.setLocationCode(req.getLocationCode());
        sample.setLocationDetail(req.getLocationDetail());
        sample.setCollectedAt(req.getCollectedAt());
        sample.setReceivedAt(LocalDateTime.now());
        sample.setExpireTime(req.getExpireTime());
        sample.setRemark(req.getRemark());
        sampleInfoMapper.insert(sample);

        // 记录登记事件
        recordEvent(sampleId, null, null,
                SampleEventTypeEnum.REGISTERED,
                null, SampleStatusEnum.REGISTERED.name(),
                null, req.getLocationCode(),
                null, req.getVolumeUl(),
                null, null);

        log.info("[SampleLifecycle] 样本登记 sampleId={} type={} batch={}",
                sampleId, req.getSampleType(), req.getBatchNo());

        return sampleId;
    }

    /**
     * 转移样本位置
     */
    @Override
    public void transferSample(String sampleId, String toLocation,
                               String toLocationDetail, String operator) {
        SampleInfoDO sample = getSample(sampleId);
        if (sample == null) return;

        String fromLocation = sample.getLocationCode();

        // 更新位置
        SampleInfoDO update = new SampleInfoDO();
        update.setId(sample.getId());
        update.setLocationCode(toLocation);
        update.setLocationDetail(toLocationDetail);
        sampleInfoMapper.updateById(update);

        // 记录转移事件
        recordEvent(sampleId, sample.getCurrentExecutionId(), null,
                SampleEventTypeEnum.TRANSFER,
                null, null,
                fromLocation, toLocation,
                null, null, operator, null);

        log.info("[SampleLifecycle] 样本转移 sampleId={} from={} to={}",
                sampleId, fromLocation, toLocation);
    }

    /**
     * 拆分样本
     * <p>
     * 事务内完成：
     * 1. 校验父样本存在 + 体积充足
     * 2. 创建子样本记录（继承父样本的类型、批次等属性）
     * 3. 记录谱系关系（lab_sample_relation）
     * 4. 扣减父样本体积
     * 5. 更新父样本状态为 SPLIT
     * 6. 记录事件日志
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SampleSplitResultVO splitSample(SampleSplitReqVO req) {
        String parentId = req.getParentSampleId();

        // 1. 校验父样本
        SampleInfoDO parent = getSample(parentId);
        if (parent == null) {
            throw new RuntimeException("父样本不存在: " + parentId);
        }

        // 校验体积
        BigDecimal totalSplitVolume = req.getSplits().stream()
                .map(SampleSplitReqVO.SplitItem::getVolumeUl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (parent.getVolumeUl() != null
                && parent.getVolumeUl().compareTo(totalSplitVolume) < 0) {
            throw new RuntimeException(String.format(
                    "父样本体积不足: 当前 %s μL, 需要 %s μL",
                    parent.getVolumeUl(), totalSplitVolume));
        }

        LocalDateTime now = LocalDateTime.now();
        List<SampleSplitResultVO.ChildSample> childResults = new ArrayList<>();

        // 2. 逐个创建子样本
        for (SampleSplitReqVO.SplitItem split : req.getSplits()) {
            String childId = parentId + "-" + split.getLabel();

            // 创建子样本记录
            SampleInfoDO child = new SampleInfoDO();
            child.setSampleId(childId);
            child.setParentSampleId(parentId);
            child.setRootSampleId(
                    parent.getRootSampleId() != null
                            ? parent.getRootSampleId() : parentId);
            child.setDeriveType("SPLIT");
            child.setGeneration(
                    parent.getGeneration() != null
                            ? parent.getGeneration() + 1 : 1);
            child.setSampleType(parent.getSampleType());
            child.setSampleName(parent.getSampleName() + "-" + split.getLabel());
            child.setContainerType(
                    req.getContainerType() != null
                            ? req.getContainerType() : parent.getContainerType());
            child.setVolumeUl(split.getVolumeUl());
            child.setInitialVolumeUl(split.getVolumeUl());
            child.setStatus(SampleStatusEnum.REGISTERED.name());
            child.setBatchNo(parent.getBatchNo());
            child.setOrderNo(parent.getOrderNo());
            child.setPriority(parent.getPriority());
            child.setSource("SPLIT");
            child.setLocationCode(parent.getLocationCode());
            child.setLocationDetail(parent.getLocationDetail());
            child.setCurrentExecutionId(req.getExecutionId());
            child.setReceivedAt(now);

            sampleInfoMapper.insert(child);

            // 3. 记录谱系关系
            SampleRelationDO relation = new SampleRelationDO();
            relation.setSampleId(childId);
            relation.setRelatedSampleId(parentId);
            relation.setRelationType("SPLIT_FROM");
            relation.setQuantityUl(split.getVolumeUl());
            relation.setExecutionId(req.getExecutionId());
            relation.setNodeId(req.getNodeId());
            sampleRelationMapper.insert(relation);

            // 记录子样本事件
            recordEvent(childId, req.getExecutionId(), req.getNodeId(),
                    SampleEventTypeEnum.REGISTERED,
                    null, SampleStatusEnum.REGISTERED.name(),
                    null, null,
                    null, split.getVolumeUl(),
                    null,
                    String.format("{\"splitFrom\":\"%s\",\"label\":\"%s\"}",
                            parentId, split.getLabel()));

            // 收集结果
            SampleSplitResultVO.ChildSample childResult = new SampleSplitResultVO.ChildSample();
            childResult.setSampleId(childId);
            childResult.setLabel(split.getLabel());
            childResult.setVolumeUl(split.getVolumeUl());
            childResults.add(childResult);
        }

        // 4. 扣减父样本体积
        BigDecimal remainingVolume = parent.getVolumeUl() != null
                ? parent.getVolumeUl().subtract(totalSplitVolume)
                : null;

        SampleInfoDO parentUpdate = new SampleInfoDO();
        parentUpdate.setId(parent.getId());
        parentUpdate.setVolumeUl(remainingVolume);
        parentUpdate.setStatus(SampleStatusEnum.SPLIT.name());
        sampleInfoMapper.updateById(parentUpdate);

        // 5. 记录父样本拆分事件
        recordEvent(parentId, req.getExecutionId(), req.getNodeId(),
                SampleEventTypeEnum.SPLIT,
                parent.getStatus(), SampleStatusEnum.SPLIT.name(),
                null, null,
                parent.getVolumeUl(), remainingVolume,
                null,
                String.format("{\"childCount\":%d,\"totalSplitUl\":%s}",
                        childResults.size(), totalSplitVolume));

        log.info("[SampleLifecycle] 样本拆分完成 parent={} children={} " +
                        "totalSplit={}μL remaining={}μL",
                parentId,
                childResults.stream().map(SampleSplitResultVO.ChildSample::getSampleId)
                        .collect(java.util.stream.Collectors.toList()),
                totalSplitVolume, remainingVolume);

        // 6. 构建返回
        SampleSplitResultVO result = new SampleSplitResultVO();
        result.setChildSamples(childResults);
        result.setTotalSplitVolumeUl(totalSplitVolume);
        return result;
    }

    // ================================================================
    // 私有方法
    // ================================================================

    private SampleInfoDO getSample(String sampleId) {
        return sampleInfoMapper.selectBySampleId(sampleId);
    }

    private String generateSampleId() {
        return "S" + System.currentTimeMillis()
                + String.format("%04d", new Random().nextInt(10000));
    }

    /**
     * 记录样本事件（统一入口）
     */
    private void recordEvent(String sampleId,
                             String executionId,
                             String nodeId,
                             SampleEventTypeEnum eventType,
                             String fromStatus,
                             String toStatus,
                             String locationFrom,
                             String locationTo,
                             java.math.BigDecimal volumeBefore,
                             java.math.BigDecimal volumeAfter,
                             String operator,
                             String payload) {
        SampleEventDO event = new SampleEventDO();
        event.setSampleId(sampleId);
        event.setExecutionId(executionId);
        event.setNodeId(nodeId);
        event.setEventType(eventType.name());
        event.setFromStatus(fromStatus);
        event.setToStatus(toStatus);
        event.setLocationFrom(locationFrom);
        event.setLocationTo(locationTo);
        event.setVolumeBeforeUl(volumeBefore);
        event.setVolumeAfterUl(volumeAfter);
        event.setOperator(operator != null ? operator : "SYSTEM");
        event.setOccurredAt(LocalDateTime.now());
        event.setPayload(payload);
        sampleEventMapper.insert(event);
    }
}