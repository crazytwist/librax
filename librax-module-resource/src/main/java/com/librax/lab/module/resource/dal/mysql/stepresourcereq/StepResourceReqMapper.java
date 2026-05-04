package com.librax.lab.module.resource.dal.mysql.stepresourcereq;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.resource.dal.dataobject.stepresourcereq.StepResourceReqDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.resource.controller.admin.stepresourcereq.vo.*;

/**
 * 步骤资源需求定义，一个步骤节点可配多行（一步多资源） Mapper
 *
 * @author 一南
 */
@Mapper
public interface StepResourceReqMapper extends BaseMapperX<StepResourceReqDO> {

    default PageResult<StepResourceReqDO> selectPage(StepResourceReqPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<StepResourceReqDO>()
                .orderByDesc(StepResourceReqDO::getId));
    }

}