package cn.iocoder.yudao.module.mes.service.energy;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.energy.vo.MesEnergyConsumptionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.energy.vo.MesEnergyConsumptionSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.energy.MesEnergyConsumptionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.energy.MesEnergyConsumptionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 能源消耗 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MesEnergyConsumptionServiceImpl implements MesEnergyConsumptionService {

    @Resource
    private MesEnergyConsumptionMapper energyConsumptionMapper;

    @Override
    public Long createEnergyConsumption(MesEnergyConsumptionSaveReqVO createReqVO) {
        MesEnergyConsumptionDO energyConsumption = BeanUtils.toBean(createReqVO, MesEnergyConsumptionDO.class);
        // 自动计算总金额 = 消耗量 × 单价
        calculateTotalAmount(energyConsumption);
        energyConsumptionMapper.insert(energyConsumption);
        return energyConsumption.getId();
    }

    @Override
    public void updateEnergyConsumption(MesEnergyConsumptionSaveReqVO updateReqVO) {
        validateEnergyConsumptionExists(updateReqVO.getId());
        MesEnergyConsumptionDO updateObj = BeanUtils.toBean(updateReqVO, MesEnergyConsumptionDO.class);
        calculateTotalAmount(updateObj);
        energyConsumptionMapper.updateById(updateObj);
    }

    @Override
    public void deleteEnergyConsumption(Long id) {
        validateEnergyConsumptionExists(id);
        energyConsumptionMapper.deleteById(id);
    }

    @Override
    public MesEnergyConsumptionDO getEnergyConsumption(Long id) {
        return energyConsumptionMapper.selectById(id);
    }

    @Override
    public PageResult<MesEnergyConsumptionDO> getEnergyConsumptionPage(MesEnergyConsumptionPageReqVO pageReqVO) {
        return energyConsumptionMapper.selectPage(pageReqVO);
    }

    @Override
    public Map<Integer, BigDecimal> getEnergySummaryByWorkshop(Long workshopId, LocalDate startDate, LocalDate endDate) {
        List<MesEnergyConsumptionDO> list = energyConsumptionMapper
                .selectListByWorkshopAndDateRange(workshopId, startDate, endDate);
        // 按能源类型分组汇总
        Map<Integer, BigDecimal> summary = new LinkedHashMap<>();
        for (MesEnergyConsumptionDO item : list) {
            summary.merge(item.getEnergyType(), item.getConsumption(), BigDecimal::add);
        }
        return summary;
    }

    @Override
    public List<MesEnergyConsumptionDO> getEnergyConsumptionList(Long workshopId, LocalDate startDate, LocalDate endDate) {
        return energyConsumptionMapper.selectListByWorkshopAndDateRange(workshopId, startDate, endDate);
    }

    private void validateEnergyConsumptionExists(Long id) {
        if (energyConsumptionMapper.selectById(id) == null) {
            throw exception(MES_ENERGY_CONSUMPTION_NOT_EXISTS);
        }
    }

    /**
     * 自动计算总金额 = 消耗量 × 单价
     */
    private void calculateTotalAmount(MesEnergyConsumptionDO energyConsumption) {
        if (energyConsumption.getConsumption() != null && energyConsumption.getUnitPrice() != null) {
            energyConsumption.setTotalAmount(
                    energyConsumption.getConsumption().multiply(energyConsumption.getUnitPrice()));
        }
    }

}
