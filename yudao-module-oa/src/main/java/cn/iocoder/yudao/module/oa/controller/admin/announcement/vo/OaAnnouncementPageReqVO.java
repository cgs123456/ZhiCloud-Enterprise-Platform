package cn.iocoder.yudao.module.oa.controller.admin.announcement.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 公告分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaAnnouncementPageReqVO extends PageParam {

    @Schema(description = "标题", example = "关于国庆放假的通知")
    private String title;

    @Schema(description = "公告分类", example = "company")
    private String category;

    @Schema(description = "优先级", example = "10")
    private Integer priority;

    @Schema(description = "状态", example = "20")
    private Integer status;

    @Schema(description = "发布人用户 ID", example = "2048")
    private Long publisherUserId;

    @Schema(description = "发布时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] publishTime;

}
