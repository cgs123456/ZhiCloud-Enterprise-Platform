package cn.zhicloud.module.erp.dal.dataobject.production.mrp;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 物料需求计划 DO
 *
 * @author 智云
 */
@TableName("erp_mrp_plan")
@KeySequence("erp_mrp_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpMrpPlanDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 计划编号
     */
    private String no;
    /**
     * 计划名称
     */
    private String planName;
    /**
     * 计划日期
     */
    private LocalDate planDate;
    /**
     * 关联 MPS 主生产计划编号
     *
     * 关联 {@link cn.zhicloud.module.erp.dal.dataobject.production.mps.ErpMpsPlanDO#getId()}
     */
    private Long mpsPlanId;
    /**
     * 状态
     *
     * 10 草稿 / 20 已计算 / 30 已确认 / 40 已关闭
     */
    private Integer status;
    /**
     * 总需求量
     */
    private BigDecimal totalDemandCount;
    /**
     * 总采购量
     */
    private BigDecimal totalPurchaseCount;
    /**
     * 总生产量
     */
    private BigDecimal totalProduceCount;
    /**
     * 备注
     */
    private String remark;

}
