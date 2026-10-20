#!/usr/bin/env python3
"""
离线时间线模拟：复现生产 UNLOAD waitSignal 400，并验证新逻辑下的结果。
不依赖 Maven；与 AgvOrchestrationSubtaskIds + resolveLoadPlanForOrchestrationWaitSignal 行为对齐。
"""

PLAN = "e90a45fada1b4c7e8d7f16fa0594c80f"
UNLOAD = f"{PLAN}-UNLOAD"
ATTEMPTS = 5
DELAY_MS = 100


def strip_to_plan_task_id(agv_task_id: str) -> str | None:
    if not agv_task_id:
        return None
    for marker in ("-UNLOAD-",):
        i = agv_task_id.find(marker)
        if i > 0:
            return agv_task_id[:i]
    if agv_task_id.endswith("-UNLOAD"):
        return agv_task_id[: -len("-UNLOAD")]
    for marker in ("-MOVE", "-PREPOSITION", "-LOAD-"):
        i = agv_task_id.find(marker)
        if i > 0:
            return agv_task_id[:i]
    if agv_task_id.endswith("-LOAD"):
        return agv_task_id[: -len("-LOAD")]
    return None


def gate_ready(plan: dict | None, agv_task_id: str) -> bool:
    return (
        plan is not None
        and plan.get("orchestration_node_id")
        and (plan.get("status") or "").startswith("WAIT_AGV_")
        and plan.get("current_agv_task_id") == agv_task_id
    )


def old_handle(db: dict, agv_task_id: str) -> str:
    probe = db["by_current"].get(agv_task_id)
    if probe and gate_ready(probe, agv_task_id):
        return "COORDINATOR_OK"
    return "FLOW_CALLBACK"


def new_handle(db: dict, agv_task_id: str) -> str:
    hint = strip_to_plan_task_id(agv_task_id)
    if hint is None:
        probe = db["by_current"].get(agv_task_id)
        return "COORDINATOR_OK" if gate_ready(probe, agv_task_id) else "FLOW_OR_LOADING"
    for attempt in range(1, ATTEMPTS + 1):
        by_current = db["by_current"].get(agv_task_id)
        if gate_ready(by_current, agv_task_id):
            return "COORDINATOR_OK"
        by_plan = db["plans"].get(hint)
        if gate_ready(by_plan, agv_task_id):
            return "COORDINATOR_OK"
        if attempt < ATTEMPTS:
            db["on_retry"](attempt)
    if strip_to_plan_task_id(agv_task_id):
        return "HTTP_503_RETRY"
    return "FLOW_CALLBACK"


def controller_old(handler_ok: bool, agv_task_id: str) -> str:
    if handler_ok:
        return "HTTP_200"
    # 旧逻辑：直接用 taskId 作 executionId 调 flow
    if strip_to_plan_task_id(agv_task_id):
        return "HTTP_400_EXECUTION_NOT_FOUND"
    return "HTTP_400_EXECUTION_NOT_FOUND"


def controller_new(handler_ok: bool, agv_task_id: str) -> str:
    if handler_ok:
        return "HTTP_200"
    if strip_to_plan_task_id(agv_task_id):
        return "HTTP_503"
    return "HTTP_400_EXECUTION_NOT_FOUND"


def main() -> None:
    plan_ready = {
        "task_id": PLAN,
        "orchestration_node_id": "s_warehouse_orch",
        "status": "WAIT_AGV_UNLOAD",
        "current_agv_task_id": UNLOAD,
    }

    # 场景 1：生产日志 — 索引 miss，plan 行已就绪（planTaskId 回查）
    db1 = {
        "by_current": {},
        "plans": {PLAN: plan_ready},
        "on_retry": lambda _: None,
    }
    r_old = old_handle(db1, UNLOAD)
    r_new = new_handle(db1, UNLOAD)
    http_old = controller_old(r_old == "COORDINATOR_OK", UNLOAD)
    http_new = controller_new(r_new == "COORDINATOR_OK", UNLOAD)
    assert r_old == "FLOW_CALLBACK" and http_old == "HTTP_400_EXECUTION_NOT_FOUND"
    assert r_new == "COORDINATOR_OK" and http_new == "HTTP_200"
    print("[PASS] 场景1 索引miss + plan行就绪: 旧=400, 新=200")

    # 场景 2：advance 晚 200ms 才写入 current_agv_task_id
    db2 = {
        "by_current": {},
        "plans": {PLAN: plan_ready},
        "on_retry": lambda _: None,
    }

    def commit_on_attempt(n: int) -> None:
        if n == 2:
            db2["by_current"][UNLOAD] = plan_ready

    db2["on_retry"] = commit_on_attempt
    assert new_handle(db2, UNLOAD) == "COORDINATOR_OK"
    print("[PASS] 场景2 第2次重试前索引追上: 新=COORDINATOR_OK")

    # 场景 3：协调器仍失败 → 503 而非 flow 400
    db3 = {"by_current": {}, "plans": {}, "on_retry": lambda _: None}
    r3 = new_handle(db3, UNLOAD)
    http3 = controller_new(r3 == "COORDINATOR_OK", UNLOAD)
    assert r3 == "HTTP_503_RETRY" and http3 == "HTTP_503"
    print("[PASS] 场景3 计划永远找不到: HTTP_503 可重试")

    # 场景 4：纯流程 executionId 无后缀
    flow_exec = PLAN
    db4 = {"by_current": {}, "plans": {}, "on_retry": lambda _: None}
    assert new_handle(db4, flow_exec) == "FLOW_OR_LOADING"
    assert controller_new(False, flow_exec) == "HTTP_400_EXECUTION_NOT_FOUND"
    print("[PASS] 场景4 无后缀仍走原 flow 路径（行为不变）")

    print("\n全部时间线模拟通过。")


if __name__ == "__main__":
    main()
