package com.librax.lab.module.resource.service.agvroute;

import com.librax.lab.framework.common.util.object.BeanUtils;
import com.librax.lab.module.resource.controller.admin.agvroute.vo.AgvRouteConfigSaveReqVO;
import com.librax.lab.module.resource.dal.dataobject.agvroute.AgvRouteConfigDO;
import com.librax.lab.module.resource.dal.mysql.agvroute.AgvRouteConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * AGV 路由配置 Service 实现。
 *
 * @see AgvRouteConfigService
 */
@Slf4j
@Service
public class AgvRouteConfigServiceImpl implements AgvRouteConfigService {

    @Resource
    private AgvRouteConfigMapper agvRouteConfigMapper;

    // ── 管理端 CRUD ──────────────────────────────────────────────────────────

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAgvRouteConfig(AgvRouteConfigSaveReqVO saveReqVO) {
        if (agvRouteConfigMapper.selectByRouteCode(saveReqVO.getRouteCode()) != null) {
            throw new IllegalArgumentException("路由编码已存在，不可重复创建: " + saveReqVO.getRouteCode());
        }
        AgvRouteConfigDO entity = BeanUtils.toBean(saveReqVO, AgvRouteConfigDO.class);
        agvRouteConfigMapper.insert(entity);
        log.info("[AgvRouteConfig] 新增路由配置 routeCode={} id={}", entity.getRouteCode(), entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAgvRouteConfig(AgvRouteConfigSaveReqVO saveReqVO) {
        // 校验记录存在
        requireById(saveReqVO.getId());
        AgvRouteConfigDO entity = BeanUtils.toBean(saveReqVO, AgvRouteConfigDO.class);
        // routeCode 不允许修改，清空防止被更新
        entity.setRouteCode(null);
        agvRouteConfigMapper.updateById(entity);
        log.info("[AgvRouteConfig] 更新路由配置 id={}", saveReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAgvRouteConfig(Long id) {
        requireById(id);
        agvRouteConfigMapper.deleteById(id);
        log.info("[AgvRouteConfig] 删除路由配置 id={}", id);
    }

    @Override
    public AgvRouteConfigDO getAgvRouteConfig(Long id) {
        return agvRouteConfigMapper.selectById(id);
    }

    @Override
    public List<AgvRouteConfigDO> getAgvRouteConfigList() {
        return agvRouteConfigMapper.selectAll();
    }

    @Override
    public List<AgvRouteConfigDO> getEnabledAgvRouteConfigList() {
        return agvRouteConfigMapper.selectAllEnabled();
    }

    // ── 业务查询 ─────────────────────────────────────────────────────────────

    @Override
    public AgvRouteConfigDO getRequiredByRouteCode(String routeCode) {
        AgvRouteConfigDO config = agvRouteConfigMapper.selectByRouteCode(routeCode);
        if (config == null) {
            throw new IllegalStateException("AGV路由配置不存在，请在后台维护路由配置: " + routeCode);
        }
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new IllegalStateException("AGV路由配置已禁用，请联系管理员启用: " + routeCode);
        }
        return config;
    }

    // ── 私有方法 ─────────────────────────────────────────────────────────────

    private AgvRouteConfigDO requireById(Long id) {
        AgvRouteConfigDO entity = agvRouteConfigMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("AGV路由配置不存在: id=" + id);
        }
        return entity;
    }
}
