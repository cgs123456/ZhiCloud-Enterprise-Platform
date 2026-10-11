package cn.zhicloud.module.hr.dal.dataobject.recruit;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;


/**
 * HRM 招聘职位类型 DO（树）
 *
 * @author zhicloud
 */
@TableName(value = "hr_recruit_post_type", autoResultMap = false)
@KeySequence("hr_recruit_post_type_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmRecruitPostTypeDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 父类型编号（0=根）
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：0开启 1关闭
     */
    private Integer status;

}
