package cn.zhicloud.module.hr.dal.dataobject.performance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HRM 绩效流程记录 DO
 *
 * @author zhicloud
 */
@TableName(value = "hr_performance_process_record", autoResultMap = true)
@KeySequence("hr_perf_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrmPerformanceProcessRecordDO extends TenantBaseDO {

    /**
     * 编号
     */
    private Long id;

    /**
     * 业务类型：1绩效计划 2员工考核
     */
    private Integer bizType;

    /**
     * 业务编号
     */
    private Long bizId;

    /**
     * 记录标题
     */
    private String title;

    /**
     * 记录内容
     */
    private String content;

    /**
     * 记录来源：ACTION/BUSINESS/STAGE
     */
    private String source;

    /**
     * 流程状态
     */
    private Integer status;

    /**
     * 操作人员工编号
     */
    private Long operatorEmployeeId;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    /**
     * 附件地址列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> fileUrls;

}
