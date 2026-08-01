package cn.iocoder.yudao.module.mes.service.dv.gateway;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.dv.repair.vo.MesDvRepairPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.repair.MesDvRepairDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpKpiDO;
import cn.iocoder.yudao.module.mes.service.dv.repair.MesDvRepairService;
import cn.iocoder.yudao.module.mes.service.dv.tp.MesDvTpKpiService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备维护数据 SPI 网关实现
 *
 * <p>实现 AI 模块的 {@link DeviceMaintenanceDataGateway} 接口，向预测性维护模块提供
 * MES 侧的设备维修记录与 TPM KPI（MTBF/MTTR）数据。
 *
 * <p>数据来源：
 * <ul>
 *   <li>{@link MesDvRepairService}：维修工单（lastRepairTime / repairCount）</li>
 *   <li>{@link MesDvTpKpiService}：TPM KPI 指标（MTBF / MTTR 历史）</li>
 * </ul>
 *
 * <p>依赖说明：MES 模块的 pom.xml 中 {@code yudao-module-ai} 为 optional 依赖，
 * AI 侧通过 {@code @Autowired(required=false)} 注入本接口。当 MES 模块未加载时，
 * AI 侧预测性维护功能降级为不可用。
 *
 * @author 芋道源码
 */
@Component
@Slf4j
public class MesDeviceMaintenanceDataGatewayImpl implements DeviceMaintenanceDataGateway {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Resource
    private MesDvRepairService repairService;
    @Resource
    private MesDvTpKpiService tpKpiService;

    @Override
    public LocalDateTime getLastRepairTime(Long deviceId) {
        if (deviceId == null) {
            return null;
        }
        // 通过分页查询取最新一条维修工单（默认按 id 倒序，即最新优先）
        MesDvRepairPageReqVO pageReqVO = new MesDvRepairPageReqVO();
        pageReqVO.setMachineryId(deviceId);
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(1);
        PageResult<MesDvRepairDO> page = repairService.getRepairPage(pageReqVO);
        if (page.getList().isEmpty()) {
            return null;
        }
        MesDvRepairDO repair = page.getList().get(0);
        // 优先返回维修完成日期，其次验收日期，最后报修日期
        if (repair.getFinishDate() != null) {
            return repair.getFinishDate();
        }
        if (repair.getConfirmDate() != null) {
            return repair.getConfirmDate();
        }
        return repair.getRequireDate();
    }

    @Override
    public Long getRepairCount(Long deviceId) {
        if (deviceId == null) {
            return 0L;
        }
        return repairService.getRepairCountByMachineryId(deviceId);
    }

    @Override
    public List<KpiSnapshot> getKpiHistory(Long deviceId, int limit) {
        if (deviceId == null || limit <= 0) {
            return Collections.emptyList();
        }
        // 生成最近 limit 个月的周期列表（升序，从最早到最近）
        List<String> periods = new ArrayList<>(limit);
        LocalDate base = LocalDate.now();
        for (int i = limit - 1; i >= 0; i--) {
            periods.add(base.minusMonths(i).format(PERIOD_FORMATTER));
        }
        List<MesDvTpKpiDO> kpis = tpKpiService.getEquipmentKpiHistory(deviceId, periods);
        if (kpis == null || kpis.isEmpty()) {
            return Collections.emptyList();
        }
        return kpis.stream()
                .map(k -> new KpiSnapshot(k.getPeriod(), k.getMtbf(), k.getMttr()))
                .collect(Collectors.toList());
    }

    @Override
    public KpiSnapshot getLatestKpi(Long deviceId) {
        if (deviceId == null) {
            return null;
        }
        // 优先查询当月 KPI，当月无数据时回退到上月
        String currentPeriod = LocalDate.now().format(PERIOD_FORMATTER);
        KpiSnapshot snapshot = queryKpiSnapshot(deviceId, currentPeriod);
        if (snapshot != null) {
            return snapshot;
        }
        String prevPeriod = LocalDate.now().minusMonths(1).format(PERIOD_FORMATTER);
        return queryKpiSnapshot(deviceId, prevPeriod);
    }

    private KpiSnapshot queryKpiSnapshot(Long deviceId, String period) {
        List<MesDvTpKpiDO> list = tpKpiService.getEquipmentKpiHistory(deviceId, List.of(period));
        if (list == null || list.isEmpty()) {
            return null;
        }
        MesDvTpKpiDO kpi = list.get(0);
        return new KpiSnapshot(kpi.getPeriod(), kpi.getMtbf(), kpi.getMttr());
    }

}
