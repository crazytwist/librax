package com.librax.lab.module.lab.dal.mysql.materialconsumption;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.materialconsumption.MaterialConsumptionDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.lab.controller.admin.materialconsumption.vo.*;

/**
 * 步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MaterialConsumptionMapper extends BaseMapperX<MaterialConsumptionDO> {

    default PageResult<MaterialConsumptionDO> selectPage(MaterialConsumptionPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MaterialConsumptionDO>()
                .eqIfPresent(MaterialConsumptionDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(MaterialConsumptionDO::getNodeId, reqVO.getNodeId())
                .eqIfPresent(MaterialConsumptionDO::getAttempt, reqVO.getAttempt())
                .eqIfPresent(MaterialConsumptionDO::getInstanceId, reqVO.getInstanceId())
                .eqIfPresent(MaterialConsumptionDO::getTypeCode, reqVO.getTypeCode())
                .eqIfPresent(MaterialConsumptionDO::getMaterialCode, reqVO.getMaterialCode())
                .eqIfPresent(MaterialConsumptionDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(MaterialConsumptionDO::getAction, reqVO.getAction())
                .eqIfPresent(MaterialConsumptionDO::getVolBeforeUl, reqVO.getVolBeforeUl())
                .eqIfPresent(MaterialConsumptionDO::getVolChangeUl, reqVO.getVolChangeUl())
                .eqIfPresent(MaterialConsumptionDO::getVolAfterUl, reqVO.getVolAfterUl())
                .eqIfPresent(MaterialConsumptionDO::getFromSlotId, reqVO.getFromSlotId())
                .eqIfPresent(MaterialConsumptionDO::getToSlotId, reqVO.getToSlotId())
                .eqIfPresent(MaterialConsumptionDO::getConsumedAt, reqVO.getConsumedAt())
                .eqIfPresent(MaterialConsumptionDO::getRemark, reqVO.getRemark())
                .betweenIfPresent(MaterialConsumptionDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(MaterialConsumptionDO::getId));
    }

}