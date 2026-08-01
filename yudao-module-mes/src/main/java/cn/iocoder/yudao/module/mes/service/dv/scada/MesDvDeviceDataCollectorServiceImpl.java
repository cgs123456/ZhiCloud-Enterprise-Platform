package cn.iocoder.yudao.module.mes.service.dv.scada;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.scada.MesDvDeviceDataRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.scada.MesDvScadaConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.scada.MesDvScadaConfigMapper;
import cn.iocoder.yudao.module.mes.enums.dv.MesDvScadaProtocolTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_DEVICE_DATA_COLLECT_CONFIG_DISABLED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_DEVICE_DATA_COLLECT_CONFIG_NOT_EXISTS;

/**
 * MES 设备数据采集器 Service 实现类
 *
 * <p>实现策略：
 * <ul>
 *   <li><b>MQTT</b>：通过 Redis Pub/Sub 或 Spring Event 监听 IoT 模块广播的设备消息；
 *       当前 IoT 模块未启用，{@code iotApi} 为 null 时降级为 stub 返回模拟数据</li>
 *   <li><b>MODBUS_TCP</b>：通过 Modbus4j 读取寄存器；依赖为 optional，未启用时降级为 stub</li>
 *   <li><b>OPC-UA</b>：通过 Eclipse Milo 客户端读取节点；依赖为 optional，未启用时降级为 stub</li>
 * </ul>
 *
 * <p>容错：所有可选依赖通过 {@code @Autowired(required=false)} 注入，缺失时返回模拟数据，
 * 不影响 MES 主流程。
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class MesDvDeviceDataCollectorServiceImpl implements MesDvDeviceDataCollectorService {

    /**
     * 数据类型：数字
     */
    private static final int DATA_TYPE_NUMBER = 10;
    /**
     * 数据类型：布尔
     */
    private static final int DATA_TYPE_BOOLEAN = 20;
    /**
     * 数据类型：字符串
     */
    private static final int DATA_TYPE_STRING = 30;

    /**
     * 采集状态：正常
     */
    private static final int STATUS_NORMAL = 10;
    /**
     * 采集状态：异常
     */
    private static final int STATUS_ABNORMAL = 20;

    /**
     * IoT 模块对外 API（可选）
     *
     * <p>当前 {@code yudao-module-iot} 未启用，此处为 null。
     * IoT 启用后可通过该 API 查询 MQTT 上报数据。
     */
    @Autowired(required = false)
    private Object iotApi;

    /**
     * Modbus4j Master 客户端（可选）
     *
     * <p>未引入 {@code com.ghgande:j2mod} 或 {@code com.infiniteautomation:modbus4j} 时为 null。
     */
    @Autowired(required = false)
    private Object modbusMaster;

    /**
     * Eclipse Milo OPC-UA Client（可选）
     *
     * <p>未引入 {@code org.eclipse.milo:sdk-client} 时为 null。
     */
    @Autowired(required = false)
    private Object opcUaClient;

    @Resource
    private MesDvScadaConfigMapper scadaConfigMapper;
    @Resource
    private MesDvDeviceDataRecordService deviceDataRecordService;

    /**
     * 自动采集开关（true=运行中）
     */
    private final AtomicBoolean autoCollecting = new AtomicBoolean(false);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MesDvDeviceDataRecordDO> collectDeviceData(Long machineryId) {
        if (machineryId == null) {
            return Collections.emptyList();
        }
        // 1. 获取 SCADA 配置
        MesDvScadaConfigDO config = scadaConfigMapper.selectByMachineryId(machineryId);
        if (config == null) {
            throw exception(DV_DEVICE_DATA_COLLECT_CONFIG_NOT_EXISTS);
        }
        if (!CommonStatusEnum.ENABLE.getStatus().equals(config.getEnabled())) {
            throw exception(DV_DEVICE_DATA_COLLECT_CONFIG_DISABLED);
        }

        // 2. 按协议类型分派采集
        LocalDateTime collectTime = LocalDateTime.now();
        List<MesDvDeviceDataRecordDO> records;
        try {
            records = doCollect(config, collectTime);
        } catch (Exception e) {
            log.warn("[collectDeviceData][采集异常] machineryId={}, protocolType={}",
                    machineryId, config.getProtocolType(), e);
            // 异常时记录一条异常状态记录
            records = new ArrayList<>();
            records.add(buildErrorRecord(config, collectTime, "采集异常：" + e.getMessage()));
        }

        // 3. 落库
        deviceDataRecordService.saveRecords(records);
        return records;
    }

    @Override
    public void startAutoCollection() {
        if (autoCollecting.compareAndSet(false, true)) {
            log.info("[startAutoCollection][自动采集已开启]");
        } else {
            log.warn("[startAutoCollection][自动采集已处于开启状态]");
        }
    }

    @Override
    public void stopAutoCollection() {
        if (autoCollecting.compareAndSet(true, false)) {
            log.info("[stopAutoCollection][自动采集已停止]");
        } else {
            log.warn("[stopAutoCollection][自动采集未开启]");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int collectAll() {
        if (!autoCollecting.get()) {
            log.debug("[collectAll][自动采集未开启，跳过]");
            return 0;
        }
        // 查询所有启用的 SCADA 配置
        List<MesDvScadaConfigDO> configs = scadaConfigMapper.selectList(
                MesDvScadaConfigDO::getEnabled, CommonStatusEnum.ENABLE.getStatus());
        if (CollUtil.isEmpty(configs)) {
            log.debug("[collectAll][无已启用的 SCADA 配置]");
            return 0;
        }
        int total = 0;
        for (MesDvScadaConfigDO config : configs) {
            try {
                List<MesDvDeviceDataRecordDO> records = collectDeviceData(config.getMachineryId());
                total += records.size();
            } catch (Exception e) {
                log.warn("[collectAll][设备 {} 采集失败]", config.getMachineryId(), e);
            }
        }
        log.info("[collectAll][本次采集完成，共 {} 条记录]", total);
        return total;
    }

    // ==================== 协议分派 ====================

    private List<MesDvDeviceDataRecordDO> doCollect(MesDvScadaConfigDO config, LocalDateTime collectTime) {
        String protocolType = config.getProtocolType();
        if (MesDvScadaProtocolTypeEnum.MQTT.getCode().equals(protocolType)) {
            return collectByMqtt(config, collectTime);
        }
        if (MesDvScadaProtocolTypeEnum.MODBUS_TCP.getCode().equals(protocolType)) {
            return collectByModbus(config, collectTime);
        }
        if (MesDvScadaProtocolTypeEnum.OPC_UA.getCode().equals(protocolType)) {
            return collectByOpcUa(config, collectTime);
        }
        log.warn("[doCollect][不支持的协议类型 {}]", protocolType);
        return Collections.emptyList();
    }

    /**
     * MQTT 采集：通过 IoT 模块 API 拉取最新一帧属性数据
     *
     * <p>当前 IoT 模块未启用（{@code iotApi} 为 null），降级为 stub 返回模拟数据。
     */
    private List<MesDvDeviceDataRecordDO> collectByMqtt(MesDvScadaConfigDO config, LocalDateTime collectTime) {
        if (iotApi == null) {
            log.debug("[collectByMqtt][IoT 模块未启用，降级为 stub] machineryId={}", config.getMachineryId());
            return buildStubRecords(config, collectTime);
        }
        // TODO IoT 模块启用后，调用 iotApi 查询设备最新属性并转换为 MesDvDeviceDataRecordDO
        return buildStubRecords(config, collectTime);
    }

    /**
     * Modbus TCP 采集：读取寄存器
     *
     * <p>当前 {@code modbusMaster} 为 null（modbus4j/j2mod 依赖未引入），降级为 stub 返回模拟数据。
     */
    private List<MesDvDeviceDataRecordDO> collectByModbus(MesDvScadaConfigDO config, LocalDateTime collectTime) {
        if (modbusMaster == null) {
            log.debug("[collectByModbus][Modbus4j 依赖未启用，降级为 stub] machineryId={}", config.getMachineryId());
            return buildStubRecords(config, collectTime);
        }
        // TODO 引入 Modbus4j 后，按 config.getPointConfig() 中的寄存器地址读取并转换
        return buildStubRecords(config, collectTime);
    }

    /**
     * OPC-UA 采集：读取节点
     *
     * <p>当前 {@code opcUaClient} 为 null（Eclipse Milo 依赖未引入），降级为 stub 返回模拟数据。
     */
    private List<MesDvDeviceDataRecordDO> collectByOpcUa(MesDvScadaConfigDO config, LocalDateTime collectTime) {
        if (opcUaClient == null) {
            log.debug("[collectByOpcUa][Eclipse Milo 依赖未启用，降级为 stub] machineryId={}", config.getMachineryId());
            return buildStubRecords(config, collectTime);
        }
        // TODO 引入 Eclipse Milo 后，按 config.getPointConfig() 中的 NodeId 读取并转换
        return buildStubRecords(config, collectTime);
    }

    // ==================== 工具方法 ====================

    /**
     * 构造 stub 模拟数据
     *
     * <p>当 IoT / Modbus4j / Milo 依赖未启用时使用，确保采集管道可联调。
     */
    private List<MesDvDeviceDataRecordDO> buildStubRecords(MesDvScadaConfigDO config, LocalDateTime collectTime) {
        List<MesDvDeviceDataRecordDO> records = new ArrayList<>(3);
        records.add(buildRecord(config, collectTime, "runStatus", "1", DATA_TYPE_BOOLEAN, STATUS_NORMAL));
        records.add(buildRecord(config, collectTime, "temperature", "36.5", DATA_TYPE_NUMBER, STATUS_NORMAL));
        records.add(buildRecord(config, collectTime, "counter", "1024", DATA_TYPE_NUMBER, STATUS_NORMAL));
        return records;
    }

    private MesDvDeviceDataRecordDO buildRecord(MesDvScadaConfigDO config, LocalDateTime collectTime,
                                                String propertyName, String value,
                                                int dataType, int status) {
        return MesDvDeviceDataRecordDO.builder()
                .machineryId(config.getMachineryId())
                .scadaConfigId(config.getId())
                .propertyName(propertyName)
                .propertyValue(value)
                .dataType(dataType)
                .collectTime(collectTime)
                .status(status)
                .remark(StrUtil.format("协议：{}", config.getProtocolType()))
                .build();
    }

    private MesDvDeviceDataRecordDO buildErrorRecord(MesDvScadaConfigDO config, LocalDateTime collectTime, String msg) {
        return MesDvDeviceDataRecordDO.builder()
                .machineryId(config.getMachineryId())
                .scadaConfigId(config.getId())
                .propertyName("error")
                .propertyValue(null)
                .dataType(DATA_TYPE_STRING)
                .collectTime(collectTime)
                .status(STATUS_ABNORMAL)
                .remark(msg)
                .build();
    }

}
