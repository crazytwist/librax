package com.librax.lab.module.flow.dal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteRule {

    private String fromNodeId;

    /** 条件表达式，如 "status == OK" */
    private String condition;

    private String toNodeId;
}
