package com.librax.lab.module.lab.dal.mysql.sample;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.lab.dal.dataobject.sample.SampleRelationDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.lab.controller.admin.samplerelation.vo.*;

import java.util.List;

/**
 * 样本谱系关系表，记录拆分/合并/分装等衍生关系 Mapper
 *
 * @author 一南
 */
@Mapper
public interface SampleRelationMapper extends BaseMapperX<SampleRelationDO> {

    default PageResult<SampleRelationDO> selectPage(SampleRelationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SampleRelationDO>()
                .eqIfPresent(SampleRelationDO::getSampleId, reqVO.getSampleId())
                .eqIfPresent(SampleRelationDO::getRelatedSampleId, reqVO.getRelatedSampleId())
                .eqIfPresent(SampleRelationDO::getRelationType, reqVO.getRelationType())
                .betweenIfPresent(SampleRelationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(SampleRelationDO::getId));
    }

    /** 查当前样本的所有来源（我从哪些样本衍生来的） */
    default List<SampleRelationDO> selectParentRelations(String sampleId) {
        return selectList(new LambdaQueryWrapperX<SampleRelationDO>()
                .eq(SampleRelationDO::getSampleId, sampleId));
    }

    /** 查当前样本衍生出的所有子样本 */
    default List<SampleRelationDO> selectChildRelations(String parentSampleId) {
        return selectList(new LambdaQueryWrapperX<SampleRelationDO>()
                .eq(SampleRelationDO::getRelatedSampleId, parentSampleId));
    }

}