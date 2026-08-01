package cn.iocoder.yudao.module.tms.dal.dataobject.tracking;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TMS 跟踪事件 DO
 *
 * @author yudao
 */
@TableName("tms_tracking_event")
@KeySequence("tms_tracking_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmsTrackingEventDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 运单编号
     */
    private Long shipmentId;
    /**
     * 事件类型
     *
     * 10 发车 / 20 到达站点 / 30 签收 / 40 异常报告
     */
    private Integer eventType;
    /**
     * 事件时间
     */
    private LocalDateTime eventTime;
    /**
     * 当前位置
     */
    private String location;
    /**
     * 经度
     */
    private BigDecimal longitude;
    /**
     * 纬度
     */
    private BigDecimal latitude;
    /**
     * 描述
     */
    private String description;

}
