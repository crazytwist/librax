package com.librax.lab.module.lab.dal.mysql.materialcheckrule;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.materialcheckrule.MaterialCheckRuleDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.lab.controller.admin.materialcheckrule.vo.*;

/**
 * 步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MaterialCheckRuleMapper extends BaseMapperX<MaterialCheckRuleDO> {

    default PageResult<MaterialCheckRuleDO> selectPage(MaterialCheckRulePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MaterialCheckRuleDO>()
                .eqIfPresent(MaterialCheckRuleDO::getPipelineStepId, reqVO.getPipelineStepId())
                .eqIfPresent(MaterialCheckRuleDO::getMaterialCode, reqVO.getMaterialCode())
                .eqIfPresent(MaterialCheckRuleDO::getContentType, reqVO.getContentType())
                .eqIfPresent(MaterialCheckRuleDO::getMinVolUl, reqVO.getMinVolUl())
                .eqIfPresent(MaterialCheckRuleDO::getMinCount, reqVO.getMinCount())
                .eqIfPresent(MaterialCheckRuleDO::getZoneCode, reqVO.getZoneCode())
                .eqIfPresent(MaterialCheckRuleDO::getSortOrder, reqVO.getSortOrder())
                .eqIfPresent(MaterialCheckRuleDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(MaterialCheckRuleDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MaterialCheckRuleDO::getId));
    }

}