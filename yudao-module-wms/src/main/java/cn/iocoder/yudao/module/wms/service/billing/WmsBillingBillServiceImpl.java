package cn.iocoder.yudao.module.wms.service.billing;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.wms.controller.admin.billing.vo.bill.WmsBillingBillPageReqVO;
import cn.iocoder.yudao.module.wms.controller.admin.billing.vo.bill.WmsBillingBillSaveReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingBillDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingBillLineDO;
import cn.iocoder.yudao.module.wms.dal.mysql.billing.WmsBillingBillLineMapper;
import cn.iocoder.yudao.module.wms.dal.mysql.billing.WmsBillingBillMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.*;

/**
 * WMS 3PL 计费账单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class WmsBillingBillServiceImpl implements WmsBillingBillService {

    /**
     * 账单状态：10 草稿
     */
    public static final int BILL_STATUS_DRAFT = 10;
    /**
     * 账单状态：20 已确认
     */
    public static final int BILL_STATUS_CONFIRMED = 20;
    /**
     * 账单状态：30 已结算
     */
    public static final int BILL_STATUS_SETTLED = 30;
    /**
     * 账单状态：40 已付款
     */
    public static final int BILL_STATUS_PAID = 40;

    private static final DateTimeFormatter BILL_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private WmsBillingBillMapper billingBillMapper;
    @Resource
    private WmsBillingBillLineMapper billingBillLineMapper;
    @Resource
    private WmsBillingCalculatorService billingCalculatorService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createBillingBill(WmsBillingBillSaveReqVO createReqVO) {
        // 1. 校验账单号唯一
        validateBillingBillNoUnique(createReqVO.getId(), createReqVO.getBillNo());
        // 2. 插入账单
        WmsBillingBillDO bill = BeanUtils.toBean(createReqVO, WmsBillingBillDO.class);
        if (bill.getStatus() == null) {
            bill.setStatus(BILL_STATUS_DRAFT);
        }
        billingBillMapper.insert(bill);
        return bill.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateBillingBill(WmsBillingBillSaveReqVO updateReqVO) {
        // 1. 校验存在，且为草稿状态
        validateBillingBillDraft(updateReqVO.getId());
        // 1.1 校验账单号唯一
        validateBillingBillNoUnique(updateReqVO.getId(), updateReqVO.getBillNo());
        // 2. 更新账单
        WmsBillingBillDO updateObj = BeanUtils.toBean(updateReqVO, WmsBillingBillDO.class);
        billingBillMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBillingBill(Long id) {
        // 1. 校验存在，且可删除
        WmsBillingBillDO bill = validateBillingBillExists(id);
        if (ObjectUtil.notEqual(bill.getStatus(), BILL_STATUS_DRAFT)
                && ObjectUtil.notEqual(bill.getStatus(), BILL_STATUS_PAID)) {
            throw exception(BILLING_BILL_STATUS_NOT_DELETABLE);
        }
        // 2.1 删除账单
        billingBillMapper.deleteById(id);
        // 2.2 删除账单明细
        billingBillLineMapper.deleteByBillId(id);
    }

    @Override
    public WmsBillingBillDO getBillingBill(Long id) {
        return billingBillMapper.selectById(id);
    }

    @Override
    public PageResult<WmsBillingBillDO> getBillingBillPage(WmsBillingBillPageReqVO pageReqVO) {
        return billingBillMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateBill(Long ownerId, LocalDateTime start, LocalDateTime end) {
        // 1. 调用计费引擎计算
        WmsBillingCalculatorService.BillingCalculationResult result =
                billingCalculatorService.calculateBilling(ownerId, start, end);
        WmsBillingBillDO bill = result.getBill();
        // 2. 生成账单号
        bill.setBillNo(generateBillNo(ownerId));
        billingBillMapper.insert(bill);
        // 3. 插入账单明细
        List<WmsBillingBillLineDO> lines = result.getLines();
        if (CollUtil.isNotEmpty(lines)) {
            lines.forEach(line -> line.setId(null).setBillId(bill.getId()));
            billingBillLineMapper.insertBatch(lines);
        }
        return bill.getId();
    }

    @Override
    public List<WmsBillingBillLineDO> getBillLineList(Long billId) {
        return billingBillLineMapper.selectListByBillId(billId);
    }

    private String generateBillNo(Long ownerId) {
        return "ZD" + LocalDateTime.now().format(BILL_NO_FORMATTER) + ownerId;
    }

    private void validateBillingBillNoUnique(Long id, String billNo) {
        WmsBillingBillDO bill = billingBillMapper.selectByNo(billNo);
        if (bill == null) {
            return;
        }
        if (id == null || ObjectUtil.notEqual(bill.getId(), id)) {
            throw exception(BILLING_BILL_NO_DUPLICATE);
        }
    }

    private WmsBillingBillDO validateBillingBillExists(Long id) {
        WmsBillingBillDO bill = id == null ? null : billingBillMapper.selectById(id);
        if (bill == null) {
            throw exception(BILLING_BILL_NOT_EXISTS);
        }
        return bill;
    }

    /**
     * 校验账单存在且为草稿状态
     */
    private WmsBillingBillDO validateBillingBillDraft(Long id) {
        WmsBillingBillDO bill = validateBillingBillExists(id);
        if (ObjectUtil.notEqual(bill.getStatus(), BILL_STATUS_DRAFT)) {
            throw exception(BILLING_BILL_STATUS_NOT_DRAFT);
        }
        return bill;
    }

}
