package com.librax.lab.module.lab.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import com.librax.lab.module.lab.dal.vo.SampleSplitReqVO;
import com.librax.lab.module.lab.dal.vo.SampleSplitResultVO;
import com.librax.lab.module.lab.service.sample.SampleLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 样本拆分步骤执行器
 *
 * <p>根据拆分配置把一个父样本拆成多个子样本。
 *
 * <p>输入参数（从 inputMapping + params 合并后传入）：
 * <pre>
 * {
 *   "sampleId": "S001",                     // 待拆分的父样本ID（必填）
 *   "splits": [                             // 拆分配置（必填）
 *     {"label": "A", "volumeUl": 500},
 *     {"label": "B", "volumeUl": 300},
 *     {"label": "C", "volumeUl": 200}
 *   ],
 *   "containerType": "TUBE"                 // 子样本容器类型（选填，默认继承父样本）
 * }
 * </pre>
 *
 * <p>输出：
 * <pre>
 * {
 *   "parentSampleId": "S001",
 *   "childSamples": [
 *     {"label": "A", "sampleId": "S001-A", "volumeUl": 500},
 *     {"label": "B", "sampleId": "S001-B", "volumeUl": 300},
 *     {"label": "C", "sampleId": "S001-C", "volumeUl": 200}
 *   ],
 *   "totalSplitVolumeUl": 1000,
 *   "splitCount": 3
 * }
 * </pre>
 *
 * <p>后续节点可通过 ${s_split.childSamples} 引用子样本列表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleSplitStepExecutor implements StepExecutor {

    private final SampleLifecycleService sampleLifecycleService;

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.SAMPLE_SPLIT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult execute(StepDispatchContext ctx) {
        Map<String, Object> inputParams = ctx.getInputParams();

        // 1. 取参数
        String sampleId = (String) inputParams.get("sampleId");
        if (sampleId == null || sampleId.isBlank()) {
            return StepResult.fail("PARAM_MISSING", "样本拆分缺少 sampleId 参数");
        }

        Object splitsRaw = inputParams.get("splits");
        if (!(splitsRaw instanceof List)) {
            return StepResult.fail("PARAM_MISSING", "样本拆分缺少 splits 配置");
        }
        List<Map<String, Object>> splits = (List<Map<String, Object>>) splitsRaw;
        if (splits.isEmpty()) {
            return StepResult.fail("PARAM_MISSING", "splits 配置不能为空");
        }

        String containerType = (String) inputParams.get("containerType");

        log.info("[SampleSplitExecutor] 开始拆分 executionId={} nodeId={} " +
                        "sampleId={} splitCount={}",
                ctx.getExecutionId(), ctx.getNodeId(), sampleId, splits.size());

        try {
            // 2. 构建拆分请求
            List<SampleSplitReqVO.SplitItem> splitItems = splits.stream()
                    .map(s -> {
                        SampleSplitReqVO.SplitItem item = new SampleSplitReqVO.SplitItem();
                        item.setLabel((String) s.get("label"));
                        item.setVolumeUl(toBigDecimal(s.get("volumeUl")));
                        return item;
                    })
                    .collect(Collectors.toList());

            SampleSplitReqVO req = new SampleSplitReqVO();
            req.setParentSampleId(sampleId);
            req.setSplits(splitItems);
            req.setContainerType(containerType);
            req.setExecutionId(ctx.getExecutionId());
            req.setNodeId(ctx.getNodeId());

            // 3. 执行拆分
            SampleSplitResultVO result = sampleLifecycleService.splitSample(req);

            // 4. 构建输出
            Map<String, Object> outputs = new HashMap<>();
            outputs.put("parentSampleId", sampleId);
            outputs.put("childSamples", result.getChildSamples().stream()
                    .map(child -> Map.of(
                            "label",    child.getLabel(),
                            "sampleId", child.getSampleId(),
                            "volumeUl", child.getVolumeUl()))
                    .collect(Collectors.toList()));
            outputs.put("totalSplitVolumeUl", result.getTotalSplitVolumeUl());
            outputs.put("splitCount", result.getChildSamples().size());

            log.info("[SampleSplitExecutor] 拆分完成 executionId={} nodeId={} " +
                            "parent={} children={}",
                    ctx.getExecutionId(), ctx.getNodeId(), sampleId,
                    result.getChildSamples().stream()
                            .map(SampleSplitResultVO.ChildSample::getSampleId)
                            .collect(Collectors.toList()));

            return StepResult.ok(outputs);

        } catch (Exception e) {
            log.error("[SampleSplitExecutor] 拆分失败 executionId={} nodeId={} " +
                            "sampleId={} error={}",
                    ctx.getExecutionId(), ctx.getNodeId(), sampleId, e.getMessage(), e);
            return StepResult.fail("SAMPLE_SPLIT_ERROR", "样本拆分失败: " + e.getMessage());
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal b) return b;
        if (value instanceof Number) return new BigDecimal(value.toString());
        return new BigDecimal(value.toString());
    }
}