package com.librax.lab.module.lab.service.materialcheckrule;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.lab.controller.admin.materialcheckrule.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialcheckrule.MaterialCheckRuleDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.lab.dal.mysql.materialcheckrule.MaterialCheckRuleMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.lab.enums.ErrorCodeConstants.*;

/**
 * 步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MaterialCheckRuleServiceImpl implements MaterialCheckRuleService {

    @Resource
    private MaterialCheckRuleMapper materialCheckRuleMapper;

    @Override
    public Long createMaterialCheckRule(MaterialCheckRuleSaveReqVO createReqVO) {
        // 插入
        MaterialCheckRuleDO materialCheckRule = BeanUtils.toBean(createReqVO, MaterialCheckRuleDO.class);
        materialCheckRuleMapper.insert(materialCheckRule);

        // 返回
        return materialCheckRule.getId();
    }

    @Override
    public void updateMaterialCheckRule(MaterialCheckRuleSaveReqVO updateReqVO) {
        // 校验存在
        validateMaterialCheckRuleExists(updateReqVO.getId());
        // 更新
        MaterialCheckRuleDO updateObj = BeanUtils.toBean(updateReqVO, MaterialCheckRuleDO.class);
        materialCheckRuleMapper.updateById(updateObj);
    }

    @Override
    public void deleteMaterialCheckRule(Long id) {
        // 校验存在
        validateMaterialCheckRuleExists(id);
        // 删除
        materialCheckRuleMapper.deleteById(id);
    }

    @Override
        public void deleteMaterialCheckRuleListByIds(List<Long> ids) {
        // 删除
        materialCheckRuleMapper.deleteByIds(ids);
        }


    private void validateMaterialCheckRuleExists(Long id) {
        if (materialCheckRuleMapper.selectById(id) == null) {
            throw exception(MATERIAL_CHECK_RULE_NOT_EXISTS);
        }
    }

    @Override
    public MaterialCheckRuleDO getMaterialCheckRule(Long id) {
        return materialCheckRuleMapper.selectById(id);
    }

    @Override
    public PageResult<MaterialCheckRuleDO> getMaterialCheckRulePage(MaterialCheckRulePageReqVO pageReqVO) {
        return materialCheckRuleMapper.selectPage(pageReqVO);
    }

}