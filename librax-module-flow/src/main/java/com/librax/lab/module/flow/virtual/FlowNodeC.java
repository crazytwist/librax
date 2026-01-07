package com.librax.lab.module.flow.virtual;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LiteflowComponent("NODE_C")
public class FlowNodeC extends NodeComponent {

    @Override
    public void process() throws Exception {

        String nodeCode = getNodeId();

        log.info(nodeCode);

    }
}
