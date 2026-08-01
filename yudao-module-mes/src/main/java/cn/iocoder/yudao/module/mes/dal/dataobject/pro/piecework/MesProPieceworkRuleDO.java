package cn.iocoder.yudao.module.mes.dal.dataobject.pro.piecework;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MES 计件工资规则 DO
 *
 * <p>定义按工序 / 工艺路线 / 产品 / 工作站维度的计件单价与阶梯配置，
 * 由 {@code MesProPieceworkRecordService} 在报工审批通过时匹配规则并生成计件明细。
 *
 * <p>匹配优先级（由精到粗）：工作站 + 工序 + 产品 > 工序 + 产品 > 工序 > 产品。
 *
 * @author 芋道源码
 */
@TableName("mes_pro_piecework_rule")
@KeySequence("mes_pro_piecework_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProPieceworkRuleDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 规则名称
     */
    private String ruleName;
    /**
     * 工序编号
     *
     * 关联 {@link MesProProcessDO#getId()}；为空表示不限工序
     */
    private Long processId;
    /**
     * 工艺路线编号
     *
     * 关联 {@link MesProRouteDO#getId()}；为空表示不限路线
     */
    private Long routeId;
    /**
     * 产品物料编号
     *
     * 关联 {@link MesMdItemDO#getId()}；为空表示不限产品
     */
    private Long itemId;
    /**
     * 工作站编号
     *
     * 关联 {@link MesMdWorkstationDO#getId()}；为空表示不限工作站
     */
    private Long workstationId;
    /**
     * 合格品单价（元/件）
     */
    private BigDecimal qualifiedUnitPrice;
    /**
     * 废品单价（元/件，通常为 0 或负值表示扣款）
     */
    private BigDecimal scrapUnitPrice;
    /**
     * 阶梯单价配置（JSON）
     *
     * <p>示例：{@code [{"minQty":0,"maxQty":100,"price":1.5},{"minQty":100,"maxQty":9999,"price":1.8}]}
     */
    private String stepConfig;
    /**
     * 生效日期
     */
    private LocalDate effectiveDate;
    /**
     * 失效日期
     */
    private LocalDate expireDate;
    /**
     * 状态
     *
     * 字典 {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 是否启用
     *
     * 字典 {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum}
     */
    private Integer enabled;
    /**
     * 备注
     */
    private String remark;

}
