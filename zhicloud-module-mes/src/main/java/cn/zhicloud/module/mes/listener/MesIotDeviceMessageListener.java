package cn.zhicloud.module.mes.listener;

import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.dv.scada.MesDvScadaConfigDO;
import cn.zhicloud.module.mes.service.dv.scada.MesDeviceDataDTO;
import cn.zhicloud.module.mes.service.dv.scada.MesDvDeviceDataServiceImpl;
import cn.zhicloud.module.mes.service.dv.scada.MesDvScadaConfigService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * IoT 设备消息监听器
 *
 * <p>监听 {@link MesIotDeviceMessageEvent}，根据 {@code iotDevicePk} 反查 MES 设备编号，
 * 将 IoT 上报的属性数据转换为 {@link MesDeviceDataDTO} 后写入实时数据缓存。
 *
 * <p>当前 IoT 模块未启用时，本监听器不会被触发；IoT 启用后由 IoT 适配层发布事件即可生效。
 *
 * @author 智云
 */
@Component
@Slf4j
public class MesIotDeviceMessageListener {

    @Resource
    private MesDvScadaConfigService scadaConfigService;
    @Resource
    private MesDvDeviceDataServiceImpl deviceDataService;

    @EventListener
    public void onIotDeviceMessage(MesIotDeviceMessageEvent event) {
        if (event == null || event.getIotDevicePk() == null) {
            return;
        }
        // 1. 根据 iotDevicePk 查询 MES 侧 SCADA 配置（iotDevicePk 唯一性由业务保证）
        MesDvScadaConfigDO config = resolveByIotDevicePk(event.getIotDevicePk());
        if (config == null) {
            log.debug("[onIotDeviceMessage][未找到 iotDevicePk={} 对应的 SCADA 配置，忽略]", event.getIotDevicePk());
            return;
        }
        // 2. 将每个属性转换为独立的 DTO 并更新缓存
        Map<String, String> properties = event.getProperties();
        if (properties == null || properties.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            MesDeviceDataDTO dto = new MesDeviceDataDTO();
            dto.setMachineryId(config.getMachineryId());
            dto.setTimestamp(event.getTimestamp());
            dto.setPropertyName(entry.getKey());
            dto.setPropertyValue(entry.getValue());
            dto.setDataType(inferDataType(entry.getKey()));
            deviceDataService.onDeviceData(dto);
        }
    }

    /**
     * 通过 iotDevicePk 反查 SCADA 配置
     *
     * <p>使用分页接口按 iotDevicePk 精确匹配。设备规模可控（工厂级设备数有限），
     * 不再单独新增 Mapper 方法以减少改动面。
     */
    private MesDvScadaConfigDO resolveByIotDevicePk(String iotDevicePk) {
        MesDvScadaConfigPageReqVO pageReqVO = new MesDvScadaConfigPageReqVO();
        pageReqVO.setIotDevicePk(iotDevicePk);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE); // 不分页，取全部
        PageResult<MesDvScadaConfigDO> page = scadaConfigService.getScadaConfigPage(pageReqVO);
        if (page.getList().isEmpty()) {
            return null;
        }
        return page.getList().get(0);
    }

    /**
     * 根据属性名简单推断数据类型
     */
    private String inferDataType(String propertyName) {
        if (propertyName == null) {
            return "STRING";
        }
        String lower = propertyName.toLowerCase();
        if (lower.contains("status") || lower.contains("flag") || lower.contains("running")) {
            return "BOOL";
        }
        if (lower.contains("count") || lower.contains("qty") || lower.contains("counter")) {
            return "INT";
        }
        if (lower.contains("temp") || lower.contains("pressure") || lower.contains("speed")
                || lower.contains("voltage") || lower.contains("current")) {
            return "FLOAT";
        }
        return "STRING";
    }

}
