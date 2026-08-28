package com.librax.lab.module.lab.dal.mysql.materialdef;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.materialdef.MaterialDefDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.lab.controller.admin.materialdef.vo.*;

/**
 * 内容物定义，描述试剂/标准品/缓冲液等内容物的属性，归 lab 模块管理 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MaterialDefMapper extends BaseMapperX<MaterialDefDO> {

    /** 按内容物编码批量查询，用于给物料实例列表回填 materialName/materialUnit（冗余展示字段，不落库）。 */
    default List<MaterialDefDO> selectListByMaterialCodes(Collection<String> materialCodes) {
        if (materialCodes == null || materialCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MaterialDefDO>()
                .in(MaterialDefDO::getMaterialCode, materialCodes));
    }

    default PageResult<MaterialDefDO> selectPage(MaterialDefPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MaterialDefDO>()
                .likeIfPresent(MaterialDefDO::getMaterialCode, reqVO.getMaterialCode())
                .likeIfPresent(MaterialDefDO::getMaterialName, reqVO.getMaterialName())
                .eqIfPresent(MaterialDefDO::getContentType, reqVO.getContentType())
                .eqIfPresent(MaterialDefDO::getUnit, reqVO.getUnit())
                .eqIfPresent(MaterialDefDO::getSupplier, reqVO.getSupplier())
                .eqIfPresent(MaterialDefDO::getCatalogNo, reqVO.getCatalogNo())
                .eqIfPresent(MaterialDefDO::getCasNo, reqVO.getCasNo())
                .eqIfPresent(MaterialDefDO::getConcentration, reqVO.getConcentration())
                .eqIfPresent(MaterialDefDO::getStorageTemp, reqVO.getStorageTemp())
                .eqIfPresent(MaterialDefDO::getShelfLifeDays, reqVO.getShelfLifeDays())
                .eqIfPresent(MaterialDefDO::getOpenLifeDays, reqVO.getOpenLifeDays())
                .eqIfPresent(MaterialDefDO::getHazardLevel, reqVO.getHazardLevel())
                .eqIfPresent(MaterialDefDO::getSpecJson, reqVO.getSpecJson())
                .eqIfPresent(MaterialDefDO::getEnabled, reqVO.getEnabled())
                .likeIfPresent(MaterialDefDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(MaterialDefDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MaterialDefDO::getId));
    }

}