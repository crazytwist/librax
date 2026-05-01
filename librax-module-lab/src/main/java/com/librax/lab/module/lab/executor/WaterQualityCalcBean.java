package com.librax.lab.module.lab.executor;

import com.librax.lab.module.flow.api.dispatch.StepDispatchContext;
import com.librax.lab.module.flow.api.enums.StepTypeEnum;
import com.librax.lab.module.flow.api.executor.StepExecutor;
import com.librax.lab.module.flow.api.model.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component("waterQualityCalcBean")
@RequiredArgsConstructor
public class WaterQualityCalcBean implements StepExecutor {

    @Override
    public StepTypeEnum supportType() {
        return StepTypeEnum.COMPUTE;
    }

    @Override
    public StepResult execute(StepDispatchContext ctx) {
        Object phRaw = ctx.getInputParams().get("ph");
        if (phRaw == null) {
            return StepResult.fail("MISSING_PH", "缺少 PH 参数");
        }

        // 兼容 BigDecimal / Double / Integer 等各种 JSON 反序列化类型
        double ph = ((Number) phRaw).doubleValue();

        boolean qualified = ph >= 6.5 && ph <= 8.5;
        int score = calcScore(ph);
        String verdict = qualified ? "PASS" : "FAIL";

        log.info("[WaterQualityCalc] ph={} qualified={} score={}", ph, qualified, score);

        return StepResult.ok(Map.of(
                "qualified", qualified,
                "qualityScore", score,
                "verdict", verdict
        ));
    }

    private int calcScore(double ph) {
        // 越接近 7.0 分数越高
        double distance = Math.abs(ph - 7.0);
        return (int) Math.max(0, 100 - distance * 20);
    }
}