package cn.iocoder.yudao.module.wms.service.billing;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.billing.vo.bill.WmsBillingBillPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.billing.vo.bill.WmsBillingBillSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingBillDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingBillLineDO;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;

/**
 * WMS 3PL 计费账单 Service 接口
 *
 * @author 芋道源码
 */
public interface WmsBillingBillService {

    /**
     * 创建计费账单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createBillingBill(@Valid WmsBillingBillSaveReqVO createReqVO);

    /**
     * 更新计费账单
     *
     * @param updateReqVO 更新信息
     */
    void updateBillingBill(@Valid WmsBillingBillSaveReqVO updateReqVO);

    /**
     * 删除计费账单
     *
     * @param id 编号
     */
    void deleteBillingBill(Long id);

    /**
     * 获得计费账单
     *
     * @param id 编号
     * @return 计费账单
     */
    WmsBillingBillDO getBillingBill(Long id);

    /**
     * 获得计费账单分页
     *
     * @param pageReqVO 分页查询
     * @return 计费账单分页
     */
    PageResult<WmsBillingBillDO> getBillingBillPage(WmsBillingBillPageReqVO pageReqVO);

    /**
     * 生成计费账单（调用计费引擎）
     *
     * @param ownerId 货主编号
     * @param start 计费周期开始时间
     * @param end 计费周期结束时间
     * @return 账单编号
     */
    Long generateBill(Long ownerId, LocalDateTime start, LocalDateTime end);

    /**
     * 获得账单明细列表
     *
     * @param billId 账单编号
     * @return 明细列表
     */
    List<WmsBillingBillLineDO> getBillLineList(Long billId);

}
