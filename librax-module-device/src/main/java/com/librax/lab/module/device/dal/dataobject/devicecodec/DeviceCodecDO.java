package com.librax.lab.module.device.dal.dataobject.devicecodec;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;

/**
 * 设备响应解析规则表，把原始报文转为统一的 Map 输出 [lab_device_] DO
 *
 * @author 一南
 */
@TableName("lab_device_codec")
@KeySequence("lab_device_codec_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceCodecDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * 规则名称，便于识别，如 雷磁PH计-测量结果解析
     */
    private String codecName;
    /**
     * 解析类型：JSON / HEX / REGEX / SCRIPT
     */
    private String parseType;
    /**
     * JSONPath 字段映射，key=输出字段名 value=JSONPath表达式
     */
    private String fieldMapping;
    /**
     * 十六进制按位解析规则
     */
    private String hexRules;
    /**
     * 正则提取规则
     */
    private String regexRules;
    /**
     * 脚本引擎：groovy / js
     */
    private String scriptEngine;
    /**
     * 解析脚本，入参为原始响应字符串，返回 Map
     */
    private String scriptContent;
    /**
     * 单位换算规则，解析后应用
     */
    private String unitConversions;
    /**
     * 有效值范围校验，如 {"ph":{"min":0,"max":14}}，超出则丢弃
     */
    private String validRange;
    /**
     * 备注
     */
    private String remark;


}