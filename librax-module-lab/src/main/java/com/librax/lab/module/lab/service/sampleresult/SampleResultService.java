package com.librax.lab.module.lab.service.sampleresult;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.lab.controller.admin.sampleresult.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleResultDO;
import com.librax.lab.framework.common.pojo.PageResult;

/**
 * 样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_] Service 接口
 *
 * @author 一南
 */
public interface SampleResultService {

    /**
     * 创建样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSampleResult(@Valid SampleResultSaveReqVO createReqVO);

    /**
     * 更新样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]
     *
     * @param updateReqVO 更新信息
     */
    void updateSampleResult(@Valid SampleResultSaveReqVO updateReqVO);

    /**
     * 删除样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]
     *
     * @param id 编号
     */
    void deleteSampleResult(Long id);

    /**
    * 批量删除样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]
    *
    * @param ids 编号
    */
    void deleteSampleResultListByIds(List<Long> ids);

    /**
     * 获得样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]
     *
     * @param id 编号
     * @return 样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]
     */
    SampleResultDO getSampleResult(Long id);

    /**
     * 获得样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]分页
     *
     * @param pageReqVO 分页查询
     * @return 样本检测结果表，每个检测项一行，支持查询统计和结果审核 [lab_sample_]分页
     */
    PageResult<SampleResultDO> getSampleResultPage(SampleResultPageReqVO pageReqVO);

}