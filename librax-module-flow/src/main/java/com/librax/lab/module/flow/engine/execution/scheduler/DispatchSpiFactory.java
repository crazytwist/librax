package com.librax.lab.module.flow.engine.execution.scheduler;

import com.librax.lab.module.flow.api.dispatch.DispatchSpi;
import com.librax.lab.module.flow.enums.StepDispatchModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DispatchSpi 工厂
 * <p>
 * StepSubmitter 通过此工厂获取对应的分发实现，
 * 不直接依赖 device 或 task 模块的任何类。
 */
@Slf4j
@Component
public class DispatchSpiFactory {

    private final Map<String, DispatchSpi> spiMap;

    public DispatchSpiFactory(List<DispatchSpi> spis) {
        this.spiMap = spis.stream()
                .collect(Collectors.toMap(
                        DispatchSpi::supportMode,
                        Function.identity()));
        log.info("[DispatchSpiFactory] 已注册分发实现: {}", spiMap.keySet());
    }

    public DispatchSpi getSpi(String dispatchMode) {
        // 默认 DIRECT
        String mode = StringUtils.hasText(dispatchMode) ? dispatchMode : "DIRECT";
        DispatchSpi spi = spiMap.get(mode);
        if (spi == null) {
            throw new IllegalArgumentException("未找到分发实现: dispatchMode=" + mode);
        }
        return spi;
    }
}