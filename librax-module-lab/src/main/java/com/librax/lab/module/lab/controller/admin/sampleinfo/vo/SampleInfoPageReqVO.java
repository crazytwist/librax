package com.librax.lab.module.lab.controller.admin.sampleinfo.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import com.librax.lab.framework.common.pojo.PageParam;
import java.math.BigDecimal;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static com.librax.lab.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联分页 Request VO")
@Data
public class SampleInfoPageReqVO extends PageParam {

    @Schema(description = "样本唯一业务ID（条码/二维码）", example = "22328")
    private String sampleId;

    @Schema(description = "衍生类型：ORIGINAL原始 / SPLIT拆分 / MERGE合并 / ALIQUOT分装 / ADD_REAGENT加试剂", example = "2")
    private String deriveType;

    @Schema(description = "样本类型：BLOOD/URINE/TISSUE/WATER/SOIL/AIR/MIXTURE", example = "2")
    private String sampleType;

    @Schema(description = "样本名称/描述", example = "芋艿")
    private String sampleName;

    @Schema(description = "容器条码")
    private String containerCode;

    @Schema(description = "REGISTERED/LOADED/IN_PROCESS/SPLIT/COMPLETED/ARCHIVED/REJECTED/LOST", example = "1")
    private String status;

    @Schema(description = "当前物理位置（架位编号/工位编号/冷库编号）")
    private String locationCode;

    @Schema(description = "样本批次号，同一批送检的样本共享")
    private String batchNo;

    @Schema(description = "样本有效期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] expireTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}