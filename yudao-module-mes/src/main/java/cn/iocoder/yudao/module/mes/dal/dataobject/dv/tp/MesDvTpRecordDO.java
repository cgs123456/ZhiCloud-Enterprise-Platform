package cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * MES TPM 执行记录 DO
 *
 * @author 芋道源码
 */
@TableName("mes_dv_tp_record")
@KeySequence("mes_dv_tp_record_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesDvTpRecordDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * TPM 计划编号
     *
     * 关联 {@link MesDvTpPlanDO#getId()}
     */
    private Long planId;
    /**
     * 设备编号
     *
     * 关联 {@link cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO#getId()}
     */
    private Long equipmentId;
    /**
     * 执行日期
     */
    private LocalDate executeDate;
    /**
     * 执行人编号
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long executorId;
    /**
     * 结果
     *
     * 枚举 {@link cn.iocoder.yudao.module.mes.enums.dv.tp.MesDvTpRecordResultEnum}
     */
    private Integer result;
    /**
     * 发现问题
     */
    private String issuesFound;
    /**
     * 已采取措施
     */
    private String actionTaken;
    /**
     * 备注
     */
    private String remark;

}