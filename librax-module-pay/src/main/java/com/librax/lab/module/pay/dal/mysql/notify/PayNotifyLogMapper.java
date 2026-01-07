package com.librax.lab.module.pay.dal.mysql.notify;

import com.librax.lab.module.pay.dal.dataobject.notify.PayNotifyLogDO;
import com.librax.lab.framework.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PayNotifyLogMapper extends BaseMapperX<PayNotifyLogDO> {

    default List<PayNotifyLogDO> selectListByTaskId(Long taskId) {
        return selectList(PayNotifyLogDO::getTaskId, taskId);
    }

}
