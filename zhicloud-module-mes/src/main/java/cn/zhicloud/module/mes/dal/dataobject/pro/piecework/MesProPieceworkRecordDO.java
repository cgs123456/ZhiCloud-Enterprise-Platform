package cn.zhicloud.module.mes.dal.dataobject.pro.piecework;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * MES 计件工资明细 DO
 *
 * <p>报工审批通过时根据 {@link MesProPieceworkRuleDO} 计算生成，
 * 一条报工对应一条计件明细。{@code periodMonth} 用于按月汇总。
 *
 * @author 智云
 */
@TableName("mes_pro_piecework_record")
@KeySequence("mes_pro_piecework_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProPieceworkRecordDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 报工单编号
     *
     * 关联 {@link MesProFeedbackDO#getId()}
     */
    private Long feedbackId;
    /**
     * 报工用户编号
     *
     * 关联 AdminUserDO#getId()
     */
    private Long feedbackUserId;
    /**
     * 生产工单编号
     *
     * 关联 {@link MesProWorkOrderDO#getId()}
     */
    private Long workOrderId;
    /**
     * 工序编号
     *
     * 关联 {@link MesProProcessDO#getId()}
     */
    private Long processId;
    /**
     * 产品物料编号
     *
     * 关联 {@link MesMdItemDO#getId()}
     */
    private Long itemId;
    /**
     * 工作站编号
     *
     * 关联 {@link MesMdWorkstationDO#getId()}
     */
    private Long workstationId;
    /**
     * 合格品数量
     */
    private BigDecimal qualifiedQty;
    /**
     * 废品数量（合格 + 工废 + 料废 + 其他废 的合计废品）
     */
    private BigDecimal scrapQty;
    /**
     * 工废数量（用于废品扣款单独核算）
     */
    private BigDecimal laborScrapQty;
    /**
     * 合格品单价（元/件，命中规则时的单价）
     */
    private BigDecimal unitPrice;
    /**
     * 废品单价（元/件）
     */
    private BigDecimal scrapUnitPrice;
    /**
     * 工资金额合计 = qualifiedQty * unitPrice + laborScrapQty * scrapUnitPrice
     */
    private BigDecimal totalAmount;
    /**
     * 所属月份（yyyyMM）
     */
    private String periodMonth;
    /**
     * 状态
     *
     * 字典 {@link cn.zhicloud.framework.common.enums.CommonStatusEnum}
     * 0=正常 1=作废
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
