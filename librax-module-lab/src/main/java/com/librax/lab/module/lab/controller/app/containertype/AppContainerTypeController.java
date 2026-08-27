package com.librax.lab.module.lab.controller.app.containertype;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.module.lab.controller.admin.containertype.vo.ContainerTypePageReqVO;
import com.librax.lab.module.lab.controller.admin.containertype.vo.ContainerTypeRespVO;
import com.librax.lab.module.lab.dal.dataobject.containertype.ContainerTypeDO;
import com.librax.lab.module.lab.service.containertype.ContainerTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.librax.lab.framework.common.pojo.CommonResult.success;
import static com.librax.lab.framework.common.pojo.PageParam.PAGE_SIZE_NONE;

@Tag(name = "APP - 容器类型定义")
@RestController
@RequestMapping("/lab/container-type")
@Validated
public class AppContainerTypeController {

    @Resource
    private ContainerTypeService containerTypeService;

    @GetMapping("/list")
    @Operation(summary = "获得容器类型定义List")
    @PermitAll
    public CommonResult<List<ContainerTypeRespVO>> getContainerType() {
        ContainerTypePageReqVO pageReqVO = new ContainerTypePageReqVO();
        pageReqVO.setPageSize(PAGE_SIZE_NONE);
        containerTypeService.getContainerTypePage(pageReqVO);
        List<ContainerTypeDO> list = containerTypeService.getContainerTypePage(pageReqVO).getList();
        return success(BeanUtils.toBean(list, ContainerTypeRespVO.class));
    }



}