package cn.zhicloud.module.mes.dal.dataobject.dv.scada;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * MES SCADA 设备配置 DO
 *
 * <p>将 MES 设备（{@link MesDvMachineryDO}）映射到 IoT 平台设备（{@code iotDevicePk} + {@code iotProductId}），
 * 并配置接入协议（MQTT / Modbus TCP / OPC-UA）与点位映射（{@code pointConfig} JSON）。
 *
 * @author 智云
 */
@TableName("mes_dv_scada_config")
@KeySequence("mes_dv_scada_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesDvScadaConfigDO extends BaseDO {

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
     * IoT 平台设备 PK（devicePk，唯一编码）
     */
    private String iotDevicePk;
    /**
     * IoT 平台产品编号（productId）
     */
    private Long iotProductId;
    /**
     * SCADA 协议类型
     *
     * 字典 mes_dv_scada_protocol_type
     * 枚举 {@link cn.zhicloud.module.mes.enums.dv.MesDvScadaProtocolTypeEnum}
     * 取值：MQTT / MODBUS_TCP / OPC-UA
     */
    private String protocolType;
    /**
     * 点位映射配置（JSON）
     *
     * <p>描述 MES 属性名 ↔ IoT 物模型标识 / Modbus 寄存器地址 / OPC-UA NodeId 的映射关系。
     */
    private String pointConfig;
    /**
     * 是否启用
     *
     * 字典 {@link cn.zhicloud.framework.common.enums.CommonStatusEnum}
     */
    private Integer enabled;
    /**
     * 备注
     */
    private String remark;

}
