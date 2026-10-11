package cn.zhicloud.module.hr.dal.dataobject.performance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * HRM 绩效结果模板 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_performance_result_template", autoResultMap = true)
@KeySequence("hr_perf_result_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmPerformanceResultTemplateDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 结果模板名称
     */
    private String name;

    /**
     * 结果等级数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> levels;

    /**
     * 状态：0开启 1关闭
     */
    private Integer status;

}
