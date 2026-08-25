package cn.zhicloud.module.qms.dal.dataobject.inspectionrecord;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.qms.enums.qms.InspectionResultEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QMS 检验记录 DO
 *
 * @author 智云
 */
@TableName("qms_inspection_record")
@KeySequence("qms_inspection_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionRecordDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 检验单 ID
     *
     * 关联 {@link cn.zhicloud.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO#getId()}
     */
    private Long orderId;
    /**
     * 检验项目 ID
     *
     * 关联 {@link cn.zhicloud.module.qms.dal.dataobject.inspectionitem.InspectionItemDO#getId()}
     */
    private Long itemId;
    /**
     * 实测值
     */
    private String measuredValue;
    /**
     * 检验结果
     *
     * 枚举 {@link InspectionResultEnum}
     */
    private Integer result;
    /**
     * 缺陷严重度
     *
     * 枚举 {@link cn.zhicloud.module.qms.enums.qms.InspectionSeverityEnum}
     * 10-致命(CRITICAL) 20-严重(MAJOR) 30-轻微(MINOR)
     */
    private Integer severity;
    /**
     * 检验员
     */
    private String inspector;
    /**
     * 检验时间
     */
    private LocalDateTime inspectTime;
    /**
     * 备注
     */
    private String remark;

}
