package com.librax.lab.module.lab.service.materialcheckrule;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.lab.controller.admin.materialcheckrule.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialcheckrule.MaterialCheckRuleDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理 Service 接口
 *
 * @author 芋道源码
 */
public interface MaterialCheckRuleService {

    /**
     * 创建步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMaterialCheckRule(@Valid MaterialCheckRuleSaveReqVO createReqVO);

    /**
     * 更新步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理
     *
     * @param updateReqVO 更新信息
     */
    void updateMaterialCheckRule(@Valid MaterialCheckRuleSaveReqVO updateReqVO);

    /**
     * 删除步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理
     *
     * @param id 编号
     */
    void deleteMaterialCheckRule(Long id);

    /**
    * 批量删除步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理
    *
    * @param ids 编号
    */
    void deleteMaterialCheckRuleListByIds(List<Long> ids);

    /**
     * 获得步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理
     *
     * @param id 编号
     * @return 步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理
     */
    MaterialCheckRuleDO getMaterialCheckRule(Long id);

    /**
     * 获得步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理分页
     *
     * @param pageReqVO 分页查询
     * @return 步骤物料前置检查规则，物料不足则阻止步骤执行（需人工补充），区别于资源不足等待，归 lab 模块管理分页
     */
    PageResult<MaterialCheckRuleDO> getMaterialCheckRulePage(MaterialCheckRulePageReqVO pageReqVO);

}