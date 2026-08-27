package com.librax.lab.module.resource.dal.mysql.agvroute;

import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import com.librax.lab.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.librax.lab.module.resource.dal.dataobject.agvroute.AgvRouteConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AGV 路由配置 Mapper。
 *
 * <p>继承 {@link BaseMapperX} 获得标准 CRUD 能力。
 * 业务查询方法以 {@code default} 方式内联定义，无需额外 XML。
 * {@code deleted = 0} 的过滤由 MyBatis-Plus 逻辑删除插件自动处理。
 */
@Mapper
public interface AgvRouteConfigMapper extends BaseMapperX<AgvRouteConfigDO> {

    /**
     * 按路由编码查询（精确匹配，已启用状态不限）。
     *
     * @param routeCode 路由唯一编码，如 {@code HCL_TO_WAREHOUSE}
     * @return 路由配置，不存在则返回 {@code null}
     */
    default AgvRouteConfigDO selectByRouteCode(String routeCode) {
        return selectOne(new LambdaQueryWrapperX<AgvRouteConfigDO>()
                .eq(AgvRouteConfigDO::getRouteCode, routeCode));
    }

    /**
     * 查询所有已启用的路由配置，按 ID 升序排列。
     * <p>供补料/下料服务枚举可用路由使用。
     *
     * @return 已启用路由列表
     */
    default List<AgvRouteConfigDO> selectAllEnabled() {
        return selectList(new LambdaQueryWrapperX<AgvRouteConfigDO>()
                .eq(AgvRouteConfigDO::getEnabled, true)
                .orderByAsc(AgvRouteConfigDO::getId));
    }

    /**
     * 按起始站点编码查询已启用的路由。
     * <p>供业务层按来源站点筛选可用路由使用。
     *
     * @param sourceStationCode 起始站点编码，如 {@code HCL}
     * @return 匹配的已启用路由列表，按 ID 升序排列
     */
    default List<AgvRouteConfigDO> selectEnabledBySourceStation(String sourceStationCode) {
        return selectList(new LambdaQueryWrapperX<AgvRouteConfigDO>()
                .eq(AgvRouteConfigDO::getSourceStationCode, sourceStationCode)
                .eq(AgvRouteConfigDO::getEnabled, true)
                .orderByAsc(AgvRouteConfigDO::getId));
    }

    /**
     * 查询全部路由（不过滤启用状态），用于管理端列表展示。
     *
     * @return 所有未删除的路由，按 ID 升序排列
     */
    default List<AgvRouteConfigDO> selectAll() {
        return selectList(new LambdaQueryWrapperX<AgvRouteConfigDO>()
                .orderByAsc(AgvRouteConfigDO::getId));
    }
}
