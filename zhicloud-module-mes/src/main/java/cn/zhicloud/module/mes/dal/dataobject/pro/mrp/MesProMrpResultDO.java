package cn.zhicloud.module.mes.dal.dataobject.pro.mrp;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.dal.dataobject.md.vendor.MesMdVendorDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MES MRP 计算结果 DO
 *
 * @author 智云
 */
@TableName("mes_pro_mrp_result")
@KeySequence("mes_pro_mrp_result_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProMrpResultDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * MRP 计划编号
     *
     * 关联 {@link MesProMrpPlanDO#getId()}
     */
    private Long planId;
    /**
     * 物料编号
     *
     * 关联 {@link MesMdItemDO#getId()}
     */
    private Long productId;
    /**
     * 需求量
     */
    private BigDecimal requirementQty;
    /**
     * 库存量
     */
    private BigDecimal stockQty;
    /**
     * 净需求
     */
    private BigDecimal netRequirement;
    /**
     * 安全库存（P0-11：MRP 计算时的库存缓冲）
     */
    private BigDecimal safetyStock;
    /**
     * 批量规则（P0-11：LFL/FOQ/POQ/MULTIPLES）
     *
     * 枚举 {@link cn.zhicloud.module.mes.enums.md.MesMrpLotSizeRuleEnum}
     */
    private String lotSizeRule;
    /**
     * 计划订单量
     */
    private BigDecimal plannedOrderQty;
    /**
     * 计划订单日期
     */
    private LocalDateTime plannedOrderDate;
    /**
     * 供应商编号
     *
     * 关联 {@link MesMdVendorDO#getId()}
     */
    private Long supplierId;
    /**
     * 备注
     */
    private String remark;

}
