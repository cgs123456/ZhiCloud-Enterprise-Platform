package cn.zhicloud.module.erp.service.finance.cashier;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.finance.cashier.vo.ErpCashierPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.cashier.vo.ErpCashierSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cashier.ErpCashierDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 出纳单 Service 接口
 *
 * @author 智云
 */
public interface ErpCashierService {

    /**
     * 创建出纳单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCashier(@Valid ErpCashierSaveReqVO createReqVO);

    /**
     * 更新出纳单
     *
     * @param updateReqVO 更新信息
     */
    void updateCashier(@Valid ErpCashierSaveReqVO updateReqVO);

    /**
     * 删除出纳单
     *
     * @param ids 编号数组
     */
    void deleteCashier(List<Long> ids);

    /**
     * 获得出纳单
     *
     * @param id 编号
     * @return 出纳单
     */
    ErpCashierDO getCashier(Long id);

    /**
     * 获得出纳单分页
     *
     * @param pageReqVO 分页查询
     * @return 出纳单分页
     */
    PageResult<ErpCashierDO> getCashierPage(ErpCashierPageReqVO pageReqVO);

    /**
     * 提交银行（调用网银直联接口发送支付指令）
     *
     * @param id 出纳单编号
     * @return 银行流水号
     */
    String submitToBank(Long id);

    /**
     * 同步银行状态（调用网银直联接口查询支付状态）
     *
     * @param id 出纳单编号
     * @return 最新状态
     */
    Integer syncBankStatus(Long id);

    /**
     * 校验出纳单存在
     *
     * @param id 编号
     * @return 出纳单
     */
    ErpCashierDO validateCashier(Long id);

}
