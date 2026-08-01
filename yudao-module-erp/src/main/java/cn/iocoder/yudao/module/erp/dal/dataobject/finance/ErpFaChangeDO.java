package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFixedAssetDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * ERP 固定资产变动记录 DO
 *
 * <p>记录固定资产的部门转移、状态变动、原值/使用年限/残值/折旧方法调整等变动历史。
 * 审核通过后，会根据变动类型实际更新 {@link ErpFixedAssetDO} 对应字段。
 *
 * @author 芋道源码
 */
@TableName("erp_fa_change")
@KeySequence("erp_fa_change_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpFaChangeDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 固定资产编号
     *
     * 关联 {@link ErpFixedAssetDO#getId()}
     */
    private Long assetId;
    /**
     * 变动类型（10 部门转移 / 20 状态变动 / 30 原值调整 / 40 使用年限调整 / 50 残值调整 / 60 折旧方法变更）
     */
    private Integer changeType;
    /**
     * 变更前值
     */
    private String beforeValue;
    /**
     * 变更后值
     */
    private String afterValue;
    /**
     * 变更日期
     */
    private LocalDate changeDate;
    /**
     * 变更原因
     */
    private String changeReason;
    /**
     * 操作员编号
     */
    private Long operatorId;
    /**
     * 审批人编号
     */
    private Long approverId;
    /**
     * 状态（10 待审核 / 20 已审核 / 30 已驳回）
     */
    private Integer status;
    /**
     * 驳回原因
     */
    private String rejectReason;
    /**
     * 备注
     */
    private String remark;

}
