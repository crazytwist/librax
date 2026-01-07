package com.librax.lab.module.system.api.logger;

import com.librax.lab.framework.common.biz.system.logger.OperateLogCommonApi;
import com.librax.lab.framework.common.pojo.PageResult;
import com.librax.lab.module.system.api.logger.dto.OperateLogPageReqDTO;
import com.librax.lab.module.system.api.logger.dto.OperateLogRespDTO;

/**
 * 操作日志 API 接口
 *
 * @author 芋道源码
 */
public interface OperateLogApi extends OperateLogCommonApi {

    /**
     * 获取指定模块的指定数据的操作日志分页
     *
     * @param pageReqDTO 请求
     * @return 操作日志分页
     */
    PageResult<OperateLogRespDTO> getOperateLogPage(OperateLogPageReqDTO pageReqDTO);

}
