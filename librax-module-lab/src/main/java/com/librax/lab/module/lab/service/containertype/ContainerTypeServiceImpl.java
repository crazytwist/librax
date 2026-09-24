package com.librax.lab.module.lab.service.containertype;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import com.librax.lab.module.lab.dal.mysql.materialinstance.MaterialInstanceMapper;
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

    @Resource
    private MaterialInstanceMapper materialInstanceMapper;

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

        ContainerTypeDO containerTypeDO = containerTypeMapper.selectById(updateReqVO.getId());
        //禁用时检查是否有绑定
        if( !updateReqVO.getEnabled()){
            MaterialInstanceDO instanceDO = materialInstanceMapper.selectOne(new LambdaQueryWrapper<MaterialInstanceDO>()
                    .eq(MaterialInstanceDO::getTypeCode,containerTypeDO.getTypeCode())
                    .eq(MaterialInstanceDO::getDeleted,false)
                    .apply("limit 1"));
            if(instanceDO!=null){
                throw  exception(CONTAIN_DEF_HAS_CONNECT);
            }
        }
        // 更新
        ContainerTypeDO updateObj = BeanUtils.toBean(updateReqVO, ContainerTypeDO.class);
        containerTypeMapper.updateById(updateObj);
        //更新绑定
        MaterialInstanceDO instanceDO = materialInstanceMapper.selectOne(new LambdaQueryWrapper<MaterialInstanceDO>()
                .eq(MaterialInstanceDO::getTypeCode,containerTypeDO.getTypeCode()));
        instanceDO.setTypeCode(updateReqVO.getTypeCode());
        materialInstanceMapper.updateById(instanceDO);
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

    @Override
    public Map<String, String> getTypeNameMap(Collection<String> typeCodes) {
        if (CollUtil.isEmpty(typeCodes)) {
            return Collections.emptyMap();
        }
        List<ContainerTypeDO> list = containerTypeMapper.selectListByTypeCodes(typeCodes);
        Map<String, String> map = new HashMap<>();
        for (ContainerTypeDO item : list) {
            map.put(item.getTypeCode(), item.getTypeName());
        }
        return map;
    }

    @Override
    public Map<String, ContainerTypeDO> getContainerTypeMap(Collection<String> typeCodes) {
        if (CollUtil.isEmpty(typeCodes)) {
            return Collections.emptyMap();
        }
        List<ContainerTypeDO> list = containerTypeMapper.selectListByTypeCodes(typeCodes);
        Map<String, ContainerTypeDO> map = new HashMap<>();
        for (ContainerTypeDO item : list) {
            map.put(item.getTypeCode(), item);
        }
        return map;
    }

}