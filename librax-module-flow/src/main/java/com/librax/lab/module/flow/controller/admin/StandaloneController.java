package com.librax.lab.module.flow.controller.admin;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.module.flow.engine.standalone.service.StandaloneExecutionService;
import com.librax.lab.module.flow.engine.standalone.vo.StandaloneRunReqVO;
import com.librax.lab.module.flow.engine.standalone.vo.StandaloneRunResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

/**
 * 单独运行 API
 *
 * <p>供可视化编辑器的"试运行"功能、开发调试、补偿执行使用。
 */
@Tag(name = "管理后台 - 流程节点单独运行")
@RestController
@RequestMapping("/flow/standalone")
@Validated
public class StandaloneController {

    @Resource
    private StandaloneExecutionService standaloneExecutionService;

    @PostMapping("/run")
    @Operation(summary = "单独运行指定节点")
    public CommonResult<StandaloneRunResultVO> run(@Valid @RequestBody StandaloneRunReqVO req) {
        return success(standaloneExecutionService.run(req));
    }
}