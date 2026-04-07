package com.librax.lab.module.lab.service.samplestep;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.lab.controller.admin.samplestep.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleStepDO;
import com.librax.lab.framework.common.pojo.PageResult;

/**
 * 样本-步骤绑定表，记录样本在每个流程步骤中的处理状态 Service 接口
 *
 * @author 一南
 */
public interface SampleStepService {

    /**
     * 创建样本-步骤绑定表，记录样本在每个流程步骤中的处理状态
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSampleStep(@Valid SampleStepSaveReqVO createReqVO);

    /**
     * 更新样本-步骤绑定表，记录样本在每个流程步骤中的处理状态
     *
     * @param updateReqVO 更新信息
     */
    void updateSampleStep(@Valid SampleStepSaveReqVO updateReqVO);

    /**
     * 删除样本-步骤绑定表，记录样本在每个流程步骤中的处理状态
     *
     * @param id 编号
     */
    void deleteSampleStep(Long id);

    /**
    * 批量删除样本-步骤绑定表，记录样本在每个流程步骤中的处理状态
    *
    * @param ids 编号
    */
    void deleteSampleStepListByIds(List<Long> ids);

    /**
     * 获得样本-步骤绑定表，记录样本在每个流程步骤中的处理状态
     *
     * @param id 编号
     * @return 样本-步骤绑定表，记录样本在每个流程步骤中的处理状态
     */
    SampleStepDO getSampleStep(Long id);

    /**
     * 获得样本-步骤绑定表，记录样本在每个流程步骤中的处理状态分页
     *
     * @param pageReqVO 分页查询
     * @return 样本-步骤绑定表，记录样本在每个流程步骤中的处理状态分页
     */
    PageResult<SampleStepDO> getSampleStepPage(SampleStepPageReqVO pageReqVO);

}