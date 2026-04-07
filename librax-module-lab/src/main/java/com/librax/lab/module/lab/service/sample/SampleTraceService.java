package com.librax.lab.module.lab.service.sample;

import com.librax.lab.module.lab.service.sample.vo.SampleJourneyVO;
import com.librax.lab.module.lab.service.sample.vo.SampleTimelineVO;
import com.librax.lab.module.lab.service.sample.vo.SampleTraceReportVO;
import com.librax.lab.module.lab.service.sample.vo.SampleTraceVO;

import java.util.List;

public interface SampleTraceService {

    /**
     * 查询样本谱系树
     * 从 rootSampleId 出发，递归构建整棵树
     */
    SampleTraceVO getFullFamilyTree(String sampleId);

    /**
     * 查询样本经过的所有步骤（旅程）
     * 包含每个步骤的绑定信息、设备信息、检测结果
     */
    List<SampleJourneyVO> getSampleJourney(String sampleId);

    /**
     * 查询样本的完整事件时间线
     */
    List<SampleTimelineVO> getSampleTimeline(String sampleId);

    /**
     * 获取样本完整溯源报告（汇总以上所有信息）
     */
    SampleTraceReportVO getTraceReport(String sampleId);

}
