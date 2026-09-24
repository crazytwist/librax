package com.librax.lab.module.lab.service.materialdef;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.librax.lab.module.lab.dal.dataobject.materialinstance.MaterialInstanceDO;
import com.librax.lab.module.lab.dal.mysql.materialinstance.MaterialInstanceMapper;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.materialdef.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialdef.MaterialDefDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.materialdef.MaterialDefMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MaterialDefServiceImpl implements MaterialDefService {

    @Resource
    private MaterialDefMapper materialDefMapper;

    @Resource
    private MaterialInstanceMapper materialInstanceMapper;

    @Override
    public Long createMaterialDef(MaterialDefSaveReqVO createReqVO) {
        // 插入
        MaterialDefDO materialDef = BeanUtils.toBean(createReqVO, MaterialDefDO.class);
        checkMaterialCodeExist(null,createReqVO.getMaterialCode());
        materialDefMapper.insert(materialDef);

        // 返回
        return materialDef.getId();
    }

    @Override
    public void updateMaterialDef(MaterialDefSaveReqVO updateReqVO) {
        // 校验存在
        validateMaterialDefExists(updateReqVO.getId());
        //检查物料编码重复
        checkMaterialCodeExist(updateReqVO.getId(),updateReqVO.getMaterialCode());
        //如果开启禁用，检查是否绑定物料
        if( !updateReqVO.getEnabled()){
            MaterialInstanceDO instanceDO = materialInstanceMapper.selectOne(new LambdaQueryWrapper<MaterialInstanceDO>()
                    .eq(MaterialInstanceDO::getMaterialCode,updateReqVO.getMaterialCode())
                    .eq(MaterialInstanceDO::getDeleted,false)
                    .last("limit 1"));
            if(instanceDO!=null){
                throw  exception(MATERIAL_DEF_HAS_CONNECT);
            }
        }
        // 更新
        MaterialDefDO updateObj = BeanUtils.toBean(updateReqVO, MaterialDefDO.class);
        materialDefMapper.updateById(updateObj);
    }

    @Override
    public void deleteMaterialDef(Long id) {
        // 校验存在
        validateMaterialDefExists(id);
        // 删除
        materialDefMapper.deleteById(id);
    }

    @Override
        public void deleteMaterialDefListByIds(List<Long> ids) {
        // 删除
        materialDefMapper.deleteByIds(ids);
        }


    private void validateMaterialDefExists(Long id) {
        if (materialDefMapper.selectById(id) == null) {
            throw exception(MATERIAL_DEF_NOT_EXISTS);
        }
    }

    private void checkMaterialCodeExist(Long id,String materialCode){
        LambdaQueryWrapper<MaterialDefDO> wrapper = new LambdaQueryWrapper<MaterialDefDO>()
                .eq(MaterialDefDO::getMaterialCode,materialCode);
        // 更新操作：排除当前id自身记录
        if (id != null) {
            wrapper.ne(MaterialDefDO::getId, id);
        }
        if(materialDefMapper.exists(wrapper)){
            throw  exception(MATERIAL_DEF_CODE_EXISTS);
        }
    }

    @Override
    public MaterialDefDO getMaterialDef(Long id) {
        return materialDefMapper.selectById(id);
    }

    @Override
    public PageResult<MaterialDefDO> getMaterialDefPage(MaterialDefPageReqVO pageReqVO) {
        return materialDefMapper.selectPage(pageReqVO);
    }

    @Override
    public Map<String, MaterialDefDO> getMaterialDefMap(Collection<String> materialCodes) {
        if (CollUtil.isEmpty(materialCodes)) {
            return Collections.emptyMap();
        }
        List<MaterialDefDO> list = materialDefMapper.selectListByMaterialCodes(materialCodes);
        Map<String, MaterialDefDO> map = new HashMap<>();
        for (MaterialDefDO item : list) {
            map.put(item.getMaterialCode(), item);
        }
        return map;
    }

}