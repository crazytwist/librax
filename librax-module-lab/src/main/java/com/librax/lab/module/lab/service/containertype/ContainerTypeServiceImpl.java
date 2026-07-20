package com.librax.lab.module.lab.service.containertype;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import com.librax.lab.module.lab.controller.admin.containertype.vo.*;
import com.librax.lab.module.lab.dal.dataobject.containertype.ContainerTypeDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.containertype.ContainerTypeMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 容器类型定义，描述各类实验容器的规格和层级结构，归 lab 模块管理 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ContainerTypeServiceImpl implements ContainerTypeService {

    @Resource
    private ContainerTypeMapper containerTypeMapper;

    @Override
    public Long createContainerType(ContainerTypeSaveReqVO createReqVO) {
        // 插入
        ContainerTypeDO containerType = BeanUtils.toBean(createReqVO, ContainerTypeDO.class);
        containerTypeMapper.insert(containerType);

        // 返回
        return containerType.getId();
    }

    @Override
    public void updateContainerType(ContainerTypeSaveReqVO updateReqVO) {
        // 校验存在
        validateContainerTypeExists(updateReqVO.getId());
        // 更新
        ContainerTypeDO updateObj = BeanUtils.toBean(updateReqVO, ContainerTypeDO.class);
        containerTypeMapper.updateById(updateObj);
    }

    @Override
    public void deleteContainerType(Long id) {
        // 校验存在
        validateContainerTypeExists(id);
        // 删除
        containerTypeMapper.deleteById(id);
    }

    @Override
    public void deleteContainerTypeListByIds(List<Long> ids) {
        // 删除
        containerTypeMapper.deleteByIds(ids);
    }


    private void validateContainerTypeExists(Long id) {
        if (containerTypeMapper.selectById(id) == null) {
            throw exception(CONTAINER_TYPE_NOT_EXISTS);
        }
    }

    @Override
    public ContainerTypeDO getContainerType(Long id) {
        return containerTypeMapper.selectById(id);
    }

    @Override
    public PageResult<ContainerTypeDO> getContainerTypePage(ContainerTypePageReqVO pageReqVO) {
        return containerTypeMapper.selectPage(pageReqVO);
    }

}