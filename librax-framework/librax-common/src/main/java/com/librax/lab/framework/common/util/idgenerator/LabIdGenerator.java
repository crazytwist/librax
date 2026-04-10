package com.librax.lab.framework.common.util.idgenerator;


import org.springframework.stereotype.Component;

/**
 * 实验室业务 ID 生成器
 *
 * 基于雪花算法，生成各业务域的唯一 ID。
 * 格式：业务前缀 + 雪花 long（转 36 进制压缩长度）
 *
 * 生成示例：
 *   样本ID:   S-lk4z2p8q1
 *   结果ID:   R-lk4z2p8q2
 *   执行ID:   直接用 UUID（已有，不改）
 *
 * 雪花算法说明：
 *   - 1 bit 符号位（固定0）
 *   - 41 bit 时间戳（毫秒，从 2024-01-01 起算，可用约 69 年）
 *   - 10 bit 工作节点（5 bit datacenterId + 5 bit workerId，支持 1024 个节点）
 *   - 12 bit 序列号（同一毫秒内最多 4096 个）
 */
@Component
public class LabIdGenerator {

    // 起始时间戳：2024-01-01 00:00:00 UTC
    private static final long EPOCH = 1704067200000L;

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);       // 31
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS); // 31
    private static final long SEQUENCE_BITS = 12L;

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS); // 4095

    private final long workerId;
    private final long datacenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    /**
     * 默认构造：workerId/datacenterId 从环境变量读取
     * 多实例部署时通过 -DWORKER_ID=n -DDATACENTER_ID=n 区分
     */
    public LabIdGenerator() {
        this(
                Long.parseLong(System.getProperty("WORKER_ID", "1")),
                Long.parseLong(System.getProperty("DATACENTER_ID", "1"))
        );
    }

    public LabIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0)
            throw new IllegalArgumentException("workerId 范围 0-" + MAX_WORKER_ID);
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0)
            throw new IllegalArgumentException("datacenterId 范围 0-" + MAX_DATACENTER_ID);
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    // ----------------------------------------------------------------
    // 业务 ID 生成接口
    // ----------------------------------------------------------------

    /** 样本 ID：S-xxxxxxxxxx */
    public String nextSampleId() {
        return "S-" + toBase36(nextId());
    }

    /** 检测结果 ID：R-xxxxxxxxxx */
    public String nextResultId() {
        return "R-" + toBase36(nextId());
    }

    /** 物料批次 ID：M-xxxxxxxxxx */
    public String nextMaterialId() {
        return "M-" + toBase36(nextId());
    }

    /** 通用 long ID（给 MyBatis-Plus 主键等场景） */
    public long nextLongId() {
        return nextId();
    }

    // ----------------------------------------------------------------
    // 雪花核心（synchronized 保证单实例线程安全）
    // ----------------------------------------------------------------

    private synchronized long nextId() {
        long ts = currentTimestamp();

        if (ts < lastTimestamp) {
            // 时钟回拨：最多容忍 5ms，否则抛异常
            long diff = lastTimestamp - ts;
            if (diff <= 5) {
                try { Thread.sleep(diff * 2); } catch (InterruptedException ignored) {}
                ts = currentTimestamp();
            }
            if (ts < lastTimestamp) {
                throw new IllegalStateException(
                        "时钟回拨超出容忍范围，回拨 " + diff + "ms");
            }
        }

        if (ts == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) ts = nextMillis(lastTimestamp); // 序列号耗尽，等下一毫秒
        } else {
            sequence = 0L;
        }

        lastTimestamp = ts;

        return ((ts - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long currentTimestamp() {
        return System.currentTimeMillis();
    }

    private long nextMillis(long lastTs) {
        long ts = currentTimestamp();
        while (ts <= lastTs) ts = currentTimestamp();
        return ts;
    }

    /** Long → Base36 字符串（数字+小写字母，比纯数字短约 20%） */
    private String toBase36(long id) {
        return Long.toString(id, 36);
    }
}