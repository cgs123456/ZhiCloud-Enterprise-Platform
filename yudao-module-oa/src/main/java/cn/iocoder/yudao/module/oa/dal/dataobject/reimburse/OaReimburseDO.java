package cn.iocoder.yudao.module.oa.dal.dataobject.reimburse;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * OA 报销单 DO
 *
 * @author yudao
 */
@TableName("oa_reimburse")
@KeySequence("oa_reimburse_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaReimburseDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 报销单号
     */
    private String no;
    /**
     * 报销主题
     */
    private String reimburseName;
    /**
     * 申请人 ID
     */
    private Long applicantUserId;
    /**
     * 部门 ID
     */
    private Long deptId;
    /**
     * 报销类型
     * <p>
     * 10 差旅 20 招待 30 办公 40 交通 50 其他
     */
    private Integer reimburseType;
    /**
     * 报销日期
     */
    private LocalDate reimburseDate;
    /**
     * 报销总额
     */
    private BigDecimal totalAmount;
    /**
     * 支付状态
     * <p>
     * 10 未支付 20 部分支付 30 已支付
     */
    private Integer paymentStatus;
    /**
     * 工作流编号
     */
    private String processInstanceId;
    /**
     * 状态
     * <p>
     * 10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
