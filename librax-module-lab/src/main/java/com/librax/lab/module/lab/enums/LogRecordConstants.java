package com.librax.lab.module.lab.enums;

/**
 * Lab 操作日志枚举
 * 目的：统一管理，也减少注解里各种“复杂”字符串
 */
public interface LogRecordConstants {

    // ======================= MATERIAL_INSTANCE 物料实例 =======================

    String MATERIAL_INSTANCE_TYPE = "物料实例";
    String MATERIAL_INSTANCE_CREATE_SUB_TYPE = "创建物料实例";
    String MATERIAL_INSTANCE_CREATE_SUCCESS = "创建了物料实例【{{#instance.instanceId}}】";
    String MATERIAL_INSTANCE_UPDATE_SUB_TYPE = "更新物料实例";
    String MATERIAL_INSTANCE_UPDATE_SUCCESS = "更新了物料实例【{{#updateReqVO.instanceId}}】";
    String MATERIAL_INSTANCE_DELETE_SUB_TYPE = "删除物料实例";
    String MATERIAL_INSTANCE_DELETE_SUCCESS = "删除了物料实例【{{#instance.instanceId}}】";
    String MATERIAL_INSTANCE_DELETE_LIST_SUB_TYPE = "批量删除物料实例";
    String MATERIAL_INSTANCE_DELETE_LIST_SUCCESS = "批量删除了物料实例，编号={{#ids}}";

}
