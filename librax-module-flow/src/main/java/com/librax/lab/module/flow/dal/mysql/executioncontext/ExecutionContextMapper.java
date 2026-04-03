package com.librax.lab.module.flow.dal.mysql.executioncontext;

import java.util.*;

import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.module.flow.dal.dataobject.executioncontext.ExecutionContextDO;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import org.apache.ibatis.annotations.Mapper;
import com.librax.lab.module.flow.controller.admin.executioncontext.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用 Mapper
 *
 * @author 一南
 */
@Mapper
public interface ExecutionContextMapper extends BaseMapperX<ExecutionContextDO> {

    default PageResult<ExecutionContextDO> selectPage(ExecutionContextPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ExecutionContextDO>()
                .eqIfPresent(ExecutionContextDO::getExecutionId, reqVO.getExecutionId())
                .eqIfPresent(ExecutionContextDO::getContextData, reqVO.getContextData())
                .betweenIfPresent(ExecutionContextDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ExecutionContextDO::getId));
    }

    /**
     * 根据执行ID获得执行对象
     *
     * @param executionId
     * @return
     */
    ExecutionContextDO selectByExecutionId(
            @Param("executionId") String executionId);


    void appendNodeOutput(@Param("executionId") String executionId,
                          @Param("nodeId")      String nodeId,
                          @Param("outputJson")  String outputJson);
}