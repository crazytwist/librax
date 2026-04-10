package com.librax.lab.module.device.controller.vo;


import lombok.Data;
import java.util.Map;

@Data
public class DeviceCallbackReqVO {

    /** 回调令牌，创建等待时生成，必须携带 */
    private String callbackToken;

    /** 是否成功 */
    private boolean success;

    /** 成功时的输出数据，如 {"ph": 7.2, "temperature": 25.1} */
    private Map<String, Object> data;

    /** 失败时的错误码 */
    private String errorCode;

    /** 失败时的错误信息 */
    private String errorMsg;
}