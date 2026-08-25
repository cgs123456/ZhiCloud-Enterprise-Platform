package cn.zhicloud.module.erp.dal.dataobject.production.mps;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.enums.production.mps.ErpMpsPlanStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 主生产计划 DO
 *
 * @author 智云
 */
@TableName("erp_mps_plan")
@KeySequence("erp_mps_plan_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpMpsPlanDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 计划编号
     */
    private String planNo;
    /**
     * 产品编号
     *
     * 关联 {@link cn.zhicloud.module.erp.dal.dataobject.product.ErpProductDO#getId()}
     */
    private Long productId;
    /**
     * 产品编码（冗余）
     */
    private String productCode;
    /**
     * 产品名称（冗余）
     */
    private String productName;
    /**
     * 计划周期（yyyyMM）
     */
    private String planPeriod;
    /**
     * 计划类型
     *
     * 枚举 {@link cn.zhicloud.module.erp.enums.production.mps.ErpMpsPlanTypeEnum}
     */
    private Integer planType;
    /**
     * 需求日期
     */
    private LocalDate demandDate;
    /**
     * 计划数量
     */
    private BigDecimal plannedQuantity;
    /**
     * 计划完工日期
     */
    private LocalDate plannedFinishDate;
    /**
     * 来源
     *
     * 枚举 {@link cn.zhicloud.module.erp.enums.production.mps.ErpMpsPlanSourceEnum}
     */
    private Integer source;
    /**
     * 来源订单编号
     */
    private Long sourceOrderId;
    /**
     * 状态
     *
     * 枚举 {@link ErpMpsPlanStatusEnum}
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