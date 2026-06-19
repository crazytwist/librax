package com.librax.lab.module.device.driver;

import com.librax.lab.module.device.dal.dataobject.devicecommand.DeviceCommandDO;
import com.librax.lab.module.device.dal.dataobject.deviceinfo.DeviceInfoDO;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockDeviceDriver implements DeviceDriver {

    @Override
    public String supportProtocol() {
        return "MOCK";
    }

    @Override
    public DeviceSendResult send(DeviceInfoDO device, DeviceCommandDO command, String requestBody, String executionId, String callbackToken) {
        return DeviceSendResult.of("MockID" + UUID.randomUUID(), null);
    }

    @Override
    public void cancel(DeviceInfoDO device, String taskId) {

    }


}