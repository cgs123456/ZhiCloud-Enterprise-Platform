package cn.iocoder.yudao.module.mes.dal.dataobject.pro.aps;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.enums.DictTypeConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesProApsPlanPriorityEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProApsPlanStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MES 排产计划 DO
 *
 * @author 芋道源码
 */
@TableName("mes_pro_aps_plan")
@KeySequence("mes_pro_aps_plan_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProApsPlanDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 排产计划编号
     */
    private String planNo;
    /**
     * 生产工单编号
     *
     * 关联 {@link MesProWorkOrderDO#getId()}
     */
    private Long workOrderId;
    /**
     * 产品编号
     *
     * 关联 {@link MesMdItemDO#getId()}
     */
    private Long productId;
    /**
     * 工位编号
     *
     * 关联 {@link MesMdWorkstationDO#getId()}
     */
    private Long workstationId;
    /**
     * 计划开始时间
     */
    private LocalDateTime plannedStartTime;
    /**
     * 计划结束时间
     */
    private LocalDateTime plannedEndTime;
    /**
     * 排产数量
     */
    private BigDecimal quantity;
    /**
     * 优先级
     *
     * 字典 {@link DictTypeConstants#MES_PRO_APS_PLAN_PRIORITY}
     * 枚举 {@link MesProApsPlanPriorityEnum}
     */
    private Integer priority;
    /**
     * 状态
     *
     * 字典 {@link DictTypeConstants#MES_PRO_APS_PLAN_STATUS}
     * 枚举 {@link MesProApsPlanStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
