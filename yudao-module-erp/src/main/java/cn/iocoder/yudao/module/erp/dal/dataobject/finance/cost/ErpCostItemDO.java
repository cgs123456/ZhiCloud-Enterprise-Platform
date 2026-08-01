package cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.erp.enums.finance.cost.ErpCostItemTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 成本项目 DO
 *
 * <p>成本项目用于归集产品成本的不同组成部分，如材料、人工、制造费用等。
 *
 * @author 芋道源码
 */
@TableName("erp_cost_item")
@KeySequence("erp_cost_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCostItemDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 成本项目编码
     */
    private String code;
    /**
     * 成本项目名称
     */
    private String name;
    /**
     * 类型
     *
     * 枚举 {@link ErpCostItemTypeEnum}
     */
    private Integer type;
    /**
     * 计算方法（如：标准成本法/实际成本法/加权平均法）
     */
    private String calculationMethod;
    /**
     * 是否标准成本（0 否 1 是）
     */
    private Integer isStandard;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态（0 启用 1 停用）
     */
    private Integer status;

}
