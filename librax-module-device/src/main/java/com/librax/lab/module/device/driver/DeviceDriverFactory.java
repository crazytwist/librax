package com.librax.lab.module.device.driver;

import com.librax.lab.module.device.exception.DeviceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DeviceDriverFactory {

    private final Map<String, DeviceDriver> driverMap;

    public DeviceDriverFactory(List<DeviceDriver> drivers) {
        this.driverMap = drivers.stream()
                .filter(d -> d.supportProtocol() != null)
                .collect(Collectors.toMap(
                        DeviceDriver::supportProtocol,
                        Function.identity()));
        log.info("[DeviceDriverFactory] 已注册驱动: {}", driverMap.keySet());
    }

    public DeviceDriver getDriver(String protocol) {
        DeviceDriver driver = driverMap.get(protocol);
        if (driver == null) {
            throw new DeviceException("不支持的协议: " + protocol);
        }
        return driver;
    }
}

