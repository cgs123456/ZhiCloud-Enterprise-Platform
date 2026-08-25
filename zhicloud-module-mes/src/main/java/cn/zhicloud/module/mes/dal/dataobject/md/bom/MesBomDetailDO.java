package cn.zhicloud.module.mes.dal.dataobject.md.bom;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * MES BOM 明细（子件清单）DO
 *
 * @author 智云
 */
@TableName("mes_md_bom_detail")
@KeySequence("mes_md_bom_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesBomDetailDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * BOM 主数据编号
     *
     * 关联 {@link MesBomDO#getId()}
     */
    private Long bomId;
    /**
     * 子件产品编号
     *
     * 关联 {@link MesMdItemDO#getId()}
     */
    private Long productId;
    /**
     * 用量
     */
    private BigDecimal quantity;
    /**
     * 单位
     */
    private String unit;
    /**
     * 损耗率（百分比，0-100）
     *
     * BOM 展算与成本卷积时按 (1 + scrapRate/100) 放大用量
     */
    private BigDecimal scrapRate;
    /**
     * 标准单位成本
     *
     * 叶子子件的成本取数来源，供 BOM 成本卷积自底向上累加（无独立 BOM 时使用）
     */
    private BigDecimal unitCost;
    /**
     * 备注
     */
    private String remark;

}