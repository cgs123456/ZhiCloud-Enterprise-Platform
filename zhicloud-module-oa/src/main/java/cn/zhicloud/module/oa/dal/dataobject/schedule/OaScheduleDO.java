package cn.zhicloud.module.oa.dal.dataobject.schedule;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * OA 日程 DO
 *
 * <p>个人日程管理，支持提醒、重复、共享。
 *
 * @author 智云
 */
@TableName("oa_schedule")
@KeySequence("oa_schedule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaScheduleDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 用户编号（日程归属人）
     */
    private Long userId;
    /**
     * 标题
     */
    private String title;
    /**
     * 描述
     */
    private String description;
    /**
     * 日程类型
     *
     * 10 日程 / 20 任务 / 30 纪念日 / 40 会议
     */
    private Integer type;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 全天事件
     */
    private Boolean allDay;
    /**
     * 地点
     */
    private String location;
    /**
     * 提醒时间（提前 N 分钟）
     */
    Integer remindMinutes;
    /**
     * 是否已提醒
     */
    private Boolean reminded;
    /**
     * 重复类型
     *
     * 0 不重复 / 10 每天 / 20 每周 / 30 每月 / 40 每年
     */
    private Integer repeatType;
    /**
     * 状态
     *
     * 0 未完成 / 1 已完成 / 2 已取消
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
