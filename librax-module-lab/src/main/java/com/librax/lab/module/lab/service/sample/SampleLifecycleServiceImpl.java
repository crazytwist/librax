package com.librax.lab.module.lab.service.sample;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.flow.engine.definition.PipelineGraphCache;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import com.librax.lab.module.flow.engine.definition.model.StepNode;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.lab.dal.dataobject.sample.*;
import com.librax.lab.module.lab.dal.mysql.sample.*;
import com.librax.lab.module.lab.dal.vo.SampleSplitReqVO;
import com.librax.lab.module.lab.dal.vo.SampleSplitResultVO;
import com.librax.lab.module.lab.enums.*;
import com.librax.lab.module.infra.framework.util.LabIdGenerator;
import com.librax.lab.module.flow.dal.mysql.pipelinedefinition.PipelineDefinitionMapper;
import com.librax.lab.module.flow.dal.mysql.pipelineexecution.PipelineExecutionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleLifecycleServiceImpl implements SampleLifecycleService {

    private final SampleInfoMapper         sampleInfoMapper;
    private final SampleStepMapper         sampleStepMapper;
    private final SampleEventMapper        sampleEventMapper;
    private final SampleRelationMapper     sampleRelationMapper;
    private final PipelineGraphCache       graphCache;
    private final LabIdGenerator           idGenerator;
    private final PipelineDefinitionMapper definitionMapper;
    private final PipelineExecutionMapper  executionMapper;

    // ================================================================
    // 流程集成
    // ================================================================

    /**
     * 样本进入流程（正式绑定入口）
     * ★ NONE 模式直接跳过
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onSampleLoaded(String sampleId, String executionId) {
        if (!isSampleEnabled(executionId)) {
            log.debug("[SampleLifecycle] NONE 模式，跳过 onSampleLoaded executionId={}", executionId);
            return;
        }
        SampleInfoDO sample = getSample(sampleId);
        if (sample == null) {
            log.warn("[SampleLifecycle] 样本不存在，跳过 sampleId={}", sampleId);
            return;
        }
        String fromStatus = sample.getStatus();
        sampleInfoMapper.updateStatusBySampleId(
                sampleId, SampleStatusEnum.LOADED.name(), executionId, null);
        recordEvent(sampleId, executionId, null,
                SampleEventTypeEnum.LOADED,
                fromStatus, SampleStatusEnum.LOADED.name(),
                null, null, null, null, null, null);
        log.info("[SampleLifecycle] 样本进入流程 sampleId={} executionId={}",
                sampleId, executionId);
    }

    /**
     * 预绑定：把样本绑到流程的 INSTRUMENT 步骤上
     *
     * ★ 改动：
     *   - NONE 模式跳过
     *   - sample_bind_nodes 不为空：存入待消费队列，等指定节点触发
     *   - sample_bind_nodes 为空：立即绑定所有 INSTRUMENT 步骤（原有行为）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void preBindSteps(List<String> sampleIds, String executionId,
                             String pipelineKey, int pipelineVersion) {
        if (!isSampleEnabled(pipelineKey, pipelineVersion)) {
            log.debug("[SampleLifecycle] NONE 模式，跳过预绑定 executionId={}", executionId);
            return;
        }

        // 直接拿 List<String>，JacksonTypeHandler 已处理反序列化
        List<String> bindNodes = definitionMapper.selectSampleBindNodes(pipelineKey, pipelineVersion);

        if (!CollectionUtils.isEmpty(bindNodes)) {
            // 延迟绑定：存入待消费队列（FIFO，逗号分隔）
            log.info("[SampleLifecycle] 延迟绑定模式 executionId={} bindNodes={} sampleIds={}",
                    executionId, bindNodes, sampleIds);
            if (!CollectionUtils.isEmpty(sampleIds)) {
                executionMapper.updatePendingSampleIds(
                        executionId, String.join(",", sampleIds));
            }
            return;
        }

        // 立即绑定（原有逻辑，完全不动）
        if (CollectionUtils.isEmpty(sampleIds)) return;

        PipelineGraph graph = graphCache.get(pipelineKey, pipelineVersion);
        List<StepNode> instrumentSteps = graph.getSteps().stream()
                .filter(n -> n.getStepType() == StepTypeEnum.INSTRUMENT)
                .toList();
        if (instrumentSteps.isEmpty()) {
            log.debug("[SampleLifecycle] 流程无 INSTRUMENT 步骤，跳过预绑定 executionId={}", executionId);
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int totalBound = 0;
        for (StepNode step : instrumentSteps) {
            int seqNo = 1;
            for (String sampleId : sampleIds) {
                if (sampleStepMapper.selectBySampleAndStep(
                        sampleId, executionId, step.getNodeId(), 1) != null) continue;
                sampleStepMapper.insert(buildStepBind(sampleId, executionId, step, seqNo++, now));
                totalBound++;
            }
        }
        log.info("[SampleLifecycle] 预绑定完成 executionId={} samples={} steps={} totalBound={}",
                executionId, sampleIds.size(), instrumentSteps.size(), totalBound);
    }

    /**
     * 步骤开始：BOUND → PROCESSING
     * ★ NONE 模式跳过
     */
    @Override
    public void onStepStarted(String sampleId, String executionId,
                              String nodeId, int attempt) {
        if (!isSampleEnabled(executionId)) return;

        SampleStepDO stepBind = sampleStepMapper.selectBySampleAndStep(
                sampleId, executionId, nodeId, attempt);
        if (stepBind == null) {
            log.debug("[SampleLifecycle] 绑定记录尚未就绪，跳过 sampleId={} nodeId={}",
                    sampleId, nodeId);
            return;
        }
        sampleStepMapper.updateStatus(sampleId, executionId, nodeId, attempt,
                SampleStepStatusEnum.PROCESSING.name());
        SampleStepDO update = new SampleStepDO();
        update.setId(stepBind.getId());
        update.setStartedAt(LocalDateTime.now());
        sampleStepMapper.updateById(update);

        sampleInfoMapper.updateStatusBySampleId(
                sampleId, SampleStatusEnum.IN_PROCESS.name(), executionId, nodeId);
        recordEvent(sampleId, executionId, nodeId,
                SampleEventTypeEnum.PROCESSING,
                null, SampleStatusEnum.IN_PROCESS.name(),
                null, null, null, null, null, null);
        log.info("[SampleLifecycle] 样本开始处理 sampleId={} nodeId={}", sampleId, nodeId);
    }

    /**
     * 步骤成功：记录结果，更新状态
     *
     * ★ 改动：
     *   - NONE 模式跳过
     *   - 当前节点在 sample_bind_nodes 里时触发延迟绑定
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onStepCompleted(String sampleId, String executionId,
                                String nodeId, int attempt,
                                Map<String, Object> outputs) {
        if (!isSampleEnabled(executionId)) return;

        // 延迟绑定触发：直接拿 List<String>，无需 parseNodeList
        List<String> bindNodes = executionMapper.selectSampleBindNodes(executionId);
        if (!CollectionUtils.isEmpty(bindNodes) && bindNodes.contains(nodeId)) {
            triggerDelayedBind(executionId, nodeId, outputs);
            if (sampleId == null) return;
        }

        // 原有逻辑，完全不动
        sampleStepMapper.updateStatus(sampleId, executionId, nodeId, attempt,
                SampleStepStatusEnum.COMPLETED.name());
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
        recordEvent(sampleId, executionId, nodeId,
                SampleEventTypeEnum.STEP_COMPLETED,
                null, null, null, null, null, null, null,
                outputs != null ? JSON.toJSONString(outputs) : null);
        log.info("[SampleLifecycle] 样本步骤完成 sampleId={} nodeId={}", sampleId, nodeId);
    }

    /**
     * 步骤失败
     * ★ NONE 模式跳过
     */
    @Override
    public void onStepFailed(String sampleId, String executionId,
                             String nodeId, int attempt) {
        if (!isSampleEnabled(executionId)) return;

        sampleStepMapper.updateStatus(sampleId, executionId, nodeId, attempt,
                SampleStepStatusEnum.FAILED.name());
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
                null, null, null, null, null, null, null, null);
        log.warn("[SampleLifecycle] 样本步骤失败 sampleId={} nodeId={}", sampleId, nodeId);
    }

    /**
     * 流程结束：样本结算
     * ★ NONE 模式跳过
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onExecutionCompleted(String sampleId, String executionId,
                                     boolean success) {
        if (!isSampleEnabled(executionId)) return;

        SampleInfoDO sample = getSample(sampleId);
        if (sample == null) return;
        String fromStatus = sample.getStatus();
        String toStatus   = success
                ? SampleStatusEnum.COMPLETED.name()
                : SampleStatusEnum.REJECTED.name();
        sampleInfoMapper.updateStatusBySampleId(sampleId, toStatus, null, null);
        recordEvent(sampleId, executionId, null,
                SampleEventTypeEnum.COMPLETED,
                fromStatus, toStatus,
                null, null, null, null, null, null);
        log.info("[SampleLifecycle] 样本流程结算 sampleId={} executionId={} success={} status={}",
                sampleId, executionId, success, toStatus);
    }

    // ================================================================
    // 样本操作（一行不动）
    // ================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String registerSample(SampleRegisterReqVO req) {
        String sampleId = req.getSampleId() != null
                ? req.getSampleId() : idGenerator.nextSampleId();
        SampleInfoDO sample = new SampleInfoDO();
        sample.setSampleId(sampleId);
        sample.setSampleType(req.getSampleType());
        sample.setSampleName(req.getSampleName());
        sample.setContainerType(req.getContainerType());
        sample.setContainerCode(req.getContainerCode());
        sample.setVolumeUl(req.getVolumeUl());
        sample.setInitialVolumeUl(req.getVolumeUl());
        sample.setConcentration(req.getConcentration());
        sample.setStatus(SampleStatusEnum.REGISTERED.name());
        sample.setDeriveType("ORIGINAL");
        sample.setGeneration(0);
        sample.setRootSampleId(sampleId);
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
        recordEvent(sampleId, null, null,
                SampleEventTypeEnum.REGISTERED,
                null, SampleStatusEnum.REGISTERED.name(),
                null, req.getLocationCode(),
                null, req.getVolumeUl(), null, null);
        log.info("[SampleLifecycle] 样本登记 sampleId={} type={} batch={}",
                sampleId, req.getSampleType(), req.getBatchNo());
        return sampleId;
    }

    @Override
    public void transferSample(String sampleId, String toLocation,
                               String toLocationDetail, String operator) {
        SampleInfoDO sample = getSample(sampleId);
        if (sample == null) return;
        String fromLocation = sample.getLocationCode();
        SampleInfoDO update = new SampleInfoDO();
        update.setId(sample.getId());
        update.setLocationCode(toLocation);
        update.setLocationDetail(toLocationDetail);
        sampleInfoMapper.updateById(update);
        recordEvent(sampleId, sample.getCurrentExecutionId(), null,
                SampleEventTypeEnum.TRANSFER,
                null, null,
                fromLocation, toLocation,
                null, null, operator, null);
        log.info("[SampleLifecycle] 样本转移 sampleId={} from={} to={}",
                sampleId, fromLocation, toLocation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SampleSplitResultVO splitSample(SampleSplitReqVO req) {
        String parentId = req.getParentSampleId();
        SampleInfoDO parent = getSample(parentId);
        if (parent == null) throw new RuntimeException("父样本不存在: " + parentId);

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

        for (SampleSplitReqVO.SplitItem split : req.getSplits()) {
            String childId = parentId + "-" + split.getLabel();
            SampleInfoDO child = new SampleInfoDO();
            child.setSampleId(childId);
            child.setParentSampleId(parentId);
            child.setRootSampleId(parent.getRootSampleId() != null
                    ? parent.getRootSampleId() : parentId);
            child.setDeriveType("SPLIT");
            child.setGeneration(parent.getGeneration() != null
                    ? parent.getGeneration() + 1 : 1);
            child.setSampleType(parent.getSampleType());
            child.setSampleName(parent.getSampleName() + "-" + split.getLabel());
            child.setContainerType(req.getContainerType() != null
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

            SampleRelationDO relation = new SampleRelationDO();
            relation.setSampleId(childId);
            relation.setRelatedSampleId(parentId);
            relation.setRelationType("SPLIT_FROM");
            relation.setQuantityUl(split.getVolumeUl());
            relation.setExecutionId(req.getExecutionId());
            relation.setNodeId(req.getNodeId());
            sampleRelationMapper.insert(relation);

            recordEvent(childId, req.getExecutionId(), req.getNodeId(),
                    SampleEventTypeEnum.REGISTERED,
                    null, SampleStatusEnum.REGISTERED.name(),
                    null, null, null, split.getVolumeUl(), null,
                    String.format("{\"splitFrom\":\"%s\",\"label\":\"%s\"}",
                            parentId, split.getLabel()));

            SampleSplitResultVO.ChildSample childResult = new SampleSplitResultVO.ChildSample();
            childResult.setSampleId(childId);
            childResult.setLabel(split.getLabel());
            childResult.setVolumeUl(split.getVolumeUl());
            childResults.add(childResult);
        }

        BigDecimal remainingVolume = parent.getVolumeUl() != null
                ? parent.getVolumeUl().subtract(totalSplitVolume) : null;
        SampleInfoDO parentUpdate = new SampleInfoDO();
        parentUpdate.setId(parent.getId());
        parentUpdate.setVolumeUl(remainingVolume);
        parentUpdate.setStatus(SampleStatusEnum.SPLIT.name());
        sampleInfoMapper.updateById(parentUpdate);

        recordEvent(parentId, req.getExecutionId(), req.getNodeId(),
                SampleEventTypeEnum.SPLIT,
                parent.getStatus(), SampleStatusEnum.SPLIT.name(),
                null, null, parent.getVolumeUl(), remainingVolume, null,
                String.format("{\"childCount\":%d,\"totalSplitUl\":%s}",
                        childResults.size(), totalSplitVolume));

        log.info("[SampleLifecycle] 样本拆分完成 parent={} children={} totalSplit={}μL remaining={}μL",
                parentId,
                childResults.stream().map(SampleSplitResultVO.ChildSample::getSampleId)
                        .collect(Collectors.toList()),
                totalSplitVolume, remainingVolume);

        SampleSplitResultVO result = new SampleSplitResultVO();
        result.setChildSamples(childResults);
        result.setTotalSplitVolumeUl(totalSplitVolume);
        return result;
    }

    // ================================================================
    // ★ 新增私有方法
    // ================================================================

    /**
     * 延迟绑定核心逻辑
     *
     * 优先级：outputs.sampleId > pendingQueue FIFO
     *
     *   场景A 扫码/循环：每次从 outputs 读当次 sampleId
     *   场景B 预登记多个：从队列按顺序消费
     *   场景C 预登记单个：队列里只有一个，取出绑定
     */
    private void triggerDelayedBind(String executionId, String nodeId,
                                    Map<String, Object> outputs) {
        log.info("[SampleLifecycle] 触发延迟绑定 executionId={} nodeId={}", executionId, nodeId);

        // 第一优先：从节点 outputs 读 sampleId（扫码、循环场景）
        String sampleId = null;
        if (outputs != null && outputs.get("sampleId") != null) {
            sampleId = outputs.get("sampleId").toString();
            log.info("[SampleLifecycle] 从节点输出读取 sampleId={} nodeId={}", sampleId, nodeId);
        }

        // 第二优先：从待消费队列 FIFO 取（预登记场景）
        if (sampleId == null) {
            String pending = executionMapper.selectPendingSampleIds(executionId);
            if (StringUtils.hasText(pending)) {
                String[] ids = pending.split(",");
                sampleId = ids[0].trim();
                // 消费第一个，剩余写回
                String remaining = ids.length > 1
                        ? Arrays.stream(ids, 1, ids.length)
                          .map(String::trim)
                          .collect(Collectors.joining(","))
                        : "";
                executionMapper.updatePendingSampleIds(executionId, remaining);
                log.info("[SampleLifecycle] 从队列消费 sampleId={} remaining=[{}] nodeId={}",
                        sampleId, remaining, nodeId);
            }
        }

        if (sampleId == null) {
            log.warn("[SampleLifecycle] 绑定节点 [{}] 完成但未找到 sampleId executionId={}",
                    nodeId, executionId);
            return;
        }

        // 正式绑定
        final String finalSampleId = sampleId;
        onSampleLoaded(finalSampleId, executionId);

        // 补做后续 INSTRUMENT 步骤预绑定
        String pipelineKey      = executionMapper.selectPipelineKey(executionId);
        Integer pipelineVersion = executionMapper.selectPipelineVersion(executionId);
        if (pipelineKey != null && pipelineVersion != null) {
            bindRemainingSteps(finalSampleId, executionId, pipelineKey, pipelineVersion);
        }
        log.info("[SampleLifecycle] 延迟绑定完成 sampleId={} executionId={}",
                finalSampleId, executionId);
    }

    /**
     * 补做预绑定：绑定尚未有记录的 INSTRUMENT 步骤
     * 排除 sample_bind_nodes 里的节点（绑定触发节点本身不算追溯步骤）
     */
    private void bindRemainingSteps(String sampleId, String executionId,
                                    String pipelineKey, int pipelineVersion) {
        PipelineGraph graph = graphCache.get(pipelineKey, pipelineVersion);
        // 直接拿 List<String>，无需 parseNodeList
        List<String> bindNodes = executionMapper.selectSampleBindNodes(executionId);
        LocalDateTime now = LocalDateTime.now();
        int[] seqRef = {1};

        graph.getSteps().stream()
                .filter(n -> n.getStepType() == StepTypeEnum.INSTRUMENT)
                .filter(n -> CollectionUtils.isEmpty(bindNodes)
                        || !bindNodes.contains(n.getNodeId()))
                .forEach(step -> {
                    if (sampleStepMapper.selectBySampleAndStep(
                            sampleId, executionId, step.getNodeId(), 1) != null) return;
                    sampleStepMapper.insert(
                            buildStepBind(sampleId, executionId, step, seqRef[0]++, now));
                });

        log.info("[SampleLifecycle] 补做预绑定完成 sampleId={} executionId={}", sampleId, executionId);
    }

    @Override
    public boolean isSampleTrackingEnabled(String executionId) {
        return isSampleEnabled(executionId);
    }

    /**
     * 判断是否启用样本逻辑（通过 executionId，读冗余字段，无需 JOIN）
     */
    private boolean isSampleEnabled(String executionId) {
        String mode = executionMapper.selectSampleMode(executionId);
        return !"NONE".equals(mode);
    }

    /**
     * 判断是否启用样本逻辑（通过 pipelineKey + version 直查）
     */
    private boolean isSampleEnabled(String pipelineKey, Integer version) {
        String mode = definitionMapper.selectSampleMode(pipelineKey, version);
        return !"NONE".equals(mode);
    }

    /**
     * 构建 SampleStepDO
     */
    private SampleStepDO buildStepBind(String sampleId, String executionId,
                                       StepNode step, int seqNo, LocalDateTime now) {
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
        bind.setSeqNo(seqNo);
        bind.setBoundAt(now);
        return bind;
    }

    // ================================================================
    // 原有私有方法（一行不动）
    // ================================================================

    private SampleInfoDO getSample(String sampleId) {
        return sampleInfoMapper.selectBySampleId(sampleId);
    }

    private void recordEvent(String sampleId, String executionId, String nodeId,
                             SampleEventTypeEnum eventType,
                             String fromStatus, String toStatus,
                             String locationFrom, String locationTo,
                             BigDecimal volumeBefore, BigDecimal volumeAfter,
                             String operator, String payload) {
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