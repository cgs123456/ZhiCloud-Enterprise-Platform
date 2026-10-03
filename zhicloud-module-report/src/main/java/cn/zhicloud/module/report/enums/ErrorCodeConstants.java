package cn.zhicloud.module.report.enums;

import cn.zhicloud.framework.common.exception.ErrorCode;

/**
 * Report 错误码枚举类
 *
 * report 系统，使用 1-003-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== GoView 模块 1-003-000-000 ==========
    ErrorCode GO_VIEW_PROJECT_NOT_EXISTS = new ErrorCode(1_003_000_000, "GoView 项目不存在");
    ErrorCode GO_VIEW_SQL_NOT_READ_ONLY = new ErrorCode(1_003_000_001, "报表 SQL 只允许单条只读查询（SELECT），且不允许多语句或导出到文件");

}
