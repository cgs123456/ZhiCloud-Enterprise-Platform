package cn.zhicloud.module.qms.dal.dataobject.inspectionitem;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.qms.enums.qms.InspectionMethodEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * QMS 检验项目 DO
 *
 * @author 智云
 */
@TableName("qms_inspection_item")
@KeySequence("qms_inspection_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionItemDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 检验项目编码
     */
    private String code;
    /**
     * 检验项目名称
     */
    private String name;
    /**
     * 检验类型
     *
     * 枚举 {@link InspectionTypeEnum}
     */
    private Integer type;
    /**
     * 检验方法
     *
     * 枚举 {@link InspectionMethodEnum}
     */
    private Integer method;
    /**
     * 检验标准
     */
    private String standard;
    /**
     * 目标值
     */
    private String target;
    /**
     * 上限
     */
    private BigDecimal upperLimit;
    /**
     * 下限
     */
    private BigDecimal lowerLimit;
    /**
     * 单位
     */
    private String unit;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态
     *
     * 枚举 {@link cn.zhicloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;

}
