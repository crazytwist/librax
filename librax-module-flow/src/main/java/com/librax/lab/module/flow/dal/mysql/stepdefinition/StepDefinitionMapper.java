package com.librax.lab.module.flow.dal.mysql.stepdefinition;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.stepdefinition.StepDefinitionDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.stepdefinition.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 步骤定义表，可复用的步骤组件库 [pd_] Mapper
 *
 * @author 一南
 */
@Mapper
public interface StepDefinitionMapper extends BaseMapperX<StepDefinitionDO> {

    /**
     * 批量查各 stepKey 的最新 ACTIVE 版本
     * 每个 stepKey 只返回 version 最大的一条
     */
    List<StepDefinitionDO> selectLatestActiveByKeys(@Param("stepKeys") List<String> stepKeys);

    /**
     * 按 step_key + version 查询指定版本
     * 用于 pipeline_step.step_version != NULL 的节点
     */
    StepDefinitionDO selectByKeyAndVersion(@Param("stepKey") String stepKey,
                                           @Param("version") Integer version);

    default PageResult<StepDefinitionDO> selectPage(StepDefinitionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StepDefinitionDO>()
                .eqIfPresent(StepDefinitionDO::getStepKey, reqVO.getStepKey())
                .likeIfPresent(StepDefinitionDO::getName, reqVO.getName())
                .eqIfPresent(StepDefinitionDO::getStepType, reqVO.getStepType())
                .eqIfPresent(StepDefinitionDO::getDeviceType, reqVO.getDeviceType())
                .eqIfPresent(StepDefinitionDO::getCommand, reqVO.getCommand())
                .eqIfPresent(StepDefinitionDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(StepDefinitionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(StepDefinitionDO::getId));
    }

}