package cn.zhicloud.module.mes.enums.dv;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES SCADA 协议类型枚举
 *
 * <p>对齐 {@code zhicloud-module-iot} 的 MQTT / Modbus / OPC-UA 接入能力。
 * 字典：{@code mes_dv_scada_protocol_type}
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum MesDvScadaProtocolTypeEnum {

    MQTT("MQTT", "MQTT 协议"),
    MODBUS_TCP("MODBUS_TCP", "Modbus TCP 协议"),
    OPC_UA("OPC-UA", "OPC-UA 协议");

    public static final String[] ARRAYS = Arrays.stream(values()).map(MesDvScadaProtocolTypeEnum::getCode).toArray(String[]::new);

    /**
     * 协议编码
     */
    private final String code;
    /**
     * 协议名称
     */
    private final String name;

    /**
     * 判断 code 是否为合法的协议类型
     */
    public static boolean isValid(String code) {
        if (code == null) {
            return false;
        }
        return Arrays.stream(values()).anyMatch(e -> e.getCode().equals(code));
    }

}
