package cn.zhicloud.module.qms.dal.dataobject.fmea;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * QMS FMEA 条目 DO
 *
 * @author 智云
 */
@TableName("qms_fmea_item")
@KeySequence("qms_fmea_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmeaItemDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * FMEA 文档 ID
     *
     * 关联 {@link FmeaDocumentDO#getId()}
     */
    private Long fmeaId;
    /**
     * 功能
     *
     * 落库列名为 item_function：`function` 是 MySQL 8.0 保留字，
     * 而 MyBatis-Plus 默认不转义标识符——`SELECT item_function AS function`
     * 会因别名未加反引号报 SQL 语法错误（任何 select 必 500，§12.222 修复）。
     * 故 Java 字段与对外 API 字段名统一改为 itemFunction（内部管理端点，
     * 无对外消费者；按「删除优于兼容」直接改名）。
     */
    @TableField("item_function")
    private String itemFunction;
    /**
     * 失效模式
     */
    private String failureMode;
    /**
     * 失效后果
     */
    private String failureEffect;
    /**
     * 严重度 S（1-10）
     */
    private Integer severity;
    /**
     * 潜在失效原因
     */
    private String potentialCause;
    /**
     * 频度 O（1-10）
     */
    private Integer occurrence;
    /**
     * 现行控制措施
     */
    private String currentControls;
    /**
     * 探测度 D（1-10）
     */
    private Integer detection;
    /**
     * 风险优先数 RPN = S * O * D（1-1000，自动计算）
     */
    private Integer rpn;
    // ========== P0-10 AIAG-VDA AP 行动优先级 ==========
    /**
     * 行动优先级（AIAG-VDA 2019）
     *
     * 枚举 {@link cn.zhicloud.module.qms.enums.qms.FmeaActionPriorityEnum}
     * HIGH/MEDIUM/LOW，基于 S/O/D 组合查表自动计算
     */
    private String actionPriority;
    /**
     * 建议措施
     */
    private String actionRecommended;
    /**
     * 已采取措施
     */
    private String actionTaken;
    /**
     * 备注
     */
    private String remark;

}
