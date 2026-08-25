package cn.zhicloud.module.mes.dal.dataobject.pro.workorder;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.md.client.MesMdClientDO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.dal.dataobject.md.vendor.MesMdVendorDO;
import cn.zhicloud.module.mes.enums.pro.MesProApsPlanPriorityEnum;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderSourceTypeEnum;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import cn.zhicloud.module.mes.enums.DictTypeConstants;

/**
 * MES 生产工单 DO
 *
 * @author 智云
 */
@TableName("mes_pro_work_order")
@KeySequence("mes_pro_work_order_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProWorkOrderDO extends BaseDO {

    /**
     * 父工单编号，空值
     */
    public static final Long PARENT_ID_NULL = 0L;

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 乐观锁版本号（P2：@Version 并发保护）
     */
    @Version
    private Long version;
    /**
     * 工单编码
     */
    private String code;
    /**
     * 工单名称
     */
    private String name;
    /**
     * 工单类型
     *
     * 字典 {@link DictTypeConstants#MES_PRO_WORK_ORDER_TYPE}
     * 枚举 {@link MesProWorkOrderTypeEnum}
     */
    private Integer type;
    /**
     * 来源类型
     *
     * 字典 {@link DictTypeConstants#MES_PRO_WORK_ORDER_SOURCE_TYPE}
     * 枚举 {@link MesProWorkOrderSourceTypeEnum}
     */
    private Integer orderSourceType;
    /**
     * 来源单据编号
     */
    private String orderSourceCode;
    /**
     * 产品编号
     *
     * 关联 {@link MesMdItemDO#getId()}
     */
    private Long productId;
    /**
     * 生产数量
     */
    private BigDecimal quantity;
    /**
     * 已生产数量
     */
    private BigDecimal quantityProduced;
    /**
     * 调整数量
     */
    private BigDecimal quantityChanged;
    /**
     * 已排产数量
     */
    private BigDecimal quantityScheduled;
    /**
     * 客户编号
     *
     * 关联 {@link MesMdClientDO#getId()}
     */
    private Long clientId;
    /**
     * 供应商编号
     *
     * 关联 {@link MesMdVendorDO#getId()}
     */
    private Long vendorId;
    /**
     * 批次号
     */
    private String batchCode;
    /**
     * 需求日期
     */
    private LocalDateTime requestDate;
    /**
     * 父工单编号
     *
     * 关联 {@link MesProWorkOrderDO#getId()}
     */
    private Long parentId;
    /**
     * 完成时间
     */
    private LocalDateTime finishDate;
    /**
     * 取消时间
     */
    private LocalDateTime cancelDate;
    /**
     * 工单状态
     *
     * 字典 {@link DictTypeConstants#MES_PRO_WORK_ORDER_STATUS}
     * 枚举 {@link MesProWorkOrderStatusEnum}
     */
    private Integer status;
    /**
     * 工单优先级（APS 排产排序用）
     *
     * 枚举 {@link MesProApsPlanPriorityEnum}：1=高 / 2=中（默认）/ 3=低
     * 为空时 APS 排产默认按中优先级处理
     */
    private Integer priority;
    /**
     * 备注
     */
    private String remark;

}
