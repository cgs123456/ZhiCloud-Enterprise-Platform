package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 成本中心 DO
 *
 * <p>成本中心用于管理会计（CO）模块，按组织维度归集成本。
 * 采用树形结构（parentId + code）。
 *
 * @author 智云
 */
@TableName("erp_cost_center")
@KeySequence("erp_cost_center_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCostCenterDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 成本中心编码
     */
    private String code;
    /**
     * 成本中心名称
     */
    private String name;
    /**
     * 父级编号（顶级为 0）
     */
    private Long parentId;
    /**
     * 负责人 ID（关联系统用户）
     */
    private Long managerId;
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
