package com.librax.lab.module.resource.controller.app.slotinfo.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓储备料完成回调请求体。
 *
 * <p>仓储机械臂将物料从货架取出并放置到中转位（出货口）后，
 * 调用 {@code POST /resource/slot/ready} 通知 librax 系统库位已就绪，
 * librax 随即驱动 AGV 前来取料。
 *
 * <p><b>字段必填说明：</b>
 * <ul>
 *   <li>{@code requestId}、{@code slotId}、{@code instanceId}、{@code status} — 必填</li>
 *   <li>{@code taskId} — 联动补料流程时必填；仓储独立出货时可不传</li>
 *   <li>{@code code}、{@code message}、{@code eventTime} — 可选，建议失败时填写 code + message 便于追溯</li>
 * </ul>
 */
@Data
@Schema(description = "仓储备料完成回调请求体（仓储 → librax）")
public class SlotReadyReqVO {

    /**
     * 【必填】仓储系统生成的事件唯一ID，用于幂等去重，防止网络重试导致重复处理。
     * 建议格式：{设备ID}-{时间戳}-{序号}，例如 {@code WH-01-20240716-0001}。
     */
    @Schema(description = "【必填】事件唯一ID（幂等键），仓储侧生成，防止重复通知",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "WH-01-20240716-0001")
    @NotBlank(message = "requestId不能为空")
    private String requestId;

    /**
     * 【补料联动时必填】AGV 补料流程执行ID，即 librax 发送 prepareMaterials 时携带的 executionId。
     * librax 依据此 ID 定位对应的补料计划并推进下一步（驱动 AGV 取料）。
     * 若为仓储独立出货场景（不经由 librax 补料流程），可不传或传空。
     * 也可使用别名字段名 {@code transferTaskId}。
     */
    @Schema(description = "【补料联动必填】补料流程执行ID（即 prepareMaterials 请求中的 executionId）；独立出货可不传",
            example = "a1b2c3d4e5f6...")
    @JsonAlias("transferTaskId")
    private String taskId;

    /**
     * 【必填】物料已就绪的中转位库位ID（出货口），即 librax 发送 prepareMaterials 时指定的 toLocation。
     * AGV 将来此库位取料。
     * 也可使用别名字段名 {@code toLocation}。
     */
    @Schema(description = "【必填】已就绪的中转位库位ID（即 prepareMaterials 中的 toLocation）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "R-E-1")
    @NotBlank(message = "slotId不能为空")
    @JsonAlias("toLocation")
    private String slotId;

    /**
     * 【必填】物料实例ID，与 librax 补料请求中的 instanceId 一致，用于校验物料一致性。
     * 也可使用别名字段名 {@code materialId}。
     */
    @Schema(description = "【必填】物料实例ID，与补料请求中的 instanceId 一致",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "MAT-20240716-001")
    @NotBlank(message = "instanceId不能为空")
    @JsonAlias("materialId")
    private String instanceId;

    /**
     * 【必填】操作结果状态。
     * <ul>
     *   <li>{@code READY} — 物料已成功放置到中转位，AGV 可取料</li>
     *   <li>{@code FAILED} — 仓储操作失败，librax 将终止对应补料流程</li>
     * </ul>
     */
    @Schema(description = "【必填】操作结果：READY=就绪可取料，FAILED=失败",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "READY",
            allowableValues = {"READY", "FAILED"})
    @NotBlank(message = "status不能为空")
    private String status;

    /**
     * 【可选，失败时建议填写】仓储业务错误码。
     * <ul>
     *   <li>{@code 0} — 成功（status=READY 时默认）</li>
     *   <li>{@code 1001} — 物料不存在或库存不足</li>
     *   <li>{@code 1002} — 始发货架位无效</li>
     *   <li>{@code 1003} — 中转位异常（被占用或不可用）</li>
     *   <li>{@code 1004} — 机械臂执行失败</li>
     *   <li>{@code 1005} — 幂等成功（已处理过相同 requestId）</li>
     *   <li>{@code 1099} — 仓储内部错误</li>
     * </ul>
     */
    @Schema(description = "【可选】错误码（失败时建议填写）：0=成功，1001=缺料，1002=始发位无效，1003=中转位异常，1004=机械臂失败，1005=幂等成功，1099=内部错误",
            example = "0")
    private Integer code;

    /**
     * 【可选】错误描述，失败时填写便于排查。
     */
    @Schema(description = "【可选】错误描述，失败时填写", example = "货架位 A-1-2-3 物料已被取走")
    private String message;

    /**
     * 【可选】仓储操作完成的实际时间，不传时 librax 以收到请求的时间为准。
     */
    @Schema(description = "【可选】仓储操作完成时间（ISO-8601），不传则取服务器接收时间", example = "2024-07-16T10:30:00")
    private LocalDateTime eventTime;
}
