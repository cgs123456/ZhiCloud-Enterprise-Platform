package cn.iocoder.yudao.module.oa.dal.dataobject.announcement;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * OA 公告 DO
 *
 * @author yudao
 */
@TableName("oa_announcement")
@KeySequence("oa_announcement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaAnnouncementDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 标题
     */
    private String title;
    /**
     * 正文内容
     */
    private String content;
    /**
     * 公告分类
     * <p>
     * company 公司 / dept 部门 / policy 制度 / notice 通知
     */
    private String category;
    /**
     * 优先级
     * <p>
     * 10 普通 20 重要 30 紧急
     */
    private Integer priority;
    /**
     * 发布人用户 ID
     */
    private Long publisherUserId;
    /**
     * 发布人姓名
     */
    private String publisherName;
    /**
     * 目标范围
     * <p>
     * all 全员 / dept_id 列表（逗号分隔）
     */
    private String targetScope;
    /**
     * 发布时间
     */
    private LocalDateTime publishTime;
    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 是否置顶
     */
    private Boolean topFlag;
    /**
     * 状态
     * <p>
     * 10 草稿 20 已发布 30 已下架 40 已过期
     */
    private Integer status;
    /**
     * 浏览次数
     */
    private Integer viewCount;
    /**
     * 备注
     */
    private String remark;

}
