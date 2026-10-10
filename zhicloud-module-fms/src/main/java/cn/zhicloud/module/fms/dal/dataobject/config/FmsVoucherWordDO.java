package cn.zhicloud.module.fms.dal.dataobject.config;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

/**
 * FMS 凭证字 DO（账套级主数据，租户表）
 *
 * @author 智云
 */
@TableName(value = "fms_voucher_word")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmsVoucherWordDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 账套编号
     */
    private Long accountSetId;
    /**
     * 凭证字
     */
    private String name;
    /**
     * 打印标题
     */
    private String printTitle;
    /**
     * 是否默认凭证字
     */
    private Boolean defaultStatus;
    /**
     * 显示顺序
     */
    private Integer sort;

}
