package com.librax.lab.module.bpm.dal.mysql.category;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.bpm.controller.admin.definition.vo.category.BpmCategoryPageReqVO;
import com.librax.lab.module.bpm.dal.dataobject.definition.BpmCategoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * BPM 流程分类 Mapper
 *
 * @author 一南
 */
@Mapper
public interface BpmCategoryMapper extends BaseMapperX<BpmCategoryDO> {

    default PageResult<BpmCategoryDO> selectPage(BpmCategoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<BpmCategoryDO>()
                .likeIfPresent(BpmCategoryDO::getName, reqVO.getName())
                .likeIfPresent(BpmCategoryDO::getCode, reqVO.getCode())
                .eqIfPresent(BpmCategoryDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(BpmCategoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByAsc(BpmCategoryDO::getSort));
    }

    default BpmCategoryDO selectByName(String name) {
        return selectOne(BpmCategoryDO::getName, name);
    }

    default BpmCategoryDO selectByCode(String code) {
        return selectOne(BpmCategoryDO::getCode, code);
    }

    default List<BpmCategoryDO> selectListByCode(Collection<String> codes) {
        return selectList(BpmCategoryDO::getCode, codes);
    }

    default List<BpmCategoryDO> selectListByStatus(Integer status) {
        return selectList(BpmCategoryDO::getStatus, status);
    }

}