package cn.iocoder.yudao.module.mes.dal.dataobject.pro.mrp;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.enums.DictTypeConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesProMrpPlanStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * MES MRP 计划 DO
 *
 * @author 芋道源码
 */
@TableName("mes_pro_mrp_plan")
@KeySequence("mes_pro_mrp_plan_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProMrpPlanDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * MRP 计划编号
     */
    private String planNo;
    /**
     * 计划日期
     */
    private LocalDateTime planDate;
    /**
     * 状态
     *
     * 字典 {@link DictTypeConstants#MES_PRO_MRP_PLAN_STATUS}
     * 枚举 {@link MesProMrpPlanStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
