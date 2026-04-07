package com.librax.lab.module.lab.service.sample;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.enums.StepTypeEnum;
import com.librax.lab.module.lab.dal.dataobject.sample.*;
import com.librax.lab.module.lab.dal.mysql.sample.*;
import com.librax.lab.module.lab.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleLifecycleServiceImpl implements SampleLifecycleService {

    private final SampleInfoMapper sampleInfoMapper;
    private final SampleStepMapper sampleStepMapper;
    private final SampleEventMapper sampleEventMapper;
    private final PipelineGraphCache graphCache;

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
                .collect(java.util.stream.Collectors.toList());

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
                : generateSampleId();

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