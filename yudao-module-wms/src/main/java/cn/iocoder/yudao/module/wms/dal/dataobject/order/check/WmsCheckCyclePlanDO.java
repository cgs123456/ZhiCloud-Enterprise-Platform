package cn.iocoder.yudao.module.wms.dal.dataobject.order.check;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * WMS 循环盘点计划 DO
 *
 * <p>按仓库 + ABC 分类配置循环盘点周期（cycle_days）。
 * A 类默认 30 天 / B 类 60 天 / C 类 90 天。
 *
 * @author 芋道源码
 */
@TableName("wms_check_cycle_plan")
@KeySequence("wms_check_cycle_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsCheckCyclePlanDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 仓库编号
     *
     * 关联 {@link WmsWarehouseDO#getId()}
     */
    private Long warehouseId;
    /**
     * ABC 分类（A/B/C）
     */
    private String abcClassification;
    /**
     * 循环周期天数
     */
    private Integer cycleDays;
    /**
     * 下次盘点日期（Job 计算后回写）
     */
    private LocalDate nextCheckDate;
    /**
     * 是否启用（0 停用 1 启用）
     */
    private Integer enabled;
    /**
     * 备注
     */
    private String remark;

}