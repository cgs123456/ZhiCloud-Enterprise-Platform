package cn.zhicloud.module.wms.dal.dataobject.md.item;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * WMS 商品 DO
 *
 * @author 智云
 */
@TableName("wms_item")
@KeySequence("wms_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsItemDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 商品编号
     */
    private String code;
    /**
     * 商品名称
     */
    private String name;
    /**
     * 单位
     */
    private String unit;
    /**
     * 商品分类编号
     *
     * 关联 {@link WmsItemCategoryDO#getId()}
     */
    private Long categoryId;
    /**
     * 商品品牌编号
     *
     * 关联 {@link WmsItemBrandDO#getId()}
     */
    private Long brandId;
    /**
     * 备注
     */
    private String remark;
    /**
     * ABC 分类
     *
     * 字典 {@link cn.zhicloud.module.wms.enums.DictTypeConstants#ITEM_ABC_CLASSIFICATION}
     * A 类：高频（累计 80% 出库量）；B 类：中频（80%-95%）；C 类：低频（剩余 5%）
     */
    private String abcClassification;

}
