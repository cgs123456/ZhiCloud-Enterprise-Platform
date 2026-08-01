package cn.iocoder.yudao.module.tms.service.vehicle;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.tms.controller.admin.vehicle.vo.TmsFleetOperationPageReqVO;
import cn.iocoder.yudao.module.tms.controller.admin.vehicle.vo.TmsFleetOperationSaveReqVO;
import cn.iocoder.yudao.module.tms.dal.dataobject.vehicle.TmsFleetOperationDO;
import cn.iocoder.yudao.module.tms.dal.mysql.vehicle.TmsFleetOperationMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.tms.enums.ErrorCodeConstants.*;

/**
 * TMS 车队运营 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class TmsFleetOperationServiceImpl implements TmsFleetOperationService {

    @Resource
    private TmsFleetOperationMapper fleetOperationMapper;

    @Override
    public Long createFleetOperation(TmsFleetOperationSaveReqVO createReqVO) {
        TmsFleetOperationDO fleetOperation = BeanUtils.toBean(createReqVO, TmsFleetOperationDO.class);
        calculateTotals(fleetOperation);
        fleetOperationMapper.insert(fleetOperation);
        return fleetOperation.getId();
    }

    @Override
    public void updateFleetOperation(TmsFleetOperationSaveReqVO updateReqVO) {
        validateFleetOperationExists(updateReqVO.getId());
        TmsFleetOperationDO updateObj = BeanUtils.toBean(updateReqVO, TmsFleetOperationDO.class);
        calculateTotals(updateObj);
        fleetOperationMapper.updateById(updateObj);
    }

    @Override
    public void deleteFleetOperation(Long id) {
        validateFleetOperationExists(id);
        fleetOperationMapper.deleteById(id);
    }

    @Override
    public TmsFleetOperationDO getFleetOperation(Long id) {
        return fleetOperationMapper.selectById(id);
    }

    @Override
    public PageResult<TmsFleetOperationDO> getFleetOperationPage(TmsFleetOperationPageReqVO pageReqVO) {
        return fleetOperationMapper.selectPage(pageReqVO);
    }

    /**
     * 计算总成本和利润
     * 总成本 = 油费 + 维修保养费 + 保险费 + 年检费
     * 利润 = 收入 - 总成本
     */
    private void calculateTotals(TmsFleetOperationDO operation) {
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal fuelCost = operation.getFuelCost() != null ? operation.getFuelCost() : zero;
        BigDecimal maintenanceCost = operation.getMaintenanceCost() != null ? operation.getMaintenanceCost() : zero;
        BigDecimal insuranceCost = operation.getInsuranceCost() != null ? operation.getInsuranceCost() : zero;
        BigDecimal inspectionCost = operation.getInspectionCost() != null ? operation.getInspectionCost() : zero;

        BigDecimal totalCost = fuelCost.add(maintenanceCost).add(insuranceCost).add(inspectionCost);
        BigDecimal revenue = operation.getRevenue() != null ? operation.getRevenue() : zero;
        BigDecimal profit = revenue.subtract(totalCost);

        operation.setTotalCost(totalCost);
        operation.setProfit(profit);
    }

    private TmsFleetOperationDO validateFleetOperationExists(Long id) {
        TmsFleetOperationDO operation = fleetOperationMapper.selectById(id);
        if (operation == null) {
            throw exception(TMS_FLEET_OPERATION_NOT_EXISTS);
        }
        return operation;
    }

}
