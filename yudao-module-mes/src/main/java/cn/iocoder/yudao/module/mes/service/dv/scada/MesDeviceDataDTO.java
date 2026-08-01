package cn.iocoder.yudao.module.mes.service.dv.scada;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * MES 设备实时数据 DTO（来源于 IoT 模块 MQTT/Modbus 采集）
 *
 * <p>字段对齐 {@code yudao-module-iot} 上报的设备属性数据。
 * 用于 SCADA 集成：MES 侧缓存最新一帧数据，供 OEE 实时聚合、看板推送使用。
 *
 * @author 芋道源码
 */
@Data
public class MesDeviceDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * MES 设备编号（关联 {@code MesDvMachineryDO#getId()}）
     */
    private Long machineryId;
    /**
     * 数据采集时间戳
     */
    private LocalDateTime timestamp;
    /**
     * 属性名称（如 temperature / pressure / runStatus / counter）
     */
    private String propertyName;
    /**
     * 属性值（字符串形式，使用方按 dataType 自行转换）
     */
    private String propertyValue;
    /**
     * 数据类型：BOOL / INT / FLOAT / STRING
     */
    private String dataType;

}
