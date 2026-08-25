package cn.zhicloud.module.erp.dal.dataobject.stock.vmi;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP VMI 补货建议 DO
 *
 * @author 智云
 */
@TableName("erp_vmi_replenishment")
@KeySequence("erp_vmi_replenishment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVmiReplenishmentDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 补货建议单号
     */
    private String no;
    /**
     * 供应商编号
     */
    private Long supplierId;
    /**
     * 仓库编号
     */
    private Long warehouseId;
    /**
     * 状态
     *
     * 10 待处理 / 20 已生成采购订单 / 30 已完成
     */
    private Integer status;
    /**
     * 合计数量
     */
    private BigDecimal totalQuantity;
    /**
     * 备注
     */
    private String remark;

}
