package com.jd.genie.agent.enums;

/**
 * 智能体类型 目前只支持了两种模式：综合模式（未支持）、工作流模式（未支持）、规划解决模式、路由器模式（未支持）、React模式
 */
public enum AgentType {
    COMPREHENSIVE(1),
    WORKFLOW(2),
    PLAN_SOLVE(3),
    ROUTER(4),
    REACT(5);

    private final Integer value;

    AgentType(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }

    public static AgentType fromCode(int value) {
        for (AgentType type : AgentType.values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid AgentType code: " + value);
    }
}