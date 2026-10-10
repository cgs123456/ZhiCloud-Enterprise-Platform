package cn.zhicloud.module.fms.dal.dataobject.config;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * FMS 账套成员 DO
 *
 * @author 智云
 */
@TableName("fms_account_user")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmsAccountUserDO extends TenantBaseDO {

    /** 成员权限级别 - 账套主管（OWNER） */
    public static final Integer LEVEL_ADMIN = 1;
    /** 成员权限级别 - 查看者（READ） */
    public static final Integer LEVEL_READ = 2;
    /** 成员权限级别 - 会计（WRITE） */
    public static final Integer LEVEL_MEMBER = 3;

    @TableId
    private Long id;
    /** 账套编号，关联 {@link FmsAccountSetDO#getId()} */
    private Long accountSetId;
    /** 后台用户编号 */
    private Long userId;
    /** 成员权限级别：1=主管 2=查看者 3=会计 */
    private Integer level;
    /** 是否默认账套 */
    private Boolean defaultStatus;

}
