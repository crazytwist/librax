package com.librax.lab.module.lab.service.sample.vo;

import lombok.Data;

import java.util.List;

/**
 * 样本完整溯源报告（汇总）
 */
@Data
public class SampleTraceReportVO {
    private SampleInfoSummaryVO sampleInfo;         // 样本基本信息
    private SampleTraceVO familyTree;                // 谱系树
    private List<SampleJourneyVO> journey;           // 步骤旅程
    private List<SampleJourneyResultVO> results;     // 所有检测结果
    private List<SampleTimelineVO> timeline;         // 事件时间线
}