package com.librax.lab.module.lab.service.materialcheck;

import com.librax.lab.module.flow.api.material.MaterialCheckRequest;
import com.librax.lab.module.flow.api.material.MaterialCheckResult;
import com.librax.lab.module.flow.api.material.MaterialCheckSpi;
import com.librax.lab.module.lab.dal.dataobject.materialcheckrule.MaterialCheckRuleDO;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import com.librax.lab.module.lab.dal.mysql.materialcheckrule.MaterialCheckRuleMapper;
import com.librax.lab.module.lab.dal.mysql.materialinstance.MaterialInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 物料核验 SPI 实现
 * <p>
 * 按 pipelineStepId 查出该步骤的所有物料检查规则，
 * 逐条检查库存中是否有满足条件的 AVAILABLE 物料实例。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MaterialCheckSpiImpl implements MaterialCheckSpi {

    private final MaterialCheckRuleMapper ruleMapper;
    private final MaterialInstanceMapper instanceMapper;

    @Override
    public MaterialCheckResult check(MaterialCheckRequest request) {
        List<MaterialCheckRuleDO> rules = ruleMapper.selectByPipelineStepId(
                request.getPipelineStepId());

        if (rules.isEmpty()) {
            // 该步骤没有配物料检查规则，直接放行
            return MaterialCheckResult.ok();
        }

        List<String> failReasons = new ArrayList<>();

        for (MaterialCheckRuleDO rule : rules) {
            // 确定查询区域：规则级 > 步骤级
            String zoneCode = rule.getZoneCode() != null
                    ? rule.getZoneCode() : request.getZoneCode();

            List<MaterialInstanceDO> available = instanceMapper.selectAvailable(
                    rule.getMaterialCode(), rule.getContentType(), zoneCode);

            // 检查体积（液体类）
            if (rule.getMinVolUl() != null) {
                BigDecimal totalVol = available.stream()
                        .map(MaterialInstanceDO::getCurrentVolUl)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (totalVol.compareTo(rule.getMinVolUl()) < 0) {
                    failReasons.add(String.format(
                            "物料[%s/%s]体积不足: 需要%sμL, 可用%sμL (区域=%s)",
                            rule.getMaterialCode(), rule.getContentType(),
                            rule.getMinVolUl(), totalVol, zoneCode));
                }
            }

            // 检查数量（固体/耗材类）
            if (rule.getMinCount() != null) {
                int count = available.size();
                if (count < rule.getMinCount()) {
                    failReasons.add(String.format(
                            "物料[%s/%s]数量不足: 需要%d个, 可用%d个 (区域=%s)",
                            rule.getMaterialCode(), rule.getContentType(),
                            rule.getMinCount(), count, zoneCode));
                }
            }
        }

        if (failReasons.isEmpty()) {
            log.debug("[MaterialCheck] 物料核验通过 executionId={} nodeId={} ruleCount={}",
                    request.getExecutionId(), request.getNodeId(), rules.size());
            return MaterialCheckResult.ok();
        }

        log.warn("[MaterialCheck] 物料核验不通过 executionId={} nodeId={} failures={}",
                request.getExecutionId(), request.getNodeId(), failReasons);
        return MaterialCheckResult.fail(failReasons);
    }
}
