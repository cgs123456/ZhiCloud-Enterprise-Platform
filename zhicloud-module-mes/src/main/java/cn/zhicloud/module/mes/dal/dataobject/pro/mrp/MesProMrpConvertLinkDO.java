package cn.zhicloud.module.mes.dal.dataobject.pro.mrp;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * MES MRP 结果行→工单回链 DO
 *
 * 转建议时逐行落链（一次性转换语义，plan 状态 CAS 保证无重复写），行级追溯「结果行 → 草稿工单」；
 * plan 级 orderSourceCode 之外的补充（ADR 270 D6/D7）。历史已转计划无链可回溯、不回填。
 *
 * @author 智云
 */
@TableName("mes_pro_mrp_convert_link")
@KeySequence("mes_pro_mrp_convert_link_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProMrpConvertLinkDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * MRP 计划编号
     *
     * 关联 {@link MesProMrpPlanDO#getId()}
     */
    private Long planId;
    /**
     * MRP 结果行编号
     *
     * 关联 {@link MesProMrpResultDO#getId()}
     */
    private Long resultId;
    /**
     * 生产工单编号
     *
     * 关联 {@link cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO#getId()}
     */
    private Long workOrderId;

}
