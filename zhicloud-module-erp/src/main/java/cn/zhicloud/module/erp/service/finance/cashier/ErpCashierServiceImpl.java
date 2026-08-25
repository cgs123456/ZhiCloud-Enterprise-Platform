package cn.zhicloud.module.erp.service.finance.cashier;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.controller.admin.finance.cashier.vo.ErpCashierPageReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.cashier.vo.ErpCashierSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cashier.ErpCashierDO;
import cn.zhicloud.module.erp.dal.mysql.finance.cashier.ErpCashierMapper;
import cn.zhicloud.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.zhicloud.module.erp.service.finance.ErpBankAccountService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.CASHIER_NO_EXISTS;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.CASHIER_NOT_EXISTS;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.CASHIER_STATUS_INVALID;

/**
 * ERP 出纳单 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class ErpCashierServiceImpl implements ErpCashierService {

    /**
     * 状态：10 待处理
     */
    private static final int STATUS_PENDING = 10;
    /**
     * 状态：20 已提交银行
     */
    private static final int STATUS_SUBMITTED = 20;
    /**
     * 状态：30 已到账
     */
    private static final int STATUS_ARRIVED = 30;
    /**
     * 状态：40 已退回
     */
    private static final int STATUS_RETURNED = 40;

    @Resource
    private ErpCashierMapper cashierMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpBankAccountService bankAccountService;
    @Resource
    private ErpBankDirectLinkService bankDirectLinkService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCashier(ErpCashierSaveReqVO createReqVO) {
        // 校验银行账户存在
        bankAccountService.validateBankAccount(createReqVO.getBankAccountId());
        // 生成出纳单号
        String no = createReqVO.getNo() != null ? createReqVO.getNo()
                : noRedisDAO.generate(ErpNoRedisDAO.CASHIER_NO_PREFIX);
        if (cashierMapper.selectByNo(no) != null) {
            throw exception(CASHIER_NO_EXISTS);
        }
        // 插入
        ErpCashierDO cashier = BeanUtils.toBean(createReqVO, ErpCashierDO.class);
        cashier.setNo(no);
        if (cashier.getStatus() == null) {
            cashier.setStatus(STATUS_PENDING);
        }
        cashierMapper.insert(cashier);
        return cashier.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCashier(ErpCashierSaveReqVO updateReqVO) {
        ErpCashierDO exist = validateCashier(updateReqVO.getId());
        // 只有待处理状态才能修改
        if (!ObjUtil.equal(exist.getStatus(), STATUS_PENDING)) {
            throw exception(CASHIER_STATUS_INVALID);
        }
        // 校验银行账户存在
        bankAccountService.validateBankAccount(updateReqVO.getBankAccountId());
        ErpCashierDO updateObj = BeanUtils.toBean(updateReqVO, ErpCashierDO.class);
        // 不允许通过修改改变状态与银行流水号
        updateObj.setStatus(null);
        updateObj.setBankSerialNo(null);
        cashierMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCashier(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        for (Long id : ids) {
            ErpCashierDO cashier = validateCashier(id);
            // 只有待处理/已退回状态才能删除
            if (!ObjUtil.equal(cashier.getStatus(), STATUS_PENDING)
                    && !ObjUtil.equal(cashier.getStatus(), STATUS_RETURNED)) {
                throw exception(CASHIER_STATUS_INVALID);
            }
            cashierMapper.deleteById(id);
        }
    }

    @Override
    public ErpCashierDO getCashier(Long id) {
        return cashierMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCashierDO> getCashierPage(ErpCashierPageReqVO pageReqVO) {
        return cashierMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submitToBank(Long id) {
        ErpCashierDO cashier = validateCashier(id);
        // 只有待处理状态才能提交银行
        if (!ObjUtil.equal(cashier.getStatus(), STATUS_PENDING)) {
            throw exception(CASHIER_STATUS_INVALID);
        }
        // 调用网银直联接口发送支付指令
        String bankSerialNo = bankDirectLinkService.sendPayment(cashier);
        // 更新状态为已提交银行，并记录银行流水号
        ErpCashierDO update = new ErpCashierDO();
        update.setId(id);
        update.setStatus(STATUS_SUBMITTED);
        update.setBankSerialNo(bankSerialNo);
        cashierMapper.updateById(update);
        return bankSerialNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer syncBankStatus(Long id) {
        ErpCashierDO cashier = validateCashier(id);
        // 只有已提交银行状态才能同步
        if (!ObjUtil.equal(cashier.getStatus(), STATUS_SUBMITTED)) {
            throw exception(CASHIER_STATUS_INVALID);
        }
        if (cashier.getBankSerialNo() == null) {
            throw exception(CASHIER_STATUS_INVALID);
        }
        // 调用网银直联接口查询状态
        Integer status = bankDirectLinkService.queryPaymentStatus(cashier.getBankSerialNo());
        // 状态映射：银行返回 30 视为已到账，其他保持已提交银行
        int mappedStatus = (status != null && status == STATUS_ARRIVED) ? STATUS_ARRIVED : STATUS_SUBMITTED;
        if (mappedStatus != cashier.getStatus()) {
            ErpCashierDO update = new ErpCashierDO();
            update.setId(id);
            update.setStatus(mappedStatus);
            cashierMapper.updateById(update);
        }
        return mappedStatus;
    }

    @Override
    public ErpCashierDO validateCashier(Long id) {
        ErpCashierDO cashier = cashierMapper.selectById(id);
        if (cashier == null) {
            throw exception(CASHIER_NOT_EXISTS);
        }
        return cashier;
    }

}
