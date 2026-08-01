package cn.iocoder.yudao.module.oa.controller.admin.announcement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 公告新增/修改 Request VO")
@Data
public class OaAnnouncementSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "关于国庆放假的通知")
    @NotEmpty(message = "标题不能为空")
    private String title;

    @Schema(description = "正文内容", example = "正文...")
    private String content;

    @Schema(description = "公告分类（company 公司/dept 部门/policy 制度/notice 通知）", example = "company")
    private String category;

    @Schema(description = "优先级（10 普通 20 重要 30 紧急）", example = "10")
    private Integer priority;

    @Schema(description = "发布人用户 ID", example = "2048")
    private Long publisherUserId;

    @Schema(description = "发布人姓名", example = "张三")
    private String publisherName;

    @Schema(description = "目标范围（all 全员/dept_id 列表逗号分隔）", example = "all")
    private String targetScope;

    @Schema(description = "发布时间", example = "2024-01-01 10:00:00")
    private LocalDateTime publishTime;

    @Schema(description = "过期时间", example = "2024-12-31 23:59:59")
    private LocalDateTime expireTime;

    @Schema(description = "是否置顶", example = "false")
    private Boolean topFlag;

    @Schema(description = "状态（10 草稿 20 已发布 30 已下架 40 已过期）", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
