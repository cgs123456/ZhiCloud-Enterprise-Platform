package cn.zhicloud.module.mes.dal.dataobject.md.item;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * MES 物料产品 DO
 *
 * @author 智云
 */
@TableName("mes_md_item")
@KeySequence("mes_md_item_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesMdItemDO extends BaseDO {

    /**
     * 物料编号
     */
    @TableId
    private Long id;
    /**
     * 物料编码
     */
    private String code;
    /**
     * 物料名称
     */
    private String name;
    /**
     * 规格型号
     */
    private String specification;
    /**
     * 计量单位编号
     *
     * 关联 {@link MesMdUnitMeasureDO#getId()}
     */
    private Long unitMeasureId;
    /**
     * 物料分类编号
     *
     * 关联 {@link MesMdItemTypeDO#getId()}
     */
    private Long itemTypeId;
    /**
     * 状态
     *
     * 枚举 {@link cn.zhicloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 是否启用安全库存
     */
    private Boolean safeStockFlag;
    /**
     * 最低库存量
     */
    private BigDecimal minStock;
    /**
     * 最高库存量
     */
    private BigDecimal maxStock;
    /**
     * 是否高值物料
     */
    private Boolean highValue;
    /**
     * 是否启用批次管理
     */
    private Boolean batchFlag;
    // ========== P0-11 MRP 低层码 + 安全库存 + 批量规则 ==========
    /**
     * 低层码（Low Level Code，LLC）
     *
     * <p>BOM 树中物料出现的最低层级，用于 MRP 按层级顺序展算。
     * 0 表示顶层产品，数值越大表示层级越深。
     */
    private Integer lowLevelCode;
    /**
     * 安全库存量
     *
     * <p>用于应对需求波动与供应不稳定的库存缓冲。
     * MRP 净需求计算时需保证库存不低于安全库存。
     */
    private BigDecimal safetyStock;
    /**
     * 批量规则
     *
     * 枚举 {@link cn.zhicloud.module.mes.enums.md.MesMrpLotSizeRuleEnum}
     * 默认 LFL（按需批量）
     */
    private String lotSizeRule;
    /**
     * 固定批量大小（FOQ 规则生效时使用）
     */
    private BigDecimal fixedLotSize;
    /**
     * 批量倍数（MULTIPLES 规则生效时使用）
     *
     * <p>计划订单量必须为该值的整数倍
     */
    private BigDecimal lotSizeMultiple;
    /**
     * 采购/制造提前期（天）
     *
     * <p>用于 MRP 计划订单日期倒推
     */
    private Integer leadTimeDays;
    /**
     * 损耗率（百分比，0-100）
     *
     * <p>MRP 计划订单量按损耗率放大
     */
    private BigDecimal scrapRate;
    /**
     * 备注
     */
    private String remark;

}
