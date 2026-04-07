package com.librax.lab.module.lab.service.samplerelation;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.lab.controller.admin.samplerelation.vo.*;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleRelationDO;
import com.librax.lab.framework.common.pojo.PageResult;

/**
 * 样本谱系关系表，记录拆分/合并/分装等衍生关系 Service 接口
 *
 * @author 一南
 */
public interface SampleRelationService {

    /**
     * 创建样本谱系关系表，记录拆分/合并/分装等衍生关系
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSampleRelation(@Valid SampleRelationSaveReqVO createReqVO);

    /**
     * 更新样本谱系关系表，记录拆分/合并/分装等衍生关系
     *
     * @param updateReqVO 更新信息
     */
    void updateSampleRelation(@Valid SampleRelationSaveReqVO updateReqVO);

    /**
     * 删除样本谱系关系表，记录拆分/合并/分装等衍生关系
     *
     * @param id 编号
     */
    void deleteSampleRelation(Long id);

    /**
    * 批量删除样本谱系关系表，记录拆分/合并/分装等衍生关系
    *
    * @param ids 编号
    */
    void deleteSampleRelationListByIds(List<Long> ids);

    /**
     * 获得样本谱系关系表，记录拆分/合并/分装等衍生关系
     *
     * @param id 编号
     * @return 样本谱系关系表，记录拆分/合并/分装等衍生关系
     */
    SampleRelationDO getSampleRelation(Long id);

    /**
     * 获得样本谱系关系表，记录拆分/合并/分装等衍生关系分页
     *
     * @param pageReqVO 分页查询
     * @return 样本谱系关系表，记录拆分/合并/分装等衍生关系分页
     */
    PageResult<SampleRelationDO> getSampleRelationPage(SampleRelationPageReqVO pageReqVO);

}