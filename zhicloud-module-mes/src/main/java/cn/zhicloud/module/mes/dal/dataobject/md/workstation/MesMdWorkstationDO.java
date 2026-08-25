package cn.zhicloud.module.mes.dal.dataobject.md.workstation;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * MES 工作站 DO
 *
 * @author 智云
 */
@TableName("mes_md_workstation")
@KeySequence("mes_md_workstation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesMdWorkstationDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 工作站编码
     */
    private String code;
    /**
     * 工作站名称
     */
    private String name;
    /**
     * 工作站地点
     */
    private String address;
    /**
     * 所在车间编号
     *
     * 关联 {@link MesMdWorkshopDO#getId()}
     */
    private Long workshopId;
    /**
     * 工序编号
     *
     * 关联 {@link MesProProcessDO#getId()}
     */
    private Long processId;
    /**
     * 线边库编号
     *
     * 关联 {@link MesWmWarehouseDO#getId()}
     */
    private Long warehouseId;
    /**
     * 库区编号
     *
     * 关联 {@link MesWmWarehouseLocationDO#getId()}
     */
    private Long locationId;
    /**
     * 库位编号
     *
     * 关联 {@link MesWmWarehouseAreaDO#getId()}
     */
    private Long areaId;
    /**
     * 状态
     *
     * 枚举 {@link cn.zhicloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer status;
    /**
     * 单位时间产能（件/小时，APS 排产用时估算用）
     *
     * 为空时 APS 排产回退到 1 件/小时 的简化估算
     */
    private BigDecimal capacity;
    /**
     * 效率系数（0~2，默认 1.0，APS 排产时长按此系数折算）
     *
     * 1.0 = 标准效率；&lt;1 = 老化/降额；&gt;1 = 增强
     */
    private BigDecimal efficiency;
    /**
     * 备注
     */
    private String remark;

}
