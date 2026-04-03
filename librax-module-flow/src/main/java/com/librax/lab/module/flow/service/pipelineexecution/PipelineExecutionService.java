package com.librax.lab.module.flow.service.pipelineexecution;

import java.util.*;
import jakarta.validation.*;
import com.librax.lab.module.flow.controller.admin.pipelineexecution.vo.*;
import com.librax.lab.module.flow.dal.dataobject.pipelineexecution.PipelineExecutionDO;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.framework.common.pojo.PageParam;

/**
 * 流程执行服务
 *
 * <p>对外暴露流程执行的所有操作入口：启动、暂停、恢复、取消、单节点运行。
 * 内部通过 {@link com.librax.lab.module.flow.engine.execution.scheduler.DagScheduler}
 * 驱动 DAG 调度引擎执行。
 */
public interface PipelineExecutionService {

    /**
     * 创建流程执行实例，支持完整流程、节点单独运行、补偿执行
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPipelineExecution(@Valid PipelineExecutionSaveReqVO createReqVO);

    /**
     * 更新流程执行实例，支持完整流程、节点单独运行、补偿执行
     *
     * @param updateReqVO 更新信息
     */
    void updatePipelineExecution(@Valid PipelineExecutionSaveReqVO updateReqVO);

    /**
     * 删除流程执行实例，支持完整流程、节点单独运行、补偿执行
     *
     * @param id 编号
     */
    void deletePipelineExecution(Long id);

    /**
    * 批量删除流程执行实例，支持完整流程、节点单独运行、补偿执行
    *
    * @param ids 编号
    */
    void deletePipelineExecutionListByIds(List<Long> ids);

    /**
     * 获得流程执行实例，支持完整流程、节点单独运行、补偿执行
     *
     * @param id 编号
     * @return 流程执行实例，支持完整流程、节点单独运行、补偿执行
     */
    PipelineExecutionDO getPipelineExecution(Long id);

    /**
     * 获得流程执行实例，支持完整流程、节点单独运行、补偿执行分页
     *
     * @param pageReqVO 分页查询
     * @return 流程执行实例，支持完整流程、节点单独运行、补偿执行分页
     */
    PageResult<PipelineExecutionDO> getPipelineExecutionPage(PipelineExecutionPageReqVO pageReqVO);



    // -----------------------------------------------------------------------------------------------------------------

    /**
     * 启动一条流程
     *
     * @param pipelineKey     流程唯一标识，如 water_quality_test
     * @param pipelineVersion 流程版本号，传 null 时自动使用最新 ACTIVE 版本
     * @param inputParams     外部传入的初始参数，节点可通过 ${input.xxx} 引用，如 sampleId
     * @param triggerType     触发类型：MANUAL/SCHEDULE/EVENT
     * @param triggeredBy     触发人ID或触发源标识
     * @return 执行实例唯一ID（UUID），可用于后续查询进度
     */
    String start(String pipelineKey,
                 Integer pipelineVersion,
                 Map<String, Object> inputParams,
                 String triggerType,
                 String triggeredBy);

    /**
     * 暂停正在执行的流程
     * <p>已提交到线程池的节点会执行完当前步骤，不会强制中断
     *
     * @param executionId 执行实例ID
     */
    void pause(String executionId);

    /**
     * 恢复已暂停的流程
     * <p>从当前状态继续调度，已完成的节点不会重复执行
     *
     * @param executionId 执行实例ID
     */
    void resume(String executionId);

    /**
     * 取消流程执行
     * <p>流程进入 CANCELLED 终态，不可再恢复
     *
     * @param executionId 执行实例ID
     */
    void cancel(String executionId);

}