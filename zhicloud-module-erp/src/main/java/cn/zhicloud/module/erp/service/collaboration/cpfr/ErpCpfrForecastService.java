package cn.zhicloud.module.erp.service.collaboration.cpfr;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrForecastPageReqVO;
import cn.zhicloud.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrForecastSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.collaboration.cpfr.ErpCpfrForecastDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP CPFR 联合计划预测补货 Service 接口
 *
 * @author 智云
 */
public interface ErpCpfrForecastService {

    /**
     * 创建预测
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createForecast(@Valid ErpCpfrForecastSaveReqVO createReqVO);

    /**
     * 更新预测
     *
     * @param updateReqVO 更新信息
     */
    void updateForecast(@Valid ErpCpfrForecastSaveReqVO updateReqVO);

    /**
     * 删除预测
     *
     * @param id 编号
     */
    void deleteForecast(Long id);

    /**
     * 获得预测
     *
     * @param id 编号
     * @return 预测
     */
    ErpCpfrForecastDO getForecast(Long id);

    /**
     * 获得预测分页
     *
     * @param pageReqVO 分页查询
     * @return 预测分页
     */
    PageResult<ErpCpfrForecastDO> getForecastPage(ErpCpfrForecastPageReqVO pageReqVO);

    /**
     * 计算偏差率：deviationRate = (forecastQuantity - actualQuantity) / forecastQuantity
     *
     * @param id 预测编号
     */
    void calculateDeviation(Long id);

    /**
     * 检查异常：扫描所有预测，当偏差率超过阈值时创建异常记录
     *
     * @return 创建的异常记录编号列表
     */
    List<Long> checkException();

}
