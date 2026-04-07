
package com.librax.lab.module.lab.service.sample;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SampleRegisterReqVO {

    private String sampleId;

    @NotBlank(message = "样本类型不能为空")
    private String sampleType;

    private String sampleName;
    private String containerType;
    private String containerCode;
    private BigDecimal volumeUl;
    private BigDecimal concentration;
    private String batchNo;
    private String orderNo;
    private Integer priority;
    private String source;
    private String externalId;
    private String locationCode;
    private String locationDetail;
    private LocalDateTime collectedAt;
    private LocalDateTime expireTime;
    private String remark;
}