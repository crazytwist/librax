package com.librax.lab.module.flow.service.executioncontext;

import cn.hutool.core.collection.CollUtil;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import com.librax.lab.module.flow.controller.admin.executioncontext.vo.*;
import com.librax.lab.module.flow.dal.dataobject.executioncontext.ExecutionContextDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;
import com.librax.lab.framework.common.util.object.BeanUtils;

import com.librax.lab.module.flow.dal.mysql.executioncontext.ExecutionContextMapper;

import static com.librax.lab.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.convertList;
import static com.librax.lab.framework.common.util.collection.CollectionUtils.diffList;
import static com.librax.lab.module.flow.enums.ErrorCodeConstants.*;

/**
 * 执行上下文持久化，Redis 冷备份，断点恢复与单独运行数据注入用 Service 实现类
 *
 * @author 一南
 */
@Service
@Validated
public class ExecutionContextServiceImpl implements ExecutionContextService {

    @Resource
    private ExecutionContextMapper executionContextMapper;

    @Override
    public Long createExecutionContext(ExecutionContextSaveReqVO createReqVO) {
        // 插入
        ExecutionContextDO executionContext = BeanUtils.toBean(createReqVO, ExecutionContextDO.class);
        executionContextMapper.insert(executionContext);

        // 返回
        return executionContext.getId();
    }

    @Override
    public void updateExecutionContext(ExecutionContextSaveReqVO updateReqVO) {
        // 校验存在
        validateExecutionContextExists(updateReqVO.getId());
        // 更新
        ExecutionContextDO updateObj = BeanUtils.toBean(updateReqVO, ExecutionContextDO.class);
        executionContextMapper.updateById(updateObj);
    }

    @Override
    public void deleteExecutionContext(Long id) {
        // 校验存在
        validateExecutionContextExists(id);
        // 删除
        executionContextMapper.deleteById(id);
    }

    @Override
        public void deleteExecutionContextListByIds(List<Long> ids) {
        // 删除
        executionContextMapper.deleteByIds(ids);
        }


    private void validateExecutionContextExists(Long id) {
        if (executionContextMapper.selectById(id) == null) {
            throw exception(EXECUTION_CONTEXT_NOT_EXISTS);
        }
    }

    @Override
    public ExecutionContextDO getExecutionContext(Long id) {
        return executionContextMapper.selectById(id);
    }

    @Override
    public PageResult<ExecutionContextDO> getExecutionContextPage(ExecutionContextPageReqVO pageReqVO) {
        return executionContextMapper.selectPage(pageReqVO);
    }

}