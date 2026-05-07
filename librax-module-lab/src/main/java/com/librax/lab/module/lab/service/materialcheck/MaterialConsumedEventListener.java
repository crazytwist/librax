package com.librax.lab.module.lab.service.materialcheck;

import com.librax.lab.module.flow.api.material.MaterialConsumedEvent;
import com.librax.lab.module.lab.dal.dataobject.materialcheckrule.MaterialCheckRuleDO;
import com.librax.lab.module.lab.dal.dataobject.materialconsumption.MaterialConsumptionDO;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import com.librax.lab.module.lab.dal.mysql.materialcheckrule.MaterialCheckRuleMapper;
import com.librax.lab.module.lab.dal.mysql.materialconsumption.MaterialConsumptionMapper;
import com.librax.lab.module.lab.dal.mysql.materialinstance.MaterialInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 物料消耗事件监听器
 * <p>
 * 监听 flow 引擎步骤成功后发出的 {@link MaterialConsumedEvent}，执行：
 * <ol>
 *   <li>按检查规则找到该步骤消耗的物料
 *   <li>扣减库存（current_vol_ul 或 status 更新）
 *   <li>写入消耗记录（lab_material_consumption）
 * </ol>
 * <p>
 * 异步执行，不阻塞调度主链路。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialConsumedEventListener {

    private final MaterialCheckRuleMapper ruleMapper;
    private final MaterialInstanceMapper instanceMapper;
    private final MaterialConsumptionMapper consumptionMapper;

    @Async
    @EventListener
    public void onMaterialConsumed(MaterialConsumedEvent event) {
        if (event.getPipelineStepId() == null) {
            return;
        }

        List<MaterialCheckRuleDO> rules = ruleMapper.selectByPipelineStepId(
                event.getPipelineStepId());
        if (rules.isEmpty()) {
            return;
        }

        log.info("[MaterialConsumed] 开始扣减 executionId={} nodeId={} ruleCount={}",
                event.getExecutionId(), event.getNodeId(), rules.size());

        for (MaterialCheckRuleDO rule : rules) {
            try {
                processRule(event, rule);
            } catch (Exception e) {
                log.error("[MaterialConsumed] 扣减异常 executionId={} nodeId={} materialCode={}",
                        event.getExecutionId(), event.getNodeId(),
                        rule.getMaterialCode(), e);
            }
        }
    }

    private void processRule(MaterialConsumedEvent event, MaterialCheckRuleDO rule) {
        String zoneCode = rule.getZoneCode() != null
                ? rule.getZoneCode() : event.getZoneCode();

        List<MaterialInstanceDO> available = instanceMapper.selectAvailable(
                rule.getMaterialCode(), rule.getContentType(), zoneCode);

        if (available.isEmpty()) {
            log.warn("[MaterialConsumed] 无可用物料实例 materialCode={} zoneCode={}",
                    rule.getMaterialCode(), zoneCode);
            return;
        }

        // 液体类：从第一个可用实例扣减体积
        if (rule.getMinVolUl() != null) {
            deductVolume(event, rule, available);
        }

        // 固体/耗材类：标记第一个可用实例为 IN_USE
        if (rule.getMinCount() != null && rule.getMinVolUl() == null) {
            deductCount(event, rule, available);
        }
    }

    /**
     * 液体扣减：按需从多个实例依次扣减
     */
    private void deductVolume(MaterialConsumedEvent event,
                              MaterialCheckRuleDO rule,
                              List<MaterialInstanceDO> available) {
        BigDecimal remaining = rule.getMinVolUl();

        for (MaterialInstanceDO instance : available) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
            if (instance.getCurrentVolUl() == null) continue;

            BigDecimal volBefore = instance.getCurrentVolUl();
            BigDecimal deduct = volBefore.min(remaining);
            BigDecimal volAfter = volBefore.subtract(deduct);

            // 更新实例体积
            instance.setCurrentVolUl(volAfter);
            if (volAfter.compareTo(BigDecimal.ZERO) <= 0) {
                instance.setStatus("USED");
            }
            instanceMapper.updateById(instance);

            // 写消耗记录
            consumptionMapper.insert(buildConsumption(
                    event, instance, "CONSUME",
                    volBefore, deduct.negate(), volAfter));

            remaining = remaining.subtract(deduct);

            log.info("[MaterialConsumed] 体积扣减 instanceId={} deduct={}μL after={}μL",
                    instance.getInstanceId(), deduct, volAfter);
        }
    }

    /**
     * 固体/耗材扣减：标记为 IN_USE
     */
    private void deductCount(MaterialConsumedEvent event,
                             MaterialCheckRuleDO rule,
                             List<MaterialInstanceDO> available) {
        int needed = rule.getMinCount();
        int consumed = 0;

        for (MaterialInstanceDO instance : available) {
            if (consumed >= needed) break;

            instance.setStatus("IN_USE");
            instanceMapper.updateById(instance);

            consumptionMapper.insert(buildConsumption(
                    event, instance, "CONSUME",
                    instance.getCurrentVolUl(), null, instance.getCurrentVolUl()));

            consumed++;

            log.info("[MaterialConsumed] 耗材消耗 instanceId={}", instance.getInstanceId());
        }
    }

    private MaterialConsumptionDO buildConsumption(MaterialConsumedEvent event,
                                                    MaterialInstanceDO instance,
                                                    String action,
                                                    BigDecimal volBefore,
                                                    BigDecimal volChange,
                                                    BigDecimal volAfter) {
        return MaterialConsumptionDO.builder()
                .executionId(event.getExecutionId())
                .nodeId(event.getNodeId())
                .attempt(event.getAttempt())
                .instanceId(instance.getInstanceId())
                .typeCode(instance.getTypeCode())
                .materialCode(instance.getMaterialCode())
                .batchNo(instance.getBatchNo())
                .action(action)
                .volBeforeUl(volBefore)
                .volChangeUl(volChange)
                .volAfterUl(volAfter)
                .consumedAt(LocalDateTime.now())
                .build();
    }
}
