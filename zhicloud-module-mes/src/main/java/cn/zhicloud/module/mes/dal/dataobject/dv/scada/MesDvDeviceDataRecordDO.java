package cn.zhicloud.module.mes.dal.dataobject.dv.scada;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * MES 设备数据采集记录 DO
 *
 * <p>SCADA 实际采集到的设备属性数据，每条记录对应一个属性的一次采集结果。
 * 关联 {@link MesDvScadaConfigDO} 与 {@link MesDvMachineryDO}。
 *
 * @author 智云
 */
@TableName("mes_dv_device_data_record")
@KeySequence("mes_dv_device_data_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesDvDeviceDataRecordDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * MES 设备编号
     *
     * 关联 {@link MesDvMachineryDO#getId()}
     */
    private Long machineryId;
    /**
     * SCADA 配置编号
     *
     * 关联 {@link MesDvScadaConfigDO#getId()}
     */
    private Long scadaConfigId;
    /**
     * 属性名称（如 temperature / pressure / runStatus / counter）
     */
    private String propertyName;
    /**
     * 属性值（字符串形式，使用方按 dataType 自行转换）
     */
    private String propertyValue;
    /**
     * 数据类型
     *
     * 字典 mes_dv_device_data_type
     * 取值：10 数字 / 20 布尔 / 30 字符串
     */
    private Integer dataType;
    /**
     * 采集时间
     */
    private LocalDateTime collectTime;
    /**
     * 状态
     *
     * 字典 mes_dv_device_data_record_status
     * 取值：10 正常 / 20 异常
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
