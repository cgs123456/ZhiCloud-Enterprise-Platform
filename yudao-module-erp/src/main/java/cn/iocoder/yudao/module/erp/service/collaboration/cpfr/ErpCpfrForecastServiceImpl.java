package cn.iocoder.yudao.module.erp.service.collaboration.cpfr;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrForecastPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrForecastSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.collaboration.cpfr.ErpCpfrExceptionDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.collaboration.cpfr.ErpCpfrForecastDO;
import cn.iocoder.yudao.module.erp.dal.mysql.collaboration.cpfr.ErpCpfrExceptionMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.collaboration.cpfr.ErpCpfrForecastMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CPFR_FORECAST_NOT_EXISTS;

/**
 * ERP CPFR 联合计划预测补货 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpCpfrForecastServiceImpl implements ErpCpfrForecastService {

    /**
     * 异常类型：预测偏差超限
     */
    private static final int EXCEPTION_TYPE_DEVIATION = 10;
    /**
     * 偏差率阈值（绝对值），超过则视为异常
     */
    private static final BigDecimal DEVIATION_THRESHOLD = new BigDecimal("0.20");

    @Resource
    private ErpCpfrForecastMapper cpfrForecastMapper;
    @Resource
    private ErpCpfrExceptionMapper cpfrExceptionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createForecast(ErpCpfrForecastSaveReqVO createReqVO) {
        ErpCpfrForecastDO forecast = BeanUtils.toBean(createReqVO, ErpCpfrForecastDO.class);
        cpfrForecastMapper.insert(forecast);
        return forecast.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateForecast(ErpCpfrForecastSaveReqVO updateReqVO) {
        validateForecastExists(updateReqVO.getId());
        ErpCpfrForecastDO updateObj = BeanUtils.toBean(updateReqVO, ErpCpfrForecastDO.class);
        cpfrForecastMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteForecast(Long id) {
        validateForecastExists(id);
        cpfrForecastMapper.deleteById(id);
    }

    private void validateForecastExists(Long id) {
        if (cpfrForecastMapper.selectById(id) == null) {
            throw exception(CPFR_FORECAST_NOT_EXISTS);
        }
    }

    @Override
    public ErpCpfrForecastDO getForecast(Long id) {
        return cpfrForecastMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCpfrForecastDO> getForecastPage(ErpCpfrForecastPageReqVO pageReqVO) {
        return cpfrForecastMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateDeviation(Long id) {
        ErpCpfrForecastDO forecast = cpfrForecastMapper.selectById(id);
        if (forecast == null) {
            throw exception(CPFR_FORECAST_NOT_EXISTS);
        }
        BigDecimal forecastQuantity = forecast.getForecastQuantity();
        BigDecimal actualQuantity = forecast.getActualQuantity();
        if (forecastQuantity == null || forecastQuantity.compareTo(BigDecimal.ZERO) == 0
                || actualQuantity == null) {
            return;
        }
        // deviationRate = (forecastQuantity - actualQuantity) / forecastQuantity
        BigDecimal deviationRate = forecastQuantity.subtract(actualQuantity)
                .divide(forecastQuantity, 4, RoundingMode.HALF_UP);
        ErpCpfrForecastDO updateObj = new ErpCpfrForecastDO();
        updateObj.setId(id);
        updateObj.setDeviationRate(deviationRate);
        cpfrForecastMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> checkException() {
        List<ErpCpfrForecastDO> forecastList = cpfrForecastMapper.selectList(null);
        List<Long> result = new ArrayList<>();
        for (ErpCpfrForecastDO forecast : forecastList) {
            // 先计算偏差率
            BigDecimal forecastQuantity = forecast.getForecastQuantity();
            BigDecimal actualQuantity = forecast.getActualQuantity();
            if (forecastQuantity == null || forecastQuantity.compareTo(BigDecimal.ZERO) == 0
                    || actualQuantity == null) {
                continue;
            }
            BigDecimal deviationRate = forecastQuantity.subtract(actualQuantity)
                    .divide(forecastQuantity, 4, RoundingMode.HALF_UP);
            BigDecimal absDeviation = deviationRate.abs();
            // 偏差率超过阈值，创建异常记录
            if (absDeviation.compareTo(DEVIATION_THRESHOLD) > 0) {
                // 同步更新偏差率
                ErpCpfrForecastDO updateObj = new ErpCpfrForecastDO();
                updateObj.setId(forecast.getId());
                updateObj.setDeviationRate(deviationRate);
                cpfrForecastMapper.updateById(updateObj);
                // 创建异常记录
                ErpCpfrExceptionDO exceptionDO = ErpCpfrExceptionDO.builder()
                        .forecastId(forecast.getId())
                        .exceptionType(EXCEPTION_TYPE_DEVIATION)
                        .exceptionDescription("预测偏差率 " + absDeviation.multiply(new BigDecimal("100"))
                                + "% 超过阈值 20%")
                        .handlingStatus(10)
                        .build();
                cpfrExceptionMapper.insert(exceptionDO);
                result.add(exceptionDO.getId());
            }
        }
        return result;
    }

}
