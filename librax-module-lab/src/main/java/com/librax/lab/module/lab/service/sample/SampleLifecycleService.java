package com.librax.lab.module.lab.service.sample;

import com.librax.lab.module.lab.dal.vo.SampleSplitReqVO;
import com.librax.lab.module.lab.dal.vo.SampleSplitResultVO;

import java.util.List;
import java.util.Map;

public interface SampleLifecycleService {

    /** 流程启动：样本进入流程 */
    void onSampleLoaded(String sampleId, String executionId);

    /** 预绑定：一次性把样本绑到所有需要样本的步骤上 */
    void preBindSteps(List<String> sampleIds, String executionId,
                      String pipelineKey, int pipelineVersion);

    /** 步骤开始：更新样本处理状态 */
    void onStepStarted(String sampleId, String executionId,
                       String nodeId, int attempt);

    /** 步骤成功：记录结果 */
    void onStepCompleted(String sampleId, String executionId,
                         String nodeId, int attempt,
                         Map<String, Object> outputs);

    /** 步骤失败 */
    void onStepFailed(String sampleId, String executionId,
                      String nodeId, int attempt);

    /** 流程结束：样本结算 */
    void onExecutionCompleted(String sampleId, String executionId,
                              boolean success);

    /** 登记样本 */
    String registerSample(SampleRegisterReqVO req);

    /** 转移位置 */
    void transferSample(String sampleId, String toLocation,
                        String toLocationDetail, String operator);

    /** 样本拆分 */
    SampleSplitResultVO splitSample(SampleSplitReqVO req);
}
