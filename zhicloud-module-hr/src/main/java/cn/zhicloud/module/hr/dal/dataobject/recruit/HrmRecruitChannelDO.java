package cn.zhicloud.module.hr.dal.dataobject.recruit;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;


/**
 * HRM 招聘渠道 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_recruit_channel", autoResultMap = false)
@KeySequence("hr_recruit_channel_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmRecruitChannelDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 渠道名称
     */
    private String name;

    /**
     * 是否系统内置（不可删）
     */
    private Boolean systemFlag;

    /**
     * 状态：0开启 1关闭
     */
    private Integer status;

    /**
     * 显示顺序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

}
