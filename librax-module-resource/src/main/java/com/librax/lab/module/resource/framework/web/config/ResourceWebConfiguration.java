package com.librax.lab.module.resource.framework.web.config;

import com.librax.lab.framework.swagger.config.LibraxSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * resource 模块的 web 组件的 Configuration
 *
 * @author 芋道源码
 */
@Configuration(proxyBeanMethods = false)
public class ResourceWebConfiguration {


    /**
     * resource 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi resourceGroupedOpenApi() {
        return LibraxSwaggerAutoConfiguration.buildGroupedOpenApi("resource");
    }

}
