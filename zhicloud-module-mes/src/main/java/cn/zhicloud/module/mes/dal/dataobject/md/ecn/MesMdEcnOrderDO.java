package cn.zhicloud.module.mes.dal.dataobject.md.ecn;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.md.bom.MesBomDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MES ECN 工程变更单 DO
 *
 * <p>记录对 {@link MesBomDO}（独立 BOM 主数据）的工程变更流程。
 * 一条 ECN 单据关联原 BOM 与新 BOM，可挂载多条变更明细 {@link MesMdEcnOrderItemDO}。
 *
 * @author 智云
 */
@TableName("mes_md_ecn_order")
@KeySequence("mes_md_ecn_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesMdEcnOrderDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * ECN 单号
     */
    private String no;
    /**
     * 变更名称
     */
    private String ecnName;
    /**
     * 变更类型
     *
     * 字典 mes_md_ecn_change_type
     * 取值：10 新增 BOM / 20 修改 BOM / 30 删除 BOM / 40 替换物料
     */
    private Integer changeType;
    /**
     * 原 BOM 编号
     *
     * 关联 {@link MesBomDO#getId()}
     * changeType 为 20/30/40 时必填
     */
    private Long bomId;
    /**
     * 新 BOM 编号
     *
     * 关联 {@link MesBomDO#getId()}
     * changeType 为 10/40 时可填
     */
    private Long newBomId;
    /**
     * 变更原因
     */
    private String changeReason;
    /**
     * 变更说明
     */
    private String changeDescription;
    /**
     * 状态
     *
     * 字典 mes_md_ecn_order_status
     * 取值：10 草稿 / 20 审核中 / 30 已批准 / 40 已驳回 / 50 已执行
     */
    private Integer status;
    /**
     * 申请人
     *
     * 关联 AdminUserDO#getId()
     */
    private Long applicantUserId;
    /**
     * 审批人
     *
     * 关联 AdminUserDO#getId()
     */
    private Long approveUserId;
    /**
     * 审批日期
     */
    private LocalDateTime approveDate;
    /**
     * 生效日期
     */
    private LocalDate effectiveDate;
    /**
     * 备注
     */
    private String remark;

}
