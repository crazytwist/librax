package com.librax.lab.module.flow.service;

import com.librax.lab.module.flow.dal.NodeConfig;

import java.util.List;
import java.util.Map;

public interface NodeService {

    List<NodeConfig> loadNextNodes(String currentNodeId, Map<String, Object> variables);

}
