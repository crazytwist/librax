package com.librax.lab.module.flow.engine.execution.bean;

import com.librax.lab.framework.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class ReportArchiver {

    public Map<String, Object> archive(Map<String, Object> maps) {
        log.info("开始  ReportArchiver  archive,map :{}", JsonUtils.toJsonString(maps));
        return new HashMap<>();
    }
}
