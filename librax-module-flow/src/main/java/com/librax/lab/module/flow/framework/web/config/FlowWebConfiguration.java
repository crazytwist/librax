package com.librax.lab.module.flow.framework.web.config;

import com.librax.lab.framework.swagger.config.LibraxSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * flow 模块的 web 组件的 Configuration
 *
 */
@Configuration(proxyBeanMethods = false)
public class FlowWebConfiguration {


    /**
     * flow 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi flowGroupedOpenApi() {
        return LibraxSwaggerAutoConfiguration.buildGroupedOpenApi("flow");
    }

}
