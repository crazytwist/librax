package com.librax.lab.module.device.dal.dataobject.deviceinfo;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 设备基本信息表，一行一台物理设备 [lab_device_] DO
 *
 * @author 一南
 */
@TableName("lab_device_info")
@KeySequence("lab_device_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfoDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 设备唯一业务ID，如 PH-METER-01
     */
    private String deviceId;
    /**
     * 设备名称，如 雷磁PH计1号
     */
    private String deviceName;
    /**
     * 设备类型，对应 pd_step_definition.device_type，如 PH_METER
     */
    private String deviceType;
    /**
     * 通信协议：TCP / HTTP / SERIAL / SDK / MOCK
     */
    private String protocol;
    /**
     * 所属区域，关联 lab_zone_slot
     */
    private String zoneCode;
    /**
     * IP地址或域名，TCP/HTTP时填写
     */
    private String host;
    /**
     * 端口号，TCP/HTTP时填写
     */
    private Integer port;
    /**
     * HTTP设备基础URL，如 http://192.168.1.10:8080
     */
    private String baseUrl;
    /**
     * HTTP回调模式：WEBHOOK(设备主动推) / POLL(主动轮询)
     */
    private String callbackMode;
    /**
     * 轮询间隔(ms)，callback_mode=POLL时有效
     */
    private Long pollIntervalMs;
    /**
     * 认证方式：NONE / BASIC / TOKEN / HMAC
     */
    private String authType;
    /**
     * 认证配置，如 {"token":"xxx"} / {"username":"","password":""}
     */
    private String authConfig;
    /**
     * 串口号，如 COM3 / /dev/ttyUSB0
     */
    private String serialPort;
    /**
     * 波特率，如 9600 / 115200
     */
    private Integer baudRate;
    /**
     * 数据位
     */
    private Integer dataBits;
    /**
     * 停止位
     */
    private Integer stopBits;
    /**
     * 校验位：NONE/ODD/EVEN
     */
    private String parity;
    /**
     * SDK驱动实现类全限定名，executor=SDK时使用
     */
    private String sdkClass;
    /**
     * SDK初始化参数，透传给驱动实现类
     */
    private String sdkConfig;
    /**
     * 连接超时(ms)
     */
    private Long connectTimeoutMs;
    /**
     * 读取超时(ms)
     */
    private Long readTimeoutMs;
    /**
     * 心跳间隔(ms)，0表示不发心跳
     */
    private Long heartbeatIntervalMs;
    /**
     * 心跳指令code，引用 lab_device_command.command_code
     */
    private String heartbeatCommand;
    /**
     * 最大并发指令数，通常为1
     */
    private Integer maxConcurrent;
    /**
     * 当前状态：ONLINE/OFFLINE/FAULT，运行时由心跳维护，此处为初始值
     */
    private String status;
    /**
     * 是否启用
     */
    private Boolean enabled;
    /**
     * 备注
     */
    private String remark;


}