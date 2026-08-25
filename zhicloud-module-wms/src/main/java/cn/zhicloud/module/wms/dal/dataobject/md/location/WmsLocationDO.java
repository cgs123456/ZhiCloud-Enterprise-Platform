package cn.zhicloud.module.wms.dal.dataobject.md.location;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * WMS 库位 DO
 *
 * @author 智云
 */
@TableName("wms_location")
@KeySequence("wms_location_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsLocationDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 库区 ID
     */
    private Long zoneId;
    /**
     * 仓库 ID（冗余，便于查询）
     */
    private Long warehouseId;
    /**
     * 库位编号
     */
    private String code;
    /**
     * 库位名称
     */
    private String name;
    /**
     * 库位条码
     */
    private String barcode;
    /**
     * 库位类型
     *
     * 枚举 {@link cn.zhicloud.module.wms.enums.md.WmsLocationTypeEnum}
     */
    private Integer type;
    /**
     * 承重容量（kg）
     */
    private BigDecimal capacityWeight;
    /**
     * 容积容量（m³）
     */
    private BigDecimal capacityVolume;
    /**
     * 状态
     *
     * 枚举 {@link cn.zhicloud.module.wms.enums.md.WmsLocationStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
