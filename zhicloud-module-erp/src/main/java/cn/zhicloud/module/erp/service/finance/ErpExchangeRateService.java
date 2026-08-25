package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.vo.exchangerate.ErpExchangeRatePageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.exchangerate.ErpExchangeRateSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpExchangeRateDO;
import jakarta.validation.Valid;

import java.time.LocalDate;

/**
 * ERP 汇率 Service 接口
 *
 * @author 智云
 */
public interface ErpExchangeRateService {

    /**
     * 创建汇率
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createExchangeRate(@Valid ErpExchangeRateSaveReqVO createReqVO);

    /**
     * 更新汇率
     *
     * @param updateReqVO 更新信息
     */
    void updateExchangeRate(@Valid ErpExchangeRateSaveReqVO updateReqVO);

    /**
     * 删除汇率
     *
     * @param id 编号
     */
    void deleteExchangeRate(Long id);

    /**
     * 获得汇率
     *
     * @param id 编号
     * @return 汇率
     */
    ErpExchangeRateDO getExchangeRate(Long id);

    /**
     * 获得汇率分页
     *
     * @param pageReqVO 分页查询
     * @return 汇率分页
     */
    PageResult<ErpExchangeRateDO> getExchangeRatePage(ErpExchangeRatePageReqVO pageReqVO);

    /**
     * 获取指定日期的源币种到目标币种的最新汇率
     *
     * @param fromId 源币种 ID
     * @param toId   目标币种 ID
     * @param date    查询日期（null 默认今天）
     * @return 汇率记录
     */
    ErpExchangeRateDO getLatestRate(Long fromId, Long toId, LocalDate date);

}
