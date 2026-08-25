package cn.zhicloud.module.iot.core.topic.topo;

import cn.zhicloud.module.iot.core.enums.IotDeviceMessageMethodEnum;
import cn.zhicloud.module.iot.core.topic.IotDeviceIdentity;
import lombok.Data;

import java.util.List;

/**
 * IoT 设备拓扑关系获取 Response DTO
 * <p>
 * 用于 {@link IotDeviceMessageMethodEnum#TOPO_GET} 响应
 *
 * @author 智云
 * @see <a href="https://help.aliyun.com/zh/marketplace/obtain-topological-relationship">阿里云 - 获取拓扑关系</a>
 */
@Data
public class IotDeviceTopoGetRespDTO {

    /**
     * 子设备列表
     */
    private List<IotDeviceIdentity> subDevices;

}
