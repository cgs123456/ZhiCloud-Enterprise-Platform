package cn.zhicloud.module.mes.dal.dataobject.md.bom;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MES BOM 替代料 DO
 *
 * 按 BOM 明细行（被替代的物料所在行）挂载，支持优先级、替代比例、生效/失效日期。
 * 当原物料缺料时，可按 priority 升序选取生效替代料。
 *
 * @author 智云
 */
@TableName("mes_bom_substitute")
@KeySequence("mes_bom_substitute_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesBomSubstituteDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * BOM 主表 ID
     *
     * 关联 {@link MesBomDO#getId()}
     */
    private Long bomId;
    /**
     * BOM 明细 ID（被替代的物料所在行）
     *
     * 关联 {@link MesBomDetailDO#getId()}
     */
    private Long bomDetailId;
    /**
     * 替代物料 ID
     *
     * 关联 {@link MesMdItemDO#getId()}
     */
    private Long substituteItemId;
    /**
     * 替代比例
     *
     * 1 单位原物料 = ratio 单位替代料
     */
    private BigDecimal substituteRatio;
    /**
     * 优先级（1=首选，2=次选...，数值越小优先级越高）
     */
    private Integer priority;
    /**
     * 生效日期
     */
    private LocalDate effectiveDate;
    /**
     * 失效日期
     */
    private LocalDate expiryDate;
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