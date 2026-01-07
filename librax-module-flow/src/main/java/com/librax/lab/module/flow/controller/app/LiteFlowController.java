package com.librax.lab.module.flow.controller.app;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.tenant.core.aop.TenantIgnore;
import com.librax.lab.module.flow.dal.FlowContext;
import com.librax.lab.module.flow.dal.NodeConfig;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

@Tag(name = "流程启动测试")
@RestController
@RequestMapping("/flow")
public class LiteFlowController {

    @Resource
    private FlowExecutor flowExecutor;


    @PostMapping("/start")
    @Operation(summary = "流程驱动")
    @PermitAll
    @TenantIgnore
    public CommonResult<String> start() {
        String el = "THEN(virtualNode, dynamicNode, dynamicNode)";
        FlowContext ctx = new FlowContext();
        ctx.setCurrentNodeId("N1");
        ctx.getVariables().put("status", "OK");

        // 执行 LiteFlow EL 表达式
        LiteflowResponse response = flowExecutor.execute2RespWithEL(el, ctx);

        // 输出整个流程执行结果
        System.out.println("流程执行结果: " + response.isSuccess());

        return CommonResult.success("ok");
    }

}
