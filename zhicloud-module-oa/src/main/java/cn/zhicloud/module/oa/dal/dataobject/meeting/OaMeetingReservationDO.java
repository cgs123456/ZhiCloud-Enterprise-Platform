package cn.zhicloud.module.oa.dal.dataobject.meeting;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * OA 会议室预约 DO
 *
 * @author zhicloud
 */
@TableName("oa_meeting_reservation")
@KeySequence("oa_meeting_reservation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaMeetingReservationDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 会议主题
     */
    private String title;
    /**
     * 会议室 ID
     */
    private Long roomId;
    /**
     * 组织人 ID
     */
    private Long organizerUserId;
    /**
     * 参会人（逗号分隔）
     */
    private String attendeeUserIds;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 状态
     * <p>
     * 10 待确认 20 已确认 30 已取消 40 已完成
     */
    private Integer status;
    /**
     * 是否提醒
     */
    private Boolean reminderEnabled;
    /**
     * 提前提醒分钟
     */
    private Integer reminderMinutes;
    /**
     * 备注
     */
    private String remark;

}
