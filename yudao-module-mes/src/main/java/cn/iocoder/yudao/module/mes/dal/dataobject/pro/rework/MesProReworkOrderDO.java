package cn.iocoder.yudao.module.mes.dal.dataobject.pro.rework;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.enums.DictTypeConstants;
import cn.iocoder.yudao.module.mes.enums.pro.MesProReworkStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProReworkTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MES 返工工单 DO
 *
 * @author 芋道源码
 */
@TableName("mes_pro_rework_order")
@KeySequence("mes_pro_rework_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProReworkOrderDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 返工工单号
     */
    private String code;
    /**
     * 原工单 ID
     *
     * 关联 {@link MesProWorkOrderDO#getId()}
     */
    private Long originalWorkOrderId;
    /**
     * 原工单号
     */
    private String originalWorkOrderCode;
    /**
     * 产品 ID
     */
    private Long productId;
    /**
     * 产品编码
     */
    private String productCode;
    /**
     * 产品名称
     */
    private String productName;
    /**
     * 返工数量
     */
    private BigDecimal reworkQuantity;
    /**
     * 返工原因
     */
    private String reworkReason;
    /**
     * 返工类型
     *
     * 字典 {@link DictTypeConstants#MES_PRO_REWORK_TYPE}
     * 枚举 {@link MesProReworkTypeEnum}
     */
    private Integer reworkType;
    /**
     * 返工工序 ID
     */
    private Long reworkProcessId;
    /**
     * 返工工序名称
     */
    private String reworkProcessName;
    /**
     * 状态
     *
     * 字典 {@link DictTypeConstants#MES_PRO_REWORK_STATUS}
     * 枚举 {@link MesProReworkStatusEnum}
     */
    private Integer status;
    /**
     * 责任人 ID
     */
    private Long responsiblePersonId;
    /**
     * 责任部门 ID
     */
    private Long responsibleDeptId;
    /**
     * 计划开始时间
     */
    private LocalDateTime plannedStartTime;
    /**
     * 计划结束时间
     */
    private LocalDateTime plannedEndTime;
    /**
     * 实际开始时间
     */
    private LocalDateTime actualStartTime;
    /**
     * 实际结束时间
     */
    private LocalDateTime actualEndTime;
    /**
     * 显示顺序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;

}
