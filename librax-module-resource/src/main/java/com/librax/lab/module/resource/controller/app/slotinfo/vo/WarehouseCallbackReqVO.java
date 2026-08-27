package com.librax.lab.module.resource.controller.app.slotinfo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 仓储机械臂操作完成回调请求体（补料与下料统一接口）。
 *
 * <p>字段与 librax 下发的 {@code prepareMaterials} 请求完全对应，
 * 仓储设备<b>原样透传</b>即可，无需重新命名：
 *
 * <pre>
 * librax → 仓储 (prepareMaterials)         仓储 → librax (warehouse-callback)
 * ─────────────────────────────────────    ────────────────────────────────────
 * requestId      (幂等键)              →   requestId      (必填，原样回传)
 * transferTaskId (流程ID)              →   transferTaskId (联动流程必填，原样回传)
 * materialId     (物料实例ID)          →   materialId     (必填，原样回传)
 * containerType  (容器类型)            →   containerType  (可选，原样回传)
 * fromLocation   (来源位)              →   fromLocation   (原样回传)
 * toLocation     (目标位)              →   toLocation     (原样回传)
 * callbackUrl    (回调地址，无需回传)
 *                                          status         (新增：SUCCESS/FAILED)
 *                                          code           (新增：可选错误码)
 *                                          message        (新增：可选错误描述)
 *                                          eventTime      (新增：可选操作时间)
 * </pre>
 *
 * <p><b>补料方向</b>：中转位 = {@code toLocation}（仓储将物料放到此处，AGV 来取）<br>
 * <b>下料方向</b>：中转位 = {@code fromLocation}（AGV 将物料放在此处，仓储从此取走入库）
 */
@Data
@Schema(description = "仓储机械臂操作完成回调（补料/下料统一接口，字段与 prepareMaterials 请求对应，原样透传即可）")
public class WarehouseCallbackReqVO {

    /**
     * 【必填】幂等键，对应 prepareMaterials 请求中的 {@code requestId}，原样回传。
     * librax 据此做幂等去重，防止网络重试导致重复处理。
     */
    @Schema(description = "【必填】幂等键，与 prepareMaterials 请求中的 requestId 对应，原样回传",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "execId-W1-I1")
    @NotBlank(message = "requestId不能为空")
    private String requestId;

    /**
     * 【联动流程时必填】流程ID，对应 prepareMaterials 请求中的 {@code transferTaskId}，原样回传。
     * librax 据此找到对应的补料或下料计划并推进下一步。
     * 纯独立仓储操作（不经由 librax 流程）可不传。
     */
    @Schema(description = "【联动流程必填】流程ID，与 prepareMaterials 请求中的 transferTaskId 对应，原样回传；独立操作可不传",
            example = "a1b2c3d4e5f6...")
    private String transferTaskId;

    /**
     * 【必填】物料实例ID，对应 prepareMaterials 请求中的 {@code materialId}，原样回传。
     */
    @Schema(description = "【必填】物料实例ID，与 prepareMaterials 请求中的 materialId 对应，原样回传",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "MAT-20240716-001")
    @NotBlank(message = "materialId不能为空")
    private String materialId;

    /**
     * 【可选】容器类型，对应 prepareMaterials 请求中的 {@code containerType}，原样回传。
     */
    @Schema(description = "【可选】容器类型，与 prepareMaterials 请求中的 containerType 对应，原样回传",
            example = "TYPE_A")
    private String containerType;

    /**
     * 【原样回传】来源位，对应 prepareMaterials 请求中的 {@code fromLocation}。
     * <ul>
     *   <li>补料方向：货架位（仓储从此取料）</li>
     *   <li>下料方向：中转位（仓储从此取料入库）← librax 用此字段定位下料明细</li>
     * </ul>
     */
    @Schema(description = "来源位，与 prepareMaterials 请求中的 fromLocation 对应，原样回传",
            example = "R-E-1")
    private String fromLocation;

    /**
     * 【原样回传】目标位，对应 prepareMaterials 请求中的 {@code toLocation}。
     * <ul>
     *   <li>补料方向：中转位（仓储将料放到此处）← librax 用此字段定位补料明细</li>
     *   <li>下料方向：货架位（仓储将料放回此处）</li>
     * </ul>
     */
    @Schema(description = "目标位，与 prepareMaterials 请求中的 toLocation 对应，原样回传",
            example = "R-E-1")
    private String toLocation;

    /**
     * 【必填】操作结果状态。
     * <ul>
     *   <li>{@code SUCCESS} — 操作成功</li>
     *   <li>{@code FAILED} — 操作失败，librax 将终止对应流程</li>
     * </ul>
     */
    @Schema(description = "【必填】操作结果：SUCCESS=成功，FAILED=失败",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "SUCCESS",
            allowableValues = {"SUCCESS", "FAILED"})
    @NotBlank(message = "status不能为空")
    private String status;

    /**
     * 【可选，失败时建议填写】仓储业务错误码。
     * <ul>
     *   <li>{@code 0} — 成功</li>
     *   <li>{@code 1001} — 物料不存在或库存不足</li>
     *   <li>{@code 1002} — 源位置无物料</li>
     *   <li>{@code 1003} — 目标位置不可用</li>
     *   <li>{@code 1004} — 机械臂忙或执行失败</li>
     *   <li>{@code 1005} — 幂等成功（已处理过相同 requestId）</li>
     *   <li>{@code 1099} — 仓储内部错误</li>
     * </ul>
     */
    @Schema(description = "【可选】错误码（失败时建议填写）：0=成功，1001=物料不存在，1002=源位无物料，1003=目标位不可用，1004=机械臂失败，1005=幂等成功，1099=内部错误",
            example = "0")
    private Integer code;

    /**
     * 【可选】错误描述，失败时填写便于排查。
     */
    @Schema(description = "【可选】错误描述，失败时填写", example = "目标位 B-2-3-1 已被占用")
    private String message;

    /**
     * 【可选】仓储操作完成的实际时间，不传时 librax 以收到请求的时间为准。
     */
    @Schema(description = "【可选】操作完成时间（ISO-8601），不传则取服务器接收时间", example = "2024-07-16T10:30:00")
    private LocalDateTime eventTime;

    /** 判断本次操作是否成功。 */
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }
}
