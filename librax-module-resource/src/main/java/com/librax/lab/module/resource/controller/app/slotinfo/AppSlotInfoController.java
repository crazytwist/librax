package com.librax.lab.module.resource.controller.app.slotinfo;

import com.librax.lab.framework.common.pojo.CommonResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.module.resource.controller.admin.slotinfo.vo.SlotInfoPageReqVO;
import com.librax.lab.module.resource.controller.app.slotinfo.vo.AppSlotInfoListReqVO;
import com.librax.lab.module.resource.controller.app.slotinfo.vo.AppSlotInfoPageReqVO;
import com.librax.lab.module.resource.controller.app.slotinfo.vo.AppSlotInfoRespVO;
import com.librax.lab.module.resource.dal.dataobject.slotinfo.SlotInfoDO;
import com.librax.lab.module.resource.service.slotinfo.SlotInfoService;
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

@Tag(name = "用户 App - 库位")
@RestController
@RequestMapping("/resource/slot-info")
@Validated
public class AppSlotInfoController {

    @Resource
    private SlotInfoService slotInfoService;

    @GetMapping("/get")
    @Operation(summary = "获得库位")
    @Parameter(name = "id", description = "编号", required = true, example = "1")
    @PermitAll
    public CommonResult<AppSlotInfoRespVO> getSlotInfo(@RequestParam("id") Long id) {
        SlotInfoDO slotInfo = slotInfoService.getSlotInfo(id);
        return success(BeanUtils.toBean(slotInfo, AppSlotInfoRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得库位分页")
    @PermitAll
    public CommonResult<PageResult<AppSlotInfoRespVO>> getSlotInfoPage(@Valid AppSlotInfoPageReqVO pageReqVO) {
        PageResult<SlotInfoDO> pageResult = slotInfoService.getSlotInfoPage(
                BeanUtils.toBean(pageReqVO, SlotInfoPageReqVO.class));
        return success(BeanUtils.toBean(pageResult, AppSlotInfoRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得库位列表")
    @PermitAll
    public CommonResult<List<AppSlotInfoRespVO>> getSlotInfoList(@Valid AppSlotInfoListReqVO listReqVO) {
        SlotInfoPageReqVO pageReqVO = BeanUtils.toBean(listReqVO, SlotInfoPageReqVO.class);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<SlotInfoDO> list = slotInfoService.getSlotInfoPage(pageReqVO).getList();
        return success(BeanUtils.toBean(list, AppSlotInfoRespVO.class));
    }

}
