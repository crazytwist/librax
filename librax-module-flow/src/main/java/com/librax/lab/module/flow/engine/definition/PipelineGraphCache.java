package com.librax.lab.module.flow.engine.definition;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.librax.lab.module.flow.engine.definition.model.PipelineGraph;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 流程图缓存
 *
 * <p>对外唯一入口：调度器、执行服务只通过此类获取 {@link PipelineGraph}，
 * 不直接调用 Builder 或 Validator。
 *
 * <p>缓存策略：
 * <ul>
 *   <li>key = pipelineKey:version，不同版本独立缓存，互不影响
 *   <li>首次访问时加载（懒加载），加载失败不写入缓存
 *   <li>发布新版本 / 修改定义后调用 {@link #invalidate} 主动失效
 *   <li>Caffeine Cache 保证同一 key 只加载一次（无重复 DB 查询）
 *   <li>maximumSize=256 防止版本过多导致 OOM
 *   <li>expireAfterAccess=2h，运行中的执行会持续访问保持热，结束后自然淘汰
 * </ul>
 */
@Slf4j
@Component
public class PipelineGraphCache {

    private final PipelineGraphBuilder builder;
    private final PipelineGraphValidator validator;

    /**
     * key = "pipelineKey:version"
     * <p>Caffeine 替代 ConcurrentHashMap：
     * - maximumSize 防止无限增长导致 OOM
     * - expireAfterAccess 让不再使用的版本自动淘汰
     * - 运行中的执行会持续 get() 访问，自动续期不会被淘汰
     */
    private final Cache<String, PipelineGraph> cache = Caffeine.newBuilder()
            .maximumSize(256)
            .expireAfterAccess(2, TimeUnit.HOURS)
            .removalListener((key, value, cause) ->
                    log.info("[PipelineGraphCache] 缓存淘汰 key={} cause={}", key, cause))
            .build();

    public PipelineGraphCache(PipelineGraphBuilder builder, PipelineGraphValidator validator) {
        this.builder = builder;
        this.validator = validator;
    }

    // ----------------------------------------------------------------
    // 对外接口
    // ----------------------------------------------------------------

    /**
     * 获取流程图，缓存未命中时自动加载并校验
     *
     * @param pipelineKey 流程标识
     * @param version     版本号
     * @return 校验通过的 PipelineGraph
     * @throws com.librax.lab.framework.common.exception.ServiceException 流程不存在、未发布、校验失败时抛出
     */
    public PipelineGraph get(String pipelineKey, Integer version) {
        String key = cacheKey(pipelineKey, version);
        // Caffeine.get：同一 key 并发时只有一个线程执行 load，其余等待结果
        return cache.get(key, k -> load(pipelineKey, version));
    }

    /**
     * 失效指定版本的缓存
     * <p>调用时机：流程定义发布、节点编排修改、步骤定义变更后
     *
     * @param pipelineKey 流程标识
     * @param version     版本号
     */
    public void invalidate(String pipelineKey, Integer version) {
        String key = cacheKey(pipelineKey, version);
        PipelineGraph removed = cache.getIfPresent(key);
        cache.invalidate(key);
        if (removed != null) {
            log.info("[PipelineGraphCache] 缓存已失效 pipeline_key={} version={}",
                    pipelineKey, version);
        } else {
            log.debug("[PipelineGraphCache] 缓存不存在，无需失效 pipeline_key={} version={}",
                    pipelineKey, version);
        }
    }

    /**
     * 失效某个 pipelineKey 的所有版本缓存
     * <p>调用时机：流程被整体禁用时
     *
     * @param pipelineKey 流程标识
     */
    public void invalidateAll(String pipelineKey) {
        String prefix = pipelineKey + ":";
        cache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
        log.info("[PipelineGraphCache] 所有版本缓存已失效 pipeline_key={}", pipelineKey);
    }

    /**
     * 预加载：服务启动或发布时主动加载到缓存，避免首次请求延迟
     *
     * @param pipelineKey 流程标识
     * @param version     版本号
     */
    public void preload(String pipelineKey, Integer version) {
        String key = cacheKey(pipelineKey, version);
        if (cache.getIfPresent(key) != null) {
            log.debug("[PipelineGraphCache] 已缓存，跳过预加载 pipeline_key={} version={}",
                    pipelineKey, version);
            return;
        }
        cache.put(key, load(pipelineKey, version));
        log.info("[PipelineGraphCache] 预加载完成 pipeline_key={} version={}",
                pipelineKey, version);
    }

    /**
     * 查询是否已缓存
     */
    public boolean isCached(String pipelineKey, Integer version) {
        return cache.getIfPresent(cacheKey(pipelineKey, version)) != null;
    }

    /**
     * 当前缓存数量（用于监控）
     */
    public long size() {
        return cache.estimatedSize();
    }

    // ----------------------------------------------------------------
    // 内部方法
    // ----------------------------------------------------------------

    /**
     * 从 DB 加载并校验，校验失败直接抛异常，不写入缓存
     */
    private PipelineGraph load(String pipelineKey, Integer version) {
        log.info("[PipelineGraphCache] 缓存未命中，开始加载 pipeline_key={} version={}",
                pipelineKey, version);
        // Builder：三表查询 + 三层参数合并
        PipelineGraph graph = builder.build(pipelineKey, version);
        // Validator：7条规则 + 环检测 + 填充 dag 字段
        validator.validate(graph);
        return graph;
    }

    private String cacheKey(String pipelineKey, Integer version) {
        return pipelineKey + ":" + version;
    }
}