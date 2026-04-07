package com.librax.lab.module.lab.controller.app;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.flow.engine.execution.callback.CallbackResult;
import com.librax.lab.module.flow.engine.execution.callback.StepCallbackService;
import com.librax.lab.module.flow.service.pipelineexecution.PipelineExecutionService;
import com.librax.lab.module.lab.service.sample.*;
import com.librax.lab.module.lab.service.sample.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 水质检测集成测试 Controller
 * <p>
 * 按顺序调用每个接口，模拟完整的业务流程。
 * 生产环境删除此 Controller。
 */
@Slf4j
@Tag(name = "集成测试 - 水质检测全链路")
@RestController
@RequestMapping("/test/water-quality")
@RequiredArgsConstructor
public class WaterQualityIntegrationTestController {

    private final SampleLifecycleService sampleService;
    private final SampleResultHandleService resultHandleService;
    private final SampleTraceService traceService;
    private final PipelineExecutionService executionService;
    private final StepCallbackService callbackService;

    // ================================================================
    // Step 1: 登记样本
    // ================================================================

    /**
     * POST /app-api/test/water-quality/step1-register
     * <p>
     * 模拟：实验室收到一份水样，扫码登记
     */
    @PostMapping("/step1-register")
    @Operation(summary = "Step1: 登记水质样本")
    public CommonResult<Map<String, Object>> step1Register() {
        SampleRegisterReqVO req = new SampleRegisterReqVO();
        req.setSampleId("WQ-20250405-001");
        req.setSampleType("WATER");
        req.setSampleName("东湖水质检测样本-取样点A");
        req.setContainerType("VIAL");
        req.setContainerCode("VIAL-20250405-001");
        req.setVolumeUl(new BigDecimal("50000"));  // 50ml
        req.setBatchNo("BATCH-20250405");
        req.setOrderNo("ORDER-WQ-20250405-001");
        req.setPriority(0);  // 普通优先级
        req.setSource("MANUAL");
        req.setLocationCode("RECV-STATION-01");  // 收样工位
        req.setLocationDetail("收样台-3号位");
        req.setCollectedAt(LocalDateTime.of(2025, 4, 5, 8, 30, 0));
        req.setExpireTime(LocalDateTime.of(2025, 4, 6, 8, 30, 0));  // 24小时有效
        req.setRemark("东湖A取样点，天气晴，水温18.5℃");

        String sampleId = sampleService.registerSample(req);

        log.info("========== Step1 完成：样本登记 sampleId={} ==========", sampleId);

        return CommonResult.success(Map.of(
                "sampleId", sampleId,
                "status", "REGISTERED",
                "message", "样本登记成功，请继续 Step2 启动流程"
        ));
    }

    // ================================================================
    // Step 2: 启动流程
    // ================================================================

    /**
     * POST /app-api/test/water-quality/step2-start-pipeline
     * <p>
     * 模拟：选择"水质检测"流程，传入样本ID启动
     * <p>
     * 这一步会触发：
     * - ExecutionStartedEvent → SampleFlowIntegrationListener
     * - 样本状态 REGISTERED → LOADED
     * - 预绑定所有 INSTRUMENT 步骤
     * - 自动调度第一个就绪节点（s_sample）
     */
    @PostMapping("/step2-start-pipeline")
    @Operation(summary = "Step2: 启动水质检测流程")
    public CommonResult<Map<String, Object>> step2StartPipeline(
            @RequestParam(defaultValue = "WQ-20250405-001") String sampleId) {

        Map<String, Object> inputParams = new LinkedHashMap<>();
        inputParams.put("sampleId", sampleId);
        inputParams.put("batchNo", "BATCH-20250405");
        inputParams.put("testStandard", "GB3838-2002");   // 地表水环境质量标准
        inputParams.put("testItems", List.of("PH", "TURBIDITY"));
        inputParams.put("temperature", 18.5);
        inputParams.put("collector", "张三");
        inputParams.put("collectLocation", "东湖-A取样点");

        String executionId = executionService.start(
                "water_quality_test",    // pipelineKey
                1,                        // version
                inputParams,
                "MANUAL",                 // triggerType
                "operator-001"            // triggeredBy
        );

        log.info("========== Step2 完成：流程启动 executionId={} ==========", executionId);

        // 等待异步事件处理完成（测试用，生产不需要）
        sleep(2000);

        return CommonResult.success(Map.of(
                "executionId", executionId,
                "sampleId", sampleId,
                "message", "流程已启动，s_sample 节点正在自动执行（Mock），" +
                        "s_ph 和 s_turbidity 如果是 INSTRUMENT 类型会进入 WAITING，" +
                        "如果是 Mock 会自动完成。请检查数据库状态后继续 Step3。"
        ));
    }

    // ================================================================
    // Step 3: 检查中间状态
    // ================================================================

    /**
     * GET /app-api/test/water-quality/step3-check-status
     * <p>
     * 查看当前样本状态、步骤绑定情况、流程执行进度
     */
    @GetMapping("/step3-check-status")
    @Operation(summary = "Step3: 检查样本和流程状态")
    public CommonResult<Map<String, Object>> step3CheckStatus(
            @RequestParam(defaultValue = "WQ-20250405-001") String sampleId) {

        // 样本溯源报告（包含所有信息）
        SampleTraceReportVO report = traceService.getTraceReport(sampleId);

        Map<String, Object> result = new LinkedHashMap<>();
        if (report != null) {
            result.put("sampleInfo", report.getSampleInfo());
            result.put("journey", report.getJourney());
            result.put("results", report.getResults());
            result.put("timeline", report.getTimeline());
            result.put("familyTree", report.getFamilyTree());
        } else {
            result.put("error", "样本不存在: " + sampleId);
        }

        return CommonResult.success(result);
    }

    // ================================================================
    // Step 4: 模拟设备回调（PH检测完成）
    // ================================================================

    /**
     * POST /app-api/test/water-quality/step4-ph-callback
     * <p>
     * 模拟：PH仪器检测完成，通过回调接口推送结果
     * <p>
     * 注意：只有当 s_ph 步骤是 WAITING 状态时才需要调用此接口。
     * 如果 s_ph 用的是 MockStepExecutor（同步返回），它会自动完成，
     * 不需要回调。
     * <p>
     * 真实场景下，这个接口由 PH仪器的控制软件调用。
     */
    @PostMapping("/step4-ph-callback")
    @Operation(summary = "Step4: 模拟PH仪器回调")
    public CommonResult<Map<String, Object>> step4PhCallback(
            @RequestParam String executionId,
            @RequestParam(required = false) String callbackToken) {

        Map<String, Object> phOutputs = new LinkedHashMap<>();
        phOutputs.put("ph", new BigDecimal("7.35"));
        phOutputs.put("temperature", new BigDecimal("18.5"));
        phOutputs.put("deviceId", "PH_METER_01");
        phOutputs.put("measuredAt", LocalDateTime.now().toString());
        phOutputs.put("calibrationDate", "2025-04-01");
        phOutputs.put("rawData", Map.of(
                "voltage_mv", 28.5,
                "electrode_status", "OK",
                "buffer_ph4", 4.01,
                "buffer_ph7", 7.02
        ));

        CallbackResult result = callbackService.callback(
                executionId,
                "s_ph",
                callbackToken,
                true,
                phOutputs,
                null,
                null
        );

        log.info("========== Step4 完成：PH回调 executionId={} success={} ==========",
                executionId, result.isSuccess());

        sleep(1000);

        return CommonResult.success(Map.of(
                "callbackSuccess", result.isSuccess(),
                "errorMsg", result.getErrorMsg() != null ? result.getErrorMsg() : "",
                "phValue", "7.35",
                "message", result.isSuccess()
                        ? "PH检测结果已提交，流程将自动调度后续节点"
                        : "回调失败: " + result.getErrorMsg()
        ));
    }

    // ================================================================
    // Step 5: 模拟设备回调（浊度检测完成）
    // ================================================================

    /**
     * POST /app-api/test/water-quality/step5-turbidity-callback
     * <p>
     * 模拟：浊度仪检测完成
     */
    @PostMapping("/step5-turbidity-callback")
    @Operation(summary = "Step5: 模拟浊度仪回调")
    public CommonResult<Map<String, Object>> step5TurbidityCallback(
            @RequestParam String executionId,
            @RequestParam(required = false) String callbackToken) {

        Map<String, Object> turbOutputs = new LinkedHashMap<>();
        turbOutputs.put("ntu", new BigDecimal("3.2"));
        turbOutputs.put("deviceId", "TURB_METER_01");
        turbOutputs.put("measuredAt", LocalDateTime.now().toString());
        turbOutputs.put("rawData", Map.of(
                "scattered_light", 45.2,
                "transmitted_light", 98.1,
                "lamp_status", "OK"
        ));

        CallbackResult result = callbackService.callback(
                executionId,
                "s_turbidity",
                callbackToken,
                true,
                turbOutputs,
                null,
                null
        );

        log.info("========== Step5 完成：浊度回调 executionId={} success={} ==========",
                executionId, result.isSuccess());

        sleep(1000);

        return CommonResult.success(Map.of(
                "callbackSuccess", result.isSuccess(),
                "errorMsg", result.getErrorMsg() != null ? result.getErrorMsg() : "",
                "ntuValue", "3.2",
                "message", result.isSuccess()
                        ? "浊度检测结果已提交，s_calc 节点将自动触发"
                        : "回调失败: " + result.getErrorMsg()
        ));
    }

    // ================================================================
    // Step 6: 查看最终结果
    // ================================================================

    /**
     * GET /app-api/test/water-quality/step6-final-results
     * <p>
     * 流程应该已经全部完成，查看：
     * - 样本最终状态（COMPLETED）
     * - 所有检测结果
     * - 完整溯源报告
     */
    @GetMapping("/step6-final-results")
    @Operation(summary = "Step6: 查看最终检测结果")
    public CommonResult<Map<String, Object>> step6FinalResults(
            @RequestParam(defaultValue = "WQ-20250405-001") String sampleId) {

        Map<String, Object> result = new LinkedHashMap<>();

        // 完整溯源报告
        SampleTraceReportVO report = traceService.getTraceReport(sampleId);
        result.put("traceReport", report);

        // 检测结果列表
        List<?> results = resultHandleService.getSampleResults(sampleId);
        result.put("testResults", results);

        // 待审核结果
        List<?> pendingReview = resultHandleService.getPendingReviewList();
        result.put("pendingReview", pendingReview);

        return CommonResult.success(result);
    }

    // ================================================================
    // Step 7: 结果审核
    // ================================================================

    /**
     * POST /app-api/test/water-quality/step7-review
     * <p>
     * 模拟：检测人员审核检测结果
     */
    @PostMapping("/step7-review")
    @Operation(summary = "Step7: 审核检测结果")
    public CommonResult<Map<String, Object>> step7Review(
            @RequestParam String resultId,
            @RequestParam(defaultValue = "APPROVED") String reviewStatus,
            @RequestParam(defaultValue = "结果正常，数据可靠") String comment) {

        resultHandleService.reviewResult(
                resultId,
                reviewStatus,
                "reviewer-001",
                comment
        );

        log.info("========== Step7 完成：结果审核 resultId={} status={} ==========",
                resultId, reviewStatus);

        return CommonResult.success(Map.of(
                "resultId", resultId,
                "reviewStatus", reviewStatus,
                "message", "审核完成"
        ));
    }

    // ================================================================
    // Step 8: 模拟异常场景 — PH值异常
    // ================================================================

    /**
     * POST /app-api/test/water-quality/step8-abnormal-ph
     * <p>
     * 模拟：PH值超出参考范围（偏高），验证自动判定和审核流程
     */
    @PostMapping("/step8-abnormal-ph")
    @Operation(summary = "Step8: 模拟PH异常回调")
    public CommonResult<Map<String, Object>> step8AbnormalPh(
            @RequestParam String executionId,
            @RequestParam(required = false) String callbackToken) {

        Map<String, Object> phOutputs = new LinkedHashMap<>();
        phOutputs.put("ph", new BigDecimal("9.8"));   // 超出参考范围 6.5-8.5
        phOutputs.put("temperature", new BigDecimal("19.0"));
        phOutputs.put("deviceId", "PH_METER_01");
        phOutputs.put("measuredAt", LocalDateTime.now().toString());

        CallbackResult result = callbackService.callback(
                executionId,
                "s_ph",
                callbackToken,
                true,
                phOutputs,
                null,
                null
        );

        log.info("========== Step8 完成：异常PH回调 ph=9.8（参考范围6.5-8.5）==========");

        sleep(1000);

        return CommonResult.success(Map.of(
                "callbackSuccess", result.isSuccess(),
                "phValue", "9.8",
                "expectedBehavior", Map.of(
                        "isAbnormal", true,
                        "abnormalFlag", "HIGH",
                        "reviewStatus", "PENDING（需人工审核）"
                ),
                "message", "PH=9.8 超出参考范围，应该自动标记为异常，待人工审核"
        ));
    }

    // ================================================================
    // 全流程一键执行（Mock 模式）
    // ================================================================

    /**
     * POST /app-api/test/water-quality/run-all-mock
     * <p>
     * Mock 模式下一键跑完全流程：
     * 登记样本 → 启动流程 → 等待自动完成 → 查看结果
     * <p>
     * 适用于所有步骤都走 MockStepExecutor 的场景（不需要设备回调）
     */
    @PostMapping("/run-all-mock")
    @Operation(summary = "一键跑完全流程（Mock模式）")
    public CommonResult<Map<String, Object>> runAllMock() {

        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 登记样本
        String sampleId = "WQ-MOCK-" + System.currentTimeMillis();
        SampleRegisterReqVO req = new SampleRegisterReqVO();
        req.setSampleId(sampleId);
        req.setSampleType("WATER");
        req.setSampleName("Mock水质检测样本");
        req.setContainerType("VIAL");
        req.setVolumeUl(new BigDecimal("50000"));
        req.setBatchNo("BATCH-MOCK");
        req.setSource("MANUAL");
        req.setLocationCode("RECV-STATION-01");
        sampleService.registerSample(req);
        result.put("step1_register", Map.of("sampleId", sampleId, "status", "OK"));

        // 2. 启动流程
        String executionId = executionService.start(
                "water_quality_test", 1,
                Map.of("sampleId", sampleId, "batchNo", "BATCH-MOCK"),
                "MANUAL", "test-operator"
        );
        result.put("step2_start", Map.of("executionId", executionId, "status", "OK"));

        // 3. 等待 Mock 自动执行完成
        sleep(5000);

        // 4. 查看结果
        SampleTraceReportVO report = traceService.getTraceReport(sampleId);
        result.put("step3_report", report);

        List<?> testResults = resultHandleService.getSampleResults(sampleId);
        result.put("step4_results", testResults);

        log.info("========== Mock全流程完成 sampleId={} executionId={} ==========",
                sampleId, executionId);

        return CommonResult.success(result);
    }

    // ================================================================
    // 工具
    // ================================================================

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}