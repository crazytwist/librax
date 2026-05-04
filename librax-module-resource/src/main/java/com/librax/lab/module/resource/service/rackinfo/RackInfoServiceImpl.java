package com.librax.lab.module.resource.service.rackinfo;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.resource.controller.admin.rackinfo.vo.*;
import com.librax.lab.module.resource.dal.dataobject.rackinfo.RackInfoDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.resource.dal.mysql.rackinfo.RackInfoMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.resource.enums.ErrorCodeConstants.*;

/**
 * 货架/台面定义，库位的上级容器，归 resource 模块管理 Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class RackInfoServiceImpl implements RackInfoService {

    @Resource
    private RackInfoMapper rackInfoMapper;

    @Override
    public Long createRackInfo(RackInfoSaveReqVO createReqVO) {
        // 插入
        RackInfoDO rackInfo = BeanUtils.toBean(createReqVO, RackInfoDO.class);
        rackInfoMapper.insert(rackInfo);

        // 返回
        return rackInfo.getId();
    }

    @Override
    public void updateRackInfo(RackInfoSaveReqVO updateReqVO) {
        // 校验存在
        validateRackInfoExists(updateReqVO.getId());
        // 更新
        RackInfoDO updateObj = BeanUtils.toBean(updateReqVO, RackInfoDO.class);
        rackInfoMapper.updateById(updateObj);
    }

    @Override
    public void deleteRackInfo(Long id) {
        // 校验存在
        validateRackInfoExists(id);
        // 删除
        rackInfoMapper.deleteById(id);
    }

    @Override
        public void deleteRackInfoListByIds(List<Long> ids) {
        // 删除
        rackInfoMapper.deleteByIds(ids);
        }


    private void validateRackInfoExists(Long id) {
        if (rackInfoMapper.selectById(id) == null) {
            throw exception(RACK_INFO_NOT_EXISTS);
        }
    }

    @Override
    public RackInfoDO getRackInfo(Long id) {
        return rackInfoMapper.selectById(id);
    }

    @Override
    public PageResult<RackInfoDO> getRackInfoPage(RackInfoPageReqVO pageReqVO) {
        return rackInfoMapper.selectPage(pageReqVO);
    }

}