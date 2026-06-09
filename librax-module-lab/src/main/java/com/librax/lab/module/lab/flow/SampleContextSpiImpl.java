package com.librax.lab.module.lab.flow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.librax.lab.module.flow.api.sample.SampleContextSpi;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleInfoDO;
import com.librax.lab.module.lab.dal.mysql.sample.SampleInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

/**
 * 样本上下文 SPI 实现
 *
 * <p>从 lab_sample_info.experiment_params 读取实验参数，
 * 注入到 flow 引擎每个步骤的 inputParams 基础层。
 *
 * <p>注入优先级（低 → 高）：
 *   sample.experiment_params < pipeline YAML params < inputMapping 解析结果
 *
 * <p>示例：样本登记时设置 experiment_params = {"targetPh": 7.2, "volumeUl": 500}，
 * 步骤 YAML 中通过 ${input.targetPh} 引用，无需在每个步骤重复配置。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleContextSpiImpl implements SampleContextSpi {

    private final SampleInfoMapper sampleInfoMapper;

    @Override
    public Map<String, Object> getExperimentParams(String sampleId) {
        SampleInfoDO sample = sampleInfoMapper.selectBySampleId(sampleId);
        if (sample == null) {
            log.debug("[SampleContextSpi] 样本不存在，跳过参数注入 sampleId={}", sampleId);
            return Collections.emptyMap();
        }
        String json = sample.getExperimentParams();
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> params = JSON.parseObject(json, new TypeReference<Map<String, Object>>() {});
            return params != null ? params : Collections.emptyMap();
        } catch (Exception e) {
            log.warn("[SampleContextSpi] experimentParams JSON 解析失败，跳过注入 sampleId={} json={}",
                    sampleId, json, e);
            return Collections.emptyMap();
        }
    }
}
