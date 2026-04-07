package com.librax.lab.module.lab.service.sample;

import com.librax.lab.module.lab.dal.dataobject.sample.SampleResultDO;

import java.util.List;
import java.util.Map;

public interface SampleResultHandleService {

    /**
     * 从步骤输出中解析并保存检测结果
     *
     * 步骤输出格式约定（执行器返回的 outputs）：
     *   单检测项：{"ph": 7.2, "temperature": 25.0}
     *     → key 作为 testItem，value 作为 resultValue
     *     → 自动排除非检测项的 key（如 _callbackToken, deviceTaskId 等 _ 开头的）
     *
     *   多检测项（显式声明）：
     *   {"_results": [
     *       {"testItem":"PH", "value":7.2, "unit":"pH"},
     *       {"testItem":"TURBIDITY", "value":3.5, "unit":"NTU"}
     *   ]}
     *     → 用 _results 数组明确声明每个检测项
     */
    void saveResultsFromOutput(String sampleId, String executionId,
                               String nodeId, Map<String, Object> outputs);

    /** 查询样本的所有最终结果 */
    List<SampleResultDO> getSampleResults(String sampleId);

    /** 查询样本某个检测项的所有结果（含历史复测） */
    List<SampleResultDO> getResultHistory(String sampleId, String testItem);

    /** 人工审核 */
    void reviewResult(String resultId, String reviewStatus,
                      String reviewedBy, String comment);

    /** 批量审核 */
    void batchReview(List<String> resultIds, String reviewStatus,
                     String reviewedBy, String comment);

    /** 触发复测 */
    void triggerRetest(String resultId, String reviewedBy, String comment);

    /** 查询待审核结果列表 */
    List<SampleResultDO> getPendingReviewList();

    /** 查询批次内的异常结果 */
    List<SampleResultDO> getBatchAbnormalResults(String batchNo);
}
