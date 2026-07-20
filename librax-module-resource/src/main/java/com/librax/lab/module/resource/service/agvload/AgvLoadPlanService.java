package com.librax.lab.module.resource.service.agvload;

import com.librax.lab.module.resource.controller.app.slotinfo.vo.WarehouseCallbackReqVO;

import java.util.List;
import java.util.Map;

/** AGV 一车任务内多轮装载的状态协调服务。 */
public interface AgvLoadPlanService {

    void registerIfEnabled(String taskId,
                           List<Map<String, Object>> transferItems,
                           Map<String, Object> inputParams);

    void startWarehouseOrchestration(String taskId,
                                     String nodeId,
                                     List<Map<String, Object>> transferItems,
                                     Map<String, Object> inputParams);

    Map<String, Object> markSlotReady(WarehouseCallbackReqVO req);

    Map<String, Object> getProgress(String taskId);

    boolean hasPlan(String taskId);

    void commitTransfer(String taskId);
}
