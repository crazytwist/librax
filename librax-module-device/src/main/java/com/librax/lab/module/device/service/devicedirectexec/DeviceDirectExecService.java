package com.librax.lab.module.device.service.devicedirectexec;

import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandExecuteReqVO;
import com.librax.lab.module.device.controller.admin.devicecommand.vo.DeviceCommandExecuteRespVO;
import jakarta.validation.Valid;

import java.util.Map;

/**
 * 设备指令直接执行 Service 接口
 *
 * <p>不依赖流水线，直接触发设备执行指定指令。执行记录存储于 Redis（TTL 2小时）。
 * execId 以 {@code de-} 为前缀，与流水线 executionId 区分，
 * {@link com.librax.lab.module.device.callback.DeviceCallbackHandler} 据此路由回调。
 */
public interface DeviceDirectExecService {

    /**
     * 触发设备指令直接执行（非阻塞，立即返回 execId）
     *
     * @param reqVO 执行请求
     * @return 执行应答（status=EXECUTING 时可通过 execId 轮询 getResult）
     */
    DeviceCommandExecuteRespVO execute(@Valid DeviceCommandExecuteReqVO reqVO);

    /**
     * 查询执行结果（支持轮询）
     *
     * @param execId execute 接口返回的执行ID
     * @return 当前执行状态与结果
     */
    DeviceCommandExecuteRespVO getResult(String execId);

    /**
     * 设备回调处理（内部方法，由 DeviceCallbackHandler 在识别到 de- 前缀时调用）
     *
     * @param execId      执行ID
     * @param success     是否成功
     * @param outputs     解析后的输出数据
     * @param errorCode   错误码（失败时）
     * @param errorMsg    错误信息（失败时）
     * @param rawResponse 设备原始响应报文
     */
    void onCallback(String execId,
                    boolean success,
                    Map<String, Object> outputs,
                    String errorCode,
                    String errorMsg,
                    String rawResponse);

}
