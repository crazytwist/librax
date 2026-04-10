package com.librax.lab.module.device.driver;


import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;

public interface DeviceDriver {

    /** 支持的协议，对应 lab_device_info.protocol */
    String supportProtocol();

    /**
     * 发送指令（非阻塞）
     * @return taskId
     */
    String send(DeviceInfoDO device,
                DeviceCommandDO command,
                String requestBody,
                String executionId,
                String callbackToken);

    /** 取消任务 */
    void cancel(DeviceInfoDO device, String taskId);
}
