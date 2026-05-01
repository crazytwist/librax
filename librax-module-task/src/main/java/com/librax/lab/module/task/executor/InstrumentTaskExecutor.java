package com.librax.lab.module.task.executor;

import com.librax.lab.module.task.dal.dataobject.task.TaskDO;
import com.librax.lab.module.task.dispatch.TaskCallbackDispatcher;
import com.librax.lab.module.task.enums.TaskTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InstrumentTaskExecutor implements TaskExecutor {

    private final TaskCallbackDispatcher callbackDispatcher;

    @Override
    public String supportType() {
        return TaskTypeEnum.INSTRUMENT.name();
    }

    @Override
    public void execute(TaskDO task) {
        // INSTRUMENT 类型步骤走 QUEUED 轨时，实际上是发指令给设备后等回调
        // 现阶段 Mock：直接模拟设备执行成功
        log.info("[InstrumentTaskExecutor] 执行仪器任务 taskId={} payload={}",
                task.getTaskId(), task.getPayload());

        // 现在 Mock 直接回调成功
        Map<String, Object> mockOutput = Map.of(
                "ph", 7.2,
                "temperature", 25.1,
                "measuredAt", LocalDateTime.now().toString()
        );
        callbackDispatcher.dispatch(
                task.getCallbackToken(), true, mockOutput, null, null);
    }

    @Override
    public void cancel(TaskDO task) {

    }
}
