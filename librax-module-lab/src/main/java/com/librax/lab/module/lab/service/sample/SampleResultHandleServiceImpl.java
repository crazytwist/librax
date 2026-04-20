
package com.librax.lab.module.lab.service.sample;

import com.alibaba.fastjson.JSON;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleInfoDO;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleResultDO;
import com.librax.lab.module.lab.dal.mysql.sample.SampleInfoMapper;
import com.librax.lab.module.lab.dal.mysql.sample.SampleResultMapper;
import com.librax.lab.module.lab.enums.AbnormalFlagEnum;
import com.librax.lab.module.lab.enums.ReviewStatusEnum;
import com.librax.lab.module.infra.framework.util.LabIdGenerator;
import com.librax.lab.module.lab.service.sample.TestItemReferenceRegistry.TestItemReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SampleResultHandleServiceImpl implements SampleResultHandleService {

    private final SampleResultMapper resultMapper;
    private final SampleInfoMapper sampleInfoMapper;
    private final LabIdGenerator idGenerator;

    // 框架级 key 前缀，这些不是检测结果
    private static final Set<String> IGNORED_KEYS = Set.of(
            "_callbackToken", "_waitingFor", "_waitingSince",
            "deviceTaskId", "deviceType", "command",
            "conditionResult", "branchName", "matchedTarget", "expr"
    );

    // ================================================================
    // 从步骤输出解析检测结果
    // ================================================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveResultsFromOutput(String sampleId, String executionId,
                                      String nodeId, Map<String, Object> outputs) {
        if (outputs == null || outputs.isEmpty()) return;

        // 获取样本信息（用于冗余字段）
        SampleInfoDO sample = sampleInfoMapper.selectBySampleId(sampleId);

        // 检查是否有显式声明的 _results 数组
        Object explicitResults = outputs.get("_results");
        if (explicitResults instanceof List) {
            saveExplicitResults(sampleId, sample, executionId, nodeId,
                    (List<Map<String, Object>>) explicitResults);
        } else {
            saveImplicitResults(sampleId, sample, executionId, nodeId, outputs);
        }
    }

    /**
     * 显式结果：从 _results 数组解析
     * 格式：[{"testItem":"PH","value":7.2,"unit":"pH","text":null}, ...]
     */
    private void saveExplicitResults(String sampleId, SampleInfoDO sample,
                                     String executionId, String nodeId,
                                     List<Map<String, Object>> resultList) {
        for (Map<String, Object> item : resultList) {
            String testItem = (String) item.get("testItem");
            if (testItem == null) continue;

            Object value = item.get("value");
            String text = (String) item.get("text");
            String unit = (String) item.get("unit");
            String deviceId = (String) item.get("deviceId");

            BigDecimal resultValue = toBigDecimal(value);
            saveOneResult(sampleId, sample, executionId, nodeId,
                    testItem, resultValue, text, unit, deviceId, item);
        }
    }

    /**
     * 隐式结果：从 outputs 的 key-value 推断
     * 跳过框架级 key（_ 开头的和已知的非检测项 key）
     * 只处理数值类型的 value
     */
    private void saveImplicitResults(String sampleId, SampleInfoDO sample,
                                     String executionId, String nodeId,
                                     Map<String, Object> outputs) {
        for (Map.Entry<String, Object> entry : outputs.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // 跳过框架级 key
            if (key.startsWith("_") || IGNORED_KEYS.contains(key)) continue;

            // 只处理数值类型
            BigDecimal resultValue = toBigDecimal(value);
            if (resultValue == null) continue;

            // key 转大写作为 testItem
            String testItem = key.toUpperCase();

            saveOneResult(sampleId, sample, executionId, nodeId,
                    testItem, resultValue, null, null, null, null);
        }
    }

    /**
     * 保存单个检测结果
     */
    private void saveOneResult(String sampleId, SampleInfoDO sample,
                               String executionId, String nodeId,
                               String testItem, BigDecimal resultValue,
                               String resultText, String unit,
                               String deviceId, Map<String, Object> rawData) {
        // 查参考范围
        TestItemReference ref = TestItemReferenceRegistry.get(testItem);

        // 自动判定异常
        boolean isAbnormal = false;
        String abnormalFlag = null;
        String reviewStatus;

        if (resultValue != null && ref != null) {
            if (ref.getReferenceHigh() != null
                    && resultValue.compareTo(ref.getReferenceHigh()) > 0) {
                isAbnormal = true;
                abnormalFlag = AbnormalFlagEnum.HIGH.name();
            } else if (ref.getReferenceLow() != null
                    && resultValue.compareTo(ref.getReferenceLow()) < 0) {
                isAbnormal = true;
                abnormalFlag = AbnormalFlagEnum.LOW.name();
            }
            // 异常的需要人工审核，正常的自动通过
            reviewStatus = isAbnormal
                    ? ReviewStatusEnum.PENDING.name()
                    : ReviewStatusEnum.AUTO_APPROVED.name();
        } else {
            // 没有参考范围，待人工审核
            reviewStatus = ReviewStatusEnum.PENDING.name();
        }

        // 构建结果记录
        SampleResultDO result = new SampleResultDO();
        result.setResultId(idGenerator.nextResultId());
        result.setSampleId(sampleId);
        result.setRootSampleId(sample != null ? sample.getRootSampleId() : null);
        result.setBatchNo(sample != null ? sample.getBatchNo() : null);
        result.setExecutionId(executionId);
        result.setNodeId(nodeId);
        result.setTestItem(testItem);
        result.setTestItemName(ref != null ? ref.getTestItemName() : null);
        result.setResultValue(resultValue);
        result.setResultText(resultText);
        result.setUnit(unit != null ? unit : (ref != null ? ref.getUnit() : null));
        result.setReferenceLow(ref != null ? ref.getReferenceLow() : null);
        result.setReferenceHigh(ref != null ? ref.getReferenceHigh() : null);
        result.setReferenceText(ref != null ? ref.getReferenceText() : null);
        result.setIsAbnormal(isAbnormal);
        result.setAbnormalFlag(abnormalFlag);
        result.setDeviceId(deviceId);
        result.setMeasuredAt(LocalDateTime.now());
        result.setRawData(rawData != null ? JSON.toJSONString(rawData) : null);
        result.setReviewStatus(reviewStatus);
        result.setIsFinal(true);

        resultMapper.insert(result);

        log.info("[SampleResult] 保存检测结果 sampleId={} testItem={} value={} " +
                        "abnormal={} review={}",
                sampleId, testItem, resultValue, isAbnormal, reviewStatus);
    }

    // ================================================================
    // 查询
    // ================================================================

    @Override
    public List<SampleResultDO> getSampleResults(String sampleId) {
        return resultMapper.selectBySampleId(sampleId);
    }

    @Override
    public List<SampleResultDO> getResultHistory(String sampleId, String testItem) {
        return resultMapper.selectBySampleAndItem(sampleId, testItem);
    }

    @Override
    public List<SampleResultDO> getPendingReviewList() {
        return resultMapper.selectPendingReview();
    }

    @Override
    public List<SampleResultDO> getBatchAbnormalResults(String batchNo) {
        return resultMapper.selectAbnormalByBatch(batchNo);
    }

    // ================================================================
    // 审核
    // ================================================================

    @Override
    public void reviewResult(String resultId, String reviewStatus,
                             String reviewedBy, String comment) {
        SampleResultDO result = resultMapper.selectByResultId(resultId);
        if (result == null) {
            throw new RuntimeException("检测结果不存在: " + resultId);
        }

        // 只有 PENDING 状态的才能审核
        if (!ReviewStatusEnum.PENDING.name().equals(result.getReviewStatus())) {
            throw new RuntimeException("结果状态不是待审核，当前: " + result.getReviewStatus());
        }

        resultMapper.updateReviewStatus(resultId, reviewStatus, reviewedBy, comment);

        log.info("[SampleResult] 审核完成 resultId={} status={} by={}",
                resultId, reviewStatus, reviewedBy);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchReview(List<String> resultIds, String reviewStatus,
                            String reviewedBy, String comment) {
        for (String resultId : resultIds) {
            try {
                reviewResult(resultId, reviewStatus, reviewedBy, comment);
            } catch (Exception e) {
                log.warn("[SampleResult] 批量审核单条失败 resultId={} error={}",
                        resultId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void triggerRetest(String resultId, String reviewedBy, String comment) {
        SampleResultDO result = resultMapper.selectByResultId(resultId);
        if (result == null) {
            throw new RuntimeException("检测结果不存在: " + resultId);
        }

        // 1. 标记当前结果为 RETEST，is_final=false
        resultMapper.updateReviewStatus(resultId, ReviewStatusEnum.RETEST.name(),
                reviewedBy, comment);

        // 标记为非最终结果
        SampleResultDO update = new SampleResultDO();
        update.setId(result.getId());
        update.setIsFinal(false);
        resultMapper.updateById(update);

        log.info("[SampleResult] 触发复测 resultId={} sampleId={} testItem={} by={}",
                resultId, result.getSampleId(), result.getTestItem(), reviewedBy);

        // 2. TODO: 触发复测流程
        //    这里需要调用 PipelineExecutionService.start() 启动一个新的流程执行
        //    传入 sampleId 和需要复测的 testItem
        //    复测流程执行完成后，新的结果会通过正常的事件链路写入 lab_sample_result
        //    新结果的 is_final=true，老结果的 is_final=false
        //
        //    暂时只做标记，不自动触发，由人工在前端启动复测流程
    }

    // ================================================================
    // 工具方法
    // ================================================================

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof BigDecimal) return (BigDecimal) value;
            if (value instanceof Number) return new BigDecimal(value.toString());
            if (value instanceof String) {
                String s = ((String) value).trim();
                if (s.isEmpty()) return null;
                return new BigDecimal(s);
            }
        } catch (NumberFormatException e) {
            // 不是数值，返回 null
        }
        return null;
    }

    private String generateResultId() {
        return "R" + System.currentTimeMillis()
                + String.format("%04d", new Random().nextInt(10000));
    }
}