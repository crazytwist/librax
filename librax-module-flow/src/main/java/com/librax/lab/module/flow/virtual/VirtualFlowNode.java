package com.librax.lab.module.flow.virtual;

import com.librax.lab.module.flow.dal.FlowContext;
import com.librax.lab.module.flow.dal.NodeConfig;
import com.librax.lab.module.flow.service.NodeService;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@LiteflowComponent("virtualNode")
@RequiredArgsConstructor
public class VirtualFlowNode extends NodeComponent {

    private final NodeService nodeService;

    @Override
    public void process() throws Exception {

        // 直接从 LiteFlow Request 获取上下文
        FlowContext ctx = (FlowContext) this.getRequestData();

        if (ctx == null) {
            throw new IllegalStateException("FlowContext cannot be null!");
        }

        String currentNodeId = ctx.getCurrentNodeId();
        if (currentNodeId == null) {
            throw new IllegalStateException("CurrentNodeId cannot be null in FlowContext!");
        }

        // 获取下一步动态节点列表
        List<NodeConfig> nextNodes = nodeService.loadNextNodes(currentNodeId, ctx.getVariables());

        if (nextNodes.isEmpty()) {
            // 没有下一个节点，可以标记流程结束或者直接返回
            ctx.setStatus("COMPLETED");
            return;
        }

        // 放入 nodeQueue
        ctx.getNodeQueue().addAll(nextNodes);

        // 更新下一个节点ID为第一个节点
        ctx.setNextNodeId(nextNodes.get(0).getNodeId());

        // 日志打印
        System.out.println("[VirtualFlowNode] Loaded next nodes: " +
                nextNodes.stream().map(NodeConfig::getNodeId).toList());

    }
}
