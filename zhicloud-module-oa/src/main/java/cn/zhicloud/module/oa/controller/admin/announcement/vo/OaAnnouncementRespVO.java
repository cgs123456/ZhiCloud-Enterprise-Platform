package cn.zhicloud.module.oa.controller.admin.announcement.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 公告 Response VO")
@Data
@ExcelIgnoreUnannotated
public class OaAnnouncementRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "关于国庆放假的通知")
    @ExcelProperty("标题")
    private String title;

    @Schema(description = "正文内容", example = "正文...")
    @ExcelProperty("正文内容")
    private String content;

    @Schema(description = "公告分类（company 公司/dept 部门/policy 制度/notice 通知）", example = "company")
    @ExcelProperty("公告分类")
    private String category;

    @Schema(description = "优先级（10 普通 20 重要 30 紧急）", example = "10")
    @ExcelProperty("优先级")
    private Integer priority;

    @Schema(description = "发布人用户 ID", example = "2048")
    @ExcelProperty("发布人用户 ID")
    private Long publisherUserId;

    @Schema(description = "发布人姓名", example = "张三")
    @ExcelProperty("发布人姓名")
    private String publisherName;

    @Schema(description = "目标范围（all 全员/dept_id 列表逗号分隔）", example = "all")
    @ExcelProperty("目标范围")
    private String targetScope;

    @Schema(description = "发布时间", example = "2024-01-01 10:00:00")
    @ExcelProperty("发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "过期时间", example = "2024-12-31 23:59:59")
    @ExcelProperty("过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "是否置顶", example = "false")
    @ExcelProperty("是否置顶")
    private Boolean topFlag;

    @Schema(description = "状态（10 草稿 20 已发布 30 已下架 40 已过期）", example = "20")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "浏览次数", example = "0")
    @ExcelProperty("浏览次数")
    private Integer viewCount;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
