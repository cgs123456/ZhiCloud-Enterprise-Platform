package cn.iocoder.yudao.module.wms.dal.dataobject.order.dock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * WMS 月台 DO
 *
 * <p>用于收发货月台管理，关联 ASN 到货通知与收发货作业。
 *
 * @author 芋道源码
 */
@TableName("wms_dock")
@KeySequence("wms_dock_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsDockDO extends BaseDO {

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
     * 月台编号
     */
    private String dockCode;
    /**
     * 月台名称
     */
    private String dockName;
    /**
     * 月台类型
     *
     * 枚举：10 收货月台 / 20 发货月台 / 30 越库月台
     */
    private Integer dockType;
    /**
     * 状态
     *
     * 枚举：10 空闲 / 20 占用 / 30 维修中
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
