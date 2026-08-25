package cn.zhicloud.module.hr.dal.dataobject.department;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * HR 部门 DO
 *
 * @author zhicloud
 */
@TableName("hr_department")
@KeySequence("hr_department_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrDepartmentDO extends TenantBaseDO {

    /**
     * 根节点 ID
     */
    public static final Long PARENT_ID_ROOT = 0L;

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 父部门 ID
     *
     * 关联 {@link #id}，根节点为 0
     */
    private Long parentId;
    /**
     * 部门编码
     */
    private String code;
    /**
     * 部门名称
     */
    private String name;
    /**
     * 部门负责人（员工 ID）
     */
    private Long leaderId;
    /**
     * 状态
     *
     * 枚举 {@link cn.zhicloud.module.hr.enums.department.HrDepartmentStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}