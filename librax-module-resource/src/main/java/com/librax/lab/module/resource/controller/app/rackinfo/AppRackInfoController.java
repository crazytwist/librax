package com.librax.lab.module.resource.controller.app.rackinfo;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.module.resource.controller.admin.rackinfo.vo.RackInfoPageReqVO;
import com.librax.lab.module.resource.controller.app.rackinfo.vo.AppRackInfoListReqVO;
import com.librax.lab.module.resource.controller.app.rackinfo.vo.AppRackInfoPageReqVO;
import com.librax.lab.module.resource.controller.app.rackinfo.vo.AppRackInfoRespVO;
import com.librax.lab.module.resource.dal.dataobject.rackinfo.RackInfoDO;
import com.librax.lab.module.resource.service.rackinfo.RackInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.librax.lab.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 货架")
@RestController
@RequestMapping("/resource/rack-info")
@Validated
public class AppRackInfoController {

    @Resource
    private RackInfoService rackInfoService;

    @GetMapping("/get")
    @Operation(summary = "获得货架")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PermitAll
    public CommonResult<AppRackInfoRespVO> getRackInfo(@RequestParam("id") Long id) {
        RackInfoDO rackInfo = rackInfoService.getRackInfo(id);
        return success(BeanUtils.toBean(rackInfo, AppRackInfoRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得货架分页")
    @PermitAll
    public CommonResult<PageResult<AppRackInfoRespVO>> getRackInfoPage(@Valid AppRackInfoPageReqVO pageReqVO) {
        PageResult<RackInfoDO> pageResult = rackInfoService.getRackInfoPage(
                BeanUtils.toBean(pageReqVO, RackInfoPageReqVO.class));
        return success(BeanUtils.toBean(pageResult, AppRackInfoRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得货架列表")
    @PermitAll
    public CommonResult<List<AppRackInfoRespVO>> getRackInfoList(@Valid AppRackInfoListReqVO listReqVO) {
        RackInfoPageReqVO pageReqVO = BeanUtils.toBean(listReqVO, RackInfoPageReqVO.class);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<RackInfoDO> list = rackInfoService.getRackInfoPage(pageReqVO).getList();
        return success(BeanUtils.toBean(list, AppRackInfoRespVO.class));
    }

}
