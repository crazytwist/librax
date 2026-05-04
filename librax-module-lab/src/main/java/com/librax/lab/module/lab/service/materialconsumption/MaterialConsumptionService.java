package com.librax.lab.module.lab.service.materialconsumption;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.lab.controller.admin.materialconsumption.vo.*;
import com.librax.lab.module.lab.dal.dataobject.materialconsumption.MaterialConsumptionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理 Service 接口
 *
 * @author 芋道源码
 */
public interface MaterialConsumptionService {

    /**
     * 创建步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMaterialConsumption(@Valid MaterialConsumptionSaveReqVO createReqVO);

    /**
     * 更新步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理
     *
     * @param updateReqVO 更新信息
     */
    void updateMaterialConsumption(@Valid MaterialConsumptionSaveReqVO updateReqVO);

    /**
     * 删除步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理
     *
     * @param id 编号
     */
    void deleteMaterialConsumption(Long id);

    /**
    * 批量删除步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理
    *
    * @param ids 编号
    */
    void deleteMaterialConsumptionListByIds(List<Long> ids);

    /**
     * 获得步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理
     *
     * @param id 编号
     * @return 步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理
     */
    MaterialConsumptionDO getMaterialConsumption(Long id);

    /**
     * 获得步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理分页
     *
     * @param pageReqVO 分页查询
     * @return 步骤物料消耗/操作记录，用于溯源（样本←步骤←试剂批次），归 lab 模块管理分页
     */
    PageResult<MaterialConsumptionDO> getMaterialConsumptionPage(MaterialConsumptionPageReqVO pageReqVO);

}