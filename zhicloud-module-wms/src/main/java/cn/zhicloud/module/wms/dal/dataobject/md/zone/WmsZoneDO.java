package cn.zhicloud.module.wms.dal.dataobject.md.zone;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * WMS 库区 DO
 *
 * @author 智云
 */
@TableName("wms_zone")
@KeySequence("wms_zone_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsZoneDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 仓库 ID
     */
    private Long warehouseId;
    /**
     * 库区编号
     */
    private String code;
    /**
     * 库区名称
     */
    private String name;
    /**
     * 库区类型
     *
     * 枚举 {@link cn.zhicloud.module.wms.enums.md.WmsZoneTypeEnum}
     */
    private Integer type;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
