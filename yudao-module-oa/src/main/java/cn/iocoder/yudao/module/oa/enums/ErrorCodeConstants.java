package cn.iocoder.yudao.module.oa.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * OA 错误码枚举类
 * <p>
 * OA 系统，使用 1-060-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== OA 报销（1-060-000-000） ==========
    ErrorCode OA_REIMBURSE_NOT_EXISTS = new ErrorCode(1_060_000_000, "报销单不存在");
    ErrorCode OA_REIMBURSE_NO_DUPLICATE = new ErrorCode(1_060_000_001, "报销单号已存在");
    ErrorCode OA_REIMBURSE_STATUS_INVALID = new ErrorCode(1_060_000_002, "报销单状态非法，无法操作");

    // ========== OA 会议室（1-060-001-000） ==========
    ErrorCode OA_MEETING_ROOM_NOT_EXISTS = new ErrorCode(1_060_001_000, "会议室不存在");
    ErrorCode OA_MEETING_ROOM_STATUS_INVALID = new ErrorCode(1_060_001_001, "会议室当前状态不可预约");
    ErrorCode OA_MEETING_RESERVATION_NOT_EXISTS = new ErrorCode(1_060_001_002, "会议室预约不存在");
    ErrorCode OA_MEETING_RESERVATION_TIME_CONFLICT = new ErrorCode(1_060_001_003, "会议室时段冲突，请更换时间段");
    ErrorCode OA_MEETING_RESERVATION_STATUS_INVALID = new ErrorCode(1_060_001_004, "预约状态非法，无法操作");

    // ========== OA 公文（1-060-002-000） ==========
    ErrorCode OA_DOCUMENT_NOT_EXISTS = new ErrorCode(1_060_002_000, "公文不存在");
    ErrorCode OA_DOCUMENT_NO_DUPLICATE = new ErrorCode(1_060_002_001, "公文编号已存在");
    ErrorCode OA_DOCUMENT_STATUS_INVALID = new ErrorCode(1_060_002_002, "公文状态非法，无法操作");
    ErrorCode OA_DOCUMENT_REVIEW_OPINION_REQUIRED = new ErrorCode(1_060_002_003, "核稿意见不能为空");
    ErrorCode OA_DOCUMENT_SIGN_OPINION_REQUIRED = new ErrorCode(1_060_002_004, "签发意见不能为空");
    ErrorCode OA_DOCUMENT_ARCHIVE_NO_REQUIRED = new ErrorCode(1_060_002_005, "归档编号不能为空");

    // ========== OA 知识库分类（1-060-003-000） ==========
    ErrorCode OA_KNOWLEDGE_CATEGORY_NOT_EXISTS = new ErrorCode(1_060_003_000, "知识分类不存在");
    ErrorCode OA_KNOWLEDGE_CATEGORY_HAS_CHILDREN = new ErrorCode(1_060_003_001, "存在子分类，无法删除");
    ErrorCode OA_KNOWLEDGE_CATEGORY_HAS_ARTICLES = new ErrorCode(1_060_003_002, "分类下存在文章，无法删除");
    ErrorCode OA_KNOWLEDGE_CATEGORY_NAME_DUPLICATE = new ErrorCode(1_060_003_003, "同级分类名称已存在");

    // ========== OA 知识库文章（1-060-004-000） ==========
    ErrorCode OA_KNOWLEDGE_ARTICLE_NOT_EXISTS = new ErrorCode(1_060_004_000, "知识文章不存在");
    ErrorCode OA_KNOWLEDGE_ARTICLE_STATUS_INVALID = new ErrorCode(1_060_004_001, "文章状态非法，无法操作");
    ErrorCode OA_KNOWLEDGE_VERSION_NOT_EXISTS = new ErrorCode(1_060_004_002, "文章版本不存在");

    // ========== OA 知识库评论（1-060-005-000） ==========
    ErrorCode OA_KNOWLEDGE_COMMENT_NOT_EXISTS = new ErrorCode(1_060_005_000, "评论不存在");

    // ========== OA 公告（1-060-006-000） ==========
    ErrorCode OA_ANNOUNCEMENT_NOT_EXISTS = new ErrorCode(1_060_006_000, "公告不存在");
    ErrorCode OA_ANNOUNCEMENT_STATUS_INVALID = new ErrorCode(1_060_006_001, "公告状态非法，无法操作");

    // ========== OA 审批模板库（1-060-007-000） ==========
    ErrorCode OA_APPROVAL_TEMPLATE_NOT_EXISTS = new ErrorCode(1_060_007_000, "审批模板不存在");
    ErrorCode OA_APPROVAL_TEMPLATE_CODE_DUPLICATE = new ErrorCode(1_060_007_001, "审批模板编码已存在");
    ErrorCode OA_APPROVAL_TEMPLATE_DISABLED = new ErrorCode(1_060_007_002, "审批模板已停用，无法使用");

    // ========== OA 日程（1-060-008-000） ==========
    ErrorCode OA_SCHEDULE_NOT_EXISTS = new ErrorCode(1_060_008_000, "日程不存在");

}
