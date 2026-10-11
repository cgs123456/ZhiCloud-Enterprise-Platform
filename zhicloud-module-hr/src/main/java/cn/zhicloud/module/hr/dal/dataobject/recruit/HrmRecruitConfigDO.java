package cn.zhicloud.module.hr.dal.dataobject.recruit;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;

/**
 * HRM 招聘配置 DO（淘汰原因等）
 *
 * @author zhicloud
 */
@TableName(value = "hr_recruit_config", autoResultMap = true)
@KeySequence("hr_recruit_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmRecruitConfigDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 配置种类：1淘汰原因
     */
    private Integer kind;

    /**
     * 配置内容（淘汰原因字符串数组）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> content;

}
