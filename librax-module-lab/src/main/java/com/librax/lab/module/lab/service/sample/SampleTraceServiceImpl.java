package com.librax.lab.module.lab.service.sample;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.librax.lab.module.lab.dal.dataobject.sample.*;
import com.librax.lab.module.lab.dal.mysql.sample.*;
import com.librax.lab.module.lab.service.sample.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleTraceServiceImpl implements SampleTraceService {

    private final SampleInfoMapper sampleInfoMapper;
    private final SampleRelationMapper sampleRelationMapper;
    private final SampleStepMapper sampleStepMapper;
    private final SampleEventMapper sampleEventMapper;
    private final SampleResultMapper sampleResultMapper;

    // ================================================================
    // 谱系树
    // ================================================================

    @Override
    public SampleTraceVO getFullFamilyTree(String sampleId) {
        SampleInfoDO sample = sampleInfoMapper.selectBySampleId(sampleId);
        if (sample == null) return null;

        // 找到根样本，从根开始构建整棵树
        String rootId = sample.getRootSampleId() != null
                ? sample.getRootSampleId()
                : sampleId;

        // 一次性查出根样本下所有样本（避免递归查 DB）
        List<SampleInfoDO> allSamples = sampleInfoMapper.selectByRootSampleId(rootId);
        Map<String, SampleInfoDO> sampleMap = allSamples.stream()
                .collect(Collectors.toMap(SampleInfoDO::getSampleId, s -> s));

        // 构建父子关系索引
        Map<String, List<SampleInfoDO>> childrenMap = allSamples.stream()
                .filter(s -> s.getParentSampleId() != null)
                .collect(Collectors.groupingBy(SampleInfoDO::getParentSampleId));

        // 从根节点递归构建树
        SampleInfoDO root = sampleMap.get(rootId);
        if (root == null) return null;

        return buildTreeNode(root, childrenMap);
    }

    private SampleTraceVO buildTreeNode(SampleInfoDO sample,
                                        Map<String, List<SampleInfoDO>> childrenMap) {
        SampleTraceVO node = new SampleTraceVO();
        node.setSampleId(sample.getSampleId());
        node.setSampleName(sample.getSampleName());
        node.setSampleType(sample.getSampleType());
        node.setDeriveType(sample.getDeriveType());
        node.setGeneration(sample.getGeneration());
        node.setStatus(sample.getStatus());
        node.setVolumeUl(sample.getVolumeUl());
        node.setContainerCode(sample.getContainerCode());
        node.setLocationCode(sample.getLocationCode());
        node.setCreateTime(sample.getCreateTime());

        // 递归构建子节点
        List<SampleInfoDO> children = childrenMap.get(sample.getSampleId());
        if (children != null && !children.isEmpty()) {
            node.setChildren(children.stream()
                    .map(child -> buildTreeNode(child, childrenMap))
                    .collect(Collectors.toList()));
        } else {
            node.setChildren(List.of());
        }

        return node;
    }

    // ================================================================
    // 步骤旅程
    // ================================================================

    @Override
    public List<SampleJourneyVO> getSampleJourney(String sampleId) {
        // 查样本绑定的所有步骤
        List<SampleStepDO> steps = sampleStepMapper.selectBySampleId(sampleId);
        if (steps.isEmpty()) return List.of();

        // 查该样本的所有检测结果，按 (executionId, nodeId) 分组
        List<SampleResultDO> allResults = sampleResultMapper.selectBySampleId(sampleId);
        Map<String, List<SampleResultDO>> resultMap = allResults.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getExecutionId() + ":" + r.getNodeId()));

        return steps.stream().map(step -> {
            SampleJourneyVO vo = new SampleJourneyVO();
            vo.setExecutionId(step.getExecutionId());
            vo.setNodeId(step.getNodeId());
            vo.setStepKey(step.getStepKey());
            vo.setStepType(step.getStepType());
            vo.setAttempt(step.getAttempt());
            vo.setStatus(step.getStatus());
            vo.setBindType(step.getBindType());
            vo.setDeviceId(step.getDeviceId());
            vo.setPosition(step.getPosition());
            vo.setSeqNo(step.getSeqNo());
            vo.setBoundAt(step.getBoundAt());
            vo.setStartedAt(step.getStartedAt());
            vo.setFinishedAt(step.getFinishedAt());

            // 解析 resultData JSON
            if (step.getResultData() != null) {
                try {
                    vo.setResultData(JSON.parseObject(step.getResultData(),
                            new TypeReference<Map<String, Object>>() {}));
                } catch (Exception e) {
                    log.warn("解析 resultData 失败 sampleId={} nodeId={}",
                            sampleId, step.getNodeId());
                }
            }

            // 关联结构化检测结果
            String key = step.getExecutionId() + ":" + step.getNodeId();
            List<SampleResultDO> stepResults = resultMap.getOrDefault(key, List.of());
            vo.setResults(stepResults.stream()
                    .map(this::toJourneyResult)
                    .collect(Collectors.toList()));

            return vo;
        }).collect(Collectors.toList());
    }

    private SampleJourneyResultVO toJourneyResult(SampleResultDO r) {
        SampleJourneyResultVO vo = new SampleJourneyResultVO();
        vo.setResultId(r.getResultId());
        vo.setTestItem(r.getTestItem());
        vo.setTestItemName(r.getTestItemName());
        vo.setResultValue(r.getResultValue());
        vo.setResultText(r.getResultText());
        vo.setUnit(r.getUnit());
        vo.setReferenceText(r.getReferenceText());
        vo.setIsAbnormal(r.getIsAbnormal());
        vo.setAbnormalFlag(r.getAbnormalFlag());
        vo.setReviewStatus(r.getReviewStatus());
        return vo;
    }

    // ================================================================
    // 事件时间线
    // ================================================================

    @Override
    public List<SampleTimelineVO> getSampleTimeline(String sampleId) {
        List<SampleEventDO> events = sampleEventMapper.selectBySampleId(sampleId);

        return events.stream().map(e -> {
            SampleTimelineVO vo = new SampleTimelineVO();
            vo.setEventType(e.getEventType());
            vo.setFromStatus(e.getFromStatus());
            vo.setToStatus(e.getToStatus());
            vo.setExecutionId(e.getExecutionId());
            vo.setNodeId(e.getNodeId());
            vo.setLocationFrom(e.getLocationFrom());
            vo.setLocationTo(e.getLocationTo());
            vo.setVolumeBefore(e.getVolumeBeforeUl());
            vo.setVolumeAfter(e.getVolumeAfterUl());
            vo.setDeviceId(e.getDeviceId());
            vo.setOperator(e.getOperator());
            vo.setOccurredAt(e.getOccurredAt());
            vo.setRemark(e.getRemark());

            if (e.getPayload() != null) {
                try {
                    vo.setPayload(JSON.parseObject(e.getPayload(),
                            new TypeReference<Map<String, Object>>() {}));
                } catch (Exception ex) {
                    // ignore
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    // ================================================================
    // 完整溯源报告
    // ================================================================

    @Override
    public SampleTraceReportVO getTraceReport(String sampleId) {
        SampleInfoDO sample = sampleInfoMapper.selectBySampleId(sampleId);
        if (sample == null) return null;

        SampleTraceReportVO report = new SampleTraceReportVO();

        // 1. 基本信息
        report.setSampleInfo(toSummary(sample));

        // 2. 谱系树
        report.setFamilyTree(getFullFamilyTree(sampleId));

        // 3. 步骤旅程
        List<SampleJourneyVO> journey = getSampleJourney(sampleId);
        report.setJourney(journey);

        // 4. 所有检测结果（从旅程中提取，去重）
        List<SampleJourneyResultVO> allResults = journey.stream()
                .flatMap(j -> j.getResults().stream())
                .collect(Collectors.toList());
        report.setResults(allResults);

        // 5. 事件时间线
        report.setTimeline(getSampleTimeline(sampleId));

        log.info("[SampleTrace] 溯源报告生成 sampleId={} steps={} results={} events={}",
                sampleId, journey.size(), allResults.size(),
                report.getTimeline().size());

        return report;
    }

    private SampleInfoSummaryVO toSummary(SampleInfoDO sample) {
        SampleInfoSummaryVO vo = new SampleInfoSummaryVO();
        vo.setSampleId(sample.getSampleId());
        vo.setRootSampleId(sample.getRootSampleId());
        vo.setParentSampleId(sample.getParentSampleId());
        vo.setSampleType(sample.getSampleType());
        vo.setSampleName(sample.getSampleName());
        vo.setDeriveType(sample.getDeriveType());
        vo.setGeneration(sample.getGeneration());
        vo.setStatus(sample.getStatus());
        vo.setVolumeUl(sample.getVolumeUl());
        vo.setInitialVolumeUl(sample.getInitialVolumeUl());
        vo.setContainerType(sample.getContainerType());
        vo.setContainerCode(sample.getContainerCode());
        vo.setLocationCode(sample.getLocationCode());
        vo.setBatchNo(sample.getBatchNo());
        vo.setOrderNo(sample.getOrderNo());
        vo.setPriority(sample.getPriority());
        vo.setSource(sample.getSource());
        vo.setExternalId(sample.getExternalId());
        vo.setCollectedAt(sample.getCollectedAt());
        vo.setReceivedAt(sample.getReceivedAt());
        vo.setExpireTime(sample.getExpireTime());
        vo.setCreateTime(sample.getCreateTime());
        return vo;
    }
}