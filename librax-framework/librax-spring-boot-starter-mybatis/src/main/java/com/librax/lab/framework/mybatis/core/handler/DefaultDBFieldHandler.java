package com.librax.lab.framework.mybatis.core.handler;

import com.librax.lab.framework.mybatis.core.dataobject.BaseDO;
import com.librax.lab.framework.security.core.util.SecurityFrameworkUtils;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 通用参数填充实现类
 *
 * 如果没有显式的对通用参数进行赋值，这里会对通用参数进行填充、赋值
 *
 * @author hexiaowu
 */
public class DefaultDBFieldHandler implements MetaObjectHandler {

    @Override
    @SuppressWarnings("PatternVariableCanBeUsed")
    public void insertFill(MetaObject metaObject) {
        if (Objects.nonNull(metaObject) && metaObject.getOriginalObject() instanceof BaseDO) {
            BaseDO baseDO = (BaseDO) metaObject.getOriginalObject();

            LocalDateTime current = LocalDateTime.now();
            // 创建时间为空，则以当前时间为插入时间
            if (Objects.isNull(baseDO.getCreateTime())) {
                baseDO.setCreateTime(current);
            }
            // 更新时间为空，则以当前时间为更新时间
            if (Objects.isNull(baseDO.getUpdateTime())) {
                baseDO.setUpdateTime(current);
            }

            // 无登录用户时（如异步线程/系统任务）使用 "0" 作为兜底，避免 NOT NULL 约束异常
            String userIdStr = Objects.toString(SecurityFrameworkUtils.getLoginUserId(), "0");
            if (Objects.isNull(baseDO.getCreator())) {
                baseDO.setCreator(userIdStr);
            }
            if (Objects.isNull(baseDO.getUpdater())) {
                baseDO.setUpdater(userIdStr);
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时间为空，则以当前时间为更新时间
        Object modifyTime = getFieldValByName("updateTime", metaObject);
        if (Objects.isNull(modifyTime)) {
            setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
        }

        // 无登录用户时（如异步线程/系统任务）使用 "0" 作为兜底
        Object modifier = getFieldValByName("updater", metaObject);
        if (Objects.isNull(modifier)) {
            String userIdStr = Objects.toString(SecurityFrameworkUtils.getLoginUserId(), "0");
            setFieldValByName("updater", userIdStr, metaObject);
        }
    }
}
