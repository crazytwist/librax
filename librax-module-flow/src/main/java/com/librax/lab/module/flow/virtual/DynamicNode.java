package com.librax.lab.module.flow.virtual;

import com.librax.lab.module.flow.dal.FlowContext;
import com.librax.lab.module.flow.dal.NodeConfig;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LiteflowComponent("dynamicNode")
public class DynamicNode extends NodeComponent {
    @Override
    public void process() throws Exception {
        // 直接从 LiteFlow Request 获取上下文
        FlowContext ctx = (FlowContext) this.getRequestData();

        if (ctx == null) {
            throw new IllegalStateException("FlowContext cannot be null!");
        }

        NodeConfig nodeConfig = ctx.getNodeQueue().poll();
        if (nodeConfig == null) {
            // 队列为空，没有任务
            System.out.println("[DynamicNode] No task in nodeQueue");
            return;
        }

        // 更新当前节点
        ctx.setCurrentNodeId(nodeConfig.getNodeId());

        // 根据类型执行任务
        switch (nodeConfig.getType()) {
            case AUTO -> {
                System.out.println("[DynamicNode] AUTO task executing: " + nodeConfig.getParams());
                ctx.setStatus("RUNNING");
            }
            case MANUAL -> {
                System.out.println("[DynamicNode] MANUAL task waiting: " + nodeConfig.getParams());
                ctx.setStatus("WAITING");
                // 可抛出中断异常，LiteFlow 会暂停
                throw new UnsupportedOperationException();
            }
            case DISPATCH -> {
                System.out.println("[DynamicNode] DISPATCH task executing: " + nodeConfig.getParams());
                ctx.setStatus("RUNNING");
            }
            case END -> {
                System.out.println("[DynamicNode] END node reached");
                ctx.setStatus("COMPLETED");
            }
            default -> throw new UnsupportedOperationException("Unknown node type: " + nodeConfig.getType());
        }
    }
}
