package com.librax.lab.module.device.dal.dataobject.devicecommand;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 设备指令配置表，定义每种设备支持的指令及报文模板 [lab_device_] DO
 *
 * @author 一南
 */
@TableName("lab_device_command")
@KeySequence("lab_device_command_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCommandDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 设备类型，与 lab_device_info.device_type 对应
     */
    private String deviceType;
    /**
     * 指令代码，如 MEASURE / COLLECT / CALIBRATE
     */
    private String commandCode;
    /**
     * 请求报文模板，支持 ${param} 占位符替换，如 {"action":"measure","count":${sampleCount}}
     */
    private String requestTemplate;
    /**
     * HTTP Content-Type，TCP/SERIAL时为报文编码格式：HEX/ASCII/BINARY
     */
    private String contentType;
    /**
     * HTTP方法：GET/POST/PUT，仅HTTP协议有效
     */
    private String httpMethod;
    /**
     * HTTP请求路径，如 /api/measure，拼接到 base_url 后
     */
    private String httpPath;
    /**
     * 指令执行超时(ms)，覆盖设备默认值
     */
    private Long timeoutMs;
    /**
     * 是否可重试
     */
    private Boolean retryable;
    /**
     * 关联 lab_device_codec.id，NULL时原样返回响应体
     */
    private Long codecId;
    /**
     * 轮询结果的HTTP路径，如 /api/result/${taskId}
     */
    private String pollPath;
    /**
     * 判断完成的表达式，如 $.status == "DONE"
     */
    private String pollDoneExpr;
    /**
     * 最大轮询次数
     */
    private Integer pollMaxTimes;
    /**
     * Mock协议返回数据，MockDeviceDriver 用此字段生成回调
     */
    private String mockOutput;
    /**
     * 备注
     */
    private String remark;


}