package cn.zhicloud.module.mes.dal.dataobject.md.ecn;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDetailDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * MES ECN 工程变更明细 DO
 *
 * <p>记录变更项 {@code changeItem}（物料 / 数量 / 工序 / 备注）的原值、新值，
 * 以及对应的原 BOM 明细 {@link MesBomDetailDO#getId()} 与新 BOM 明细。
 *
 * @author 智云
 */
@TableName("mes_md_ecn_order_item")
@KeySequence("mes_md_ecn_order_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesMdEcnOrderItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * ECN 单编号
     *
     * 关联 {@link MesMdEcnOrderDO#getId()}
     */
    private Long ecnOrderId;
    /**
     * 变更项
     *
     * 字典 mes_md_ecn_change_item
     * 取值：10 物料 / 20 数量 / 30 工序 / 40 备注
     */
    private Integer changeItem;
    /**
     * 原值
     */
    private String oldValue;
    /**
     * 新值
     */
    private String newValue;
    /**
     * 原 BOM 明细编号
     *
     * 关联 {@link MesBomDetailDO#getId()}
     */
    private Long bomDetailId;
    /**
     * 新 BOM 明细编号
     *
     * 关联 {@link MesBomDetailDO#getId()}
     */
    private Long newBomDetailId;
    /**
     * 备注
     */
    private String remark;

}
