package com.librax.lab.module.lab.service.sampleinfo;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.lab.controller.admin.sampleinfo.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleInfoDO;
import com.librax.lab.framework.common.pojo.PageResult;

/**
 * 样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联 Service 接口
 *
 * @author 一南
 */
public interface SampleInfoService {

    /**
     * 创建样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSampleInfo(@Valid SampleInfoSaveReqVO createReqVO);

    /**
     * 更新样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联
     *
     * @param updateReqVO 更新信息
     */
    void updateSampleInfo(@Valid SampleInfoSaveReqVO updateReqVO);

    /**
     * 删除样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联
     *
     * @param id 编号
     */
    void deleteSampleInfo(Long id);

    /**
    * 批量删除样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联
    *
    * @param ids 编号
    */
    void deleteSampleInfoListByIds(List<Long> ids);

    /**
     * 获得样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联
     *
     * @param id 编号
     * @return 样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联
     */
    SampleInfoDO getSampleInfo(Long id);

    /**
     * 获得样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联分页
     *
     * @param pageReqVO 分页查询
     * @return 样本主表，记录样本全生命周期信息：属性、状态、位置、流程关联分页
     */
    PageResult<SampleInfoDO> getSampleInfoPage(SampleInfoPageReqVO pageReqVO);

}