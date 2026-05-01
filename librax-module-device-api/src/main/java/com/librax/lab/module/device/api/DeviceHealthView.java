package com.librax.lab.module.device.api;

import com.librax.lab.module.device.api.enums.DeviceHealthStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 设备健康视图(只读)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceHealthView {

    /**
     * 设备 ID
     */
    private String deviceId;

    /**
     * 是否在线(心跳正常)
     */
    private boolean online;

    /**
     * 健康等级
     */
    private DeviceHealthStatusEnum healthStatus;

    /**
     * 最后一次心跳时间(离线判断依据)
     */
    private LocalDateTime lastHeartbeatAt;

    /**
     * 最近错误信息(故障排查用,可为空)
     */
    private String lastErrorMsg;

    /**
     * 是否可被调度使用
     * <p>
     * 便捷方法:在线 + 健康状态允许使用
     */
    public boolean isUsable() {
        return online && healthStatus != null && healthStatus.isUsable();
    }
}