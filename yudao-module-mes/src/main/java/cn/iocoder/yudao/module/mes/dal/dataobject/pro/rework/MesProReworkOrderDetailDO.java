package cn.iocoder.yudao.module.mes.dal.dataobject.pro.rework;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.enums.DictTypeConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesDefectTypeEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesRepairMethodEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * MES 返工工单明细 DO
 *
 * @author 芋道源码
 */
@TableName("mes_pro_rework_order_detail")
@KeySequence("mes_pro_rework_order_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProReworkOrderDetailDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 返工工单 ID
     *
     * 关联 {@link MesProReworkOrderDO#getId()}
     */
    private Long reworkOrderId;
    /**
     * 缺陷描述
     */
    private String defectDescription;
    /**
     * 缺陷数量
     */
    private BigDecimal defectQuantity;
    /**
     * 缺陷类型
     *
     * 字典 {@link DictTypeConstants#MES_DEFECT_TYPE}
     * 枚举 {@link MesDefectTypeEnum}
     */
    private Integer defectType;
    /**
     * 处理方式
     *
     * 字典 {@link DictTypeConstants#MES_REPAIR_METHOD}
     * 枚举 {@link MesRepairMethodEnum}
     */
    private Integer repairMethod;
    /**
     * 处理描述
     */
    private String repairDescription;
    /**
     * 已处理数量
     */
    private BigDecimal repairedQuantity;
    /**
     * 显示顺序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;

}
