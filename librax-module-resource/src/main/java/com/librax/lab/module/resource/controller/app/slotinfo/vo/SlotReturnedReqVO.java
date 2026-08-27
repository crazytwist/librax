package com.librax.lab.module.resource.controller.app.slotinfo.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓储入库完成回调请求体。
 *
 * <p>仓储机械臂将物料从中转位（卸货口）取走并放回货架后，
 * 调用 {@code POST /resource/slot/returned} 通知 librax 系统入库已完成，
 * librax 随即更新下料计划状态，并触发同波次下一件物料的入库请求（如有）。
 *
 * <p><b>字段必填说明：</b>
 * <ul>
 *   <li>{@code requestId}、{@code slotId}、{@code instanceId}、{@code status} — 必填</li>
 *   <li>{@code taskId} — 联动下料流程时必填；仓储独立入库时可不传</li>
 *   <li>{@code code}、{@code message}、{@code eventTime} — 可选，建议失败时填写 code + message 便于追溯</li>
 * </ul>
 */
@Data
@Schema(description = "仓储入库完成回调请求体（仓储 → librax）")
public class SlotReturnedReqVO {

    /**
     * 【必填】仓储系统生成的事件唯一ID，用于幂等去重，防止网络重试导致重复处理。
     * 建议格式：{设备ID}-{时间戳}-{序号}，例如 {@code WH-01-20240716-0002}。
     */
    @Schema(description = "【必填】事件唯一ID（幂等键），仓储侧生成，防止重复通知",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "WH-01-20240716-0002")
    @NotBlank(message = "requestId不能为空")
    private String requestId;

    /**
     * 【下料联动时必填】AGV 下料流程执行ID，即 librax 发送 returnMaterials 时携带的 executionId。
     * librax 依据此 ID 定位对应的下料计划，推进下一步（触发同波次下一件入库请求或进入下一波次）。
     * 若为仓储独立入库场景（不经由 librax 下料流程），可不传或传空。
     * 也可使用别名字段名 {@code transferTaskId}。
     */
    @Schema(description = "【下料联动必填】下料流程执行ID（即 returnMaterials 请求中的 executionId）；独立入库可不传",
            example = "a1b2c3d4e5f6...")
    @JsonAlias("transferTaskId")
    private String taskId;

    /**
     * 【必填】物料已被取走的中转位库位ID（卸货口），即 librax 发送 returnMaterials 时指定的 fromLocation。
     * librax 据此定位下料明细记录，将该库位状态改回空闲。
     * 也可使用别名字段名 {@code fromLocation}。
     */
    @Schema(description = "【必填】中转位库位ID（即 returnMaterials 中的 fromLocation），物料已从此处被取走",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "R-E-1")
    @NotBlank(message = "slotId不能为空")
    @JsonAlias("fromLocation")
    private String slotId;

    /**
     * 【必填】物料实例ID，与 librax 下料请求中的 instanceId 一致，用于校验物料一致性。
     * 也可使用别名字段名 {@code materialId}。
     */
    @Schema(description = "【必填】物料实例ID，与下料请求中的 instanceId 一致",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "MAT-20240716-001")
    @NotBlank(message = "instanceId不能为空")
    @JsonAlias("materialId")
    private String instanceId;

    /**
     * 【必填】操作结果状态。
     * <ul>
     *   <li>{@code RETURNED} — 物料已成功放回货架</li>
     *   <li>{@code FAILED} — 仓储操作失败，librax 将终止对应下料流程</li>
     * </ul>
     */
    @Schema(description = "【必填】操作结果：RETURNED=已入库，FAILED=失败",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "RETURNED",
            allowableValues = {"RETURNED", "FAILED"})
    @NotBlank(message = "status不能为空")
    private String status;

    /**
     * 【可选，失败时建议填写】仓储业务错误码。
     * <ul>
     *   <li>{@code 0} — 成功（status=RETURNED 时默认）</li>
     *   <li>{@code 1001} — 物料实例不存在</li>
     *   <li>{@code 1002} — 中转位无物料（物料未到位）</li>
     *   <li>{@code 1003} — 目标货架位不可用（被占用或损坏）</li>
     *   <li>{@code 1004} — 机械臂忙或执行失败</li>
     *   <li>{@code 1005} — 幂等成功（已处理过相同 requestId）</li>
     *   <li>{@code 1099} — 仓储内部错误</li>
     * </ul>
     */
    @Schema(description = "【可选】错误码（失败时建议填写）：0=成功，1001=物料不存在，1002=中转位无物料，1003=目标位不可用，1004=机械臂失败，1005=幂等成功，1099=内部错误",
            example = "0")
    private Integer code;

    /**
     * 【可选】错误描述，失败时填写便于排查。
     */
    @Schema(description = "【可选】错误描述，失败时填写", example = "目标货架位 B-2-3-1 已被占用")
    private String message;

    /**
     * 【可选】仓储操作完成的实际时间，不传时 librax 以收到请求的时间为准。
     */
    @Schema(description = "【可选】仓储操作完成时间（ISO-8601），不传则取服务器接收时间", example = "2024-07-16T11:00:00")
    private LocalDateTime eventTime;
}
