package cn.zhicloud.module.mes.dal.dataobject.md.bom;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.enums.DictTypeConstants;
import cn.zhicloud.module.mes.enums.md.MesBomTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * MES 独立 BOM 主数据 DO
 *
 * 区别于 mes_md_product_bom（按物料挂载的单层 BOM），本表为独立 BOM 主数据，
 * 支持工程 BOM / 制造 BOM / 虚拟件区分、多版本管理。
 *
 * @author 智云
 */
@TableName("mes_md_bom")
@KeySequence("mes_md_bom_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesBomDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * BOM 编号
     */
    private String bomNo;
    /**
     * 产品编号
     *
     * 关联 {@link MesMdItemDO#getId()}
     */
    private Long productId;
    /**
     * BOM 类型
     *
     * 字典 {@link DictTypeConstants#MES_BOM_TYPE}
     * 枚举 {@link MesBomTypeEnum}
     */
    private String bomType;
    /**
     * 版本号
     */
    private String version;
    /**
     * 状态
     *
     * 枚举 {@link cn.zhicloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}