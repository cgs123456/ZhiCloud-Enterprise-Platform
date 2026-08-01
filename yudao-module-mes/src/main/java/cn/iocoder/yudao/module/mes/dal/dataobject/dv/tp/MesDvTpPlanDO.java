package cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.enums.dv.tp.MesDvTpPlanStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * MES TPM 计划 DO
 *
 * @author 芋道源码
 */
@TableName("mes_dv_tp_plan")
@KeySequence("mes_dv_tp_plan_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesDvTpPlanDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 设备编号
     *
     * 关联 {@link cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO#getId()}
     */
    private Long equipmentId;
    /**
     * 计划编号
     */
    private String planNo;
    /**
     * 计划类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.mes.enums.dv.tp.MesDvTpPlanTypeEnum}
     */
    private Integer planType;
    /**
     * 周期类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.mes.enums.dv.tp.MesDvTpCycleTypeEnum}
     */
    private Integer cycleType;
    /**
     * 周期值
     */
    private Integer cycleValue;
    /**
     * 下次执行日期
     */
    private LocalDate nextExecuteDate;
    /**
     * 状态
     *
     * 枚举 {@link MesDvTpPlanStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}