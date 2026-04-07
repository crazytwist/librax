package com.librax.lab.module.lab.service.sampleevent;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.lab.controller.admin.sampleevent.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleEventDO;
import com.librax.lab.framework.common.pojo.PageResult;

/**
 * 样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_] Service 接口
 *
 * @author 一南
 */
public interface SampleEventService {

    /**
     * 创建样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSampleEvent(@Valid SampleEventSaveReqVO createReqVO);

    /**
     * 更新样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]
     *
     * @param updateReqVO 更新信息
     */
    void updateSampleEvent(@Valid SampleEventSaveReqVO updateReqVO);

    /**
     * 删除样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]
     *
     * @param id 编号
     */
    void deleteSampleEvent(Long id);

    /**
    * 批量删除样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]
    *
    * @param ids 编号
    */
    void deleteSampleEventListByIds(List<Long> ids);

    /**
     * 获得样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]
     *
     * @param id 编号
     * @return 样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]
     */
    SampleEventDO getSampleEvent(Long id);

    /**
     * 获得样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]分页
     *
     * @param pageReqVO 分页查询
     * @return 样本事件日志，全链路追踪，只INSERT不修改 [lab_sample_]分页
     */
    PageResult<SampleEventDO> getSampleEventPage(SampleEventPageReqVO pageReqVO);

}