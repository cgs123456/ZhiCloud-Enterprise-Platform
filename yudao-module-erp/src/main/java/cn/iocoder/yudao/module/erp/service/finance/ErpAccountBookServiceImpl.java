package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpAccountBookMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpAccountBookStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

/**
 * ERP 账簿 Service 实现类（P1-多账簿）
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpAccountBookServiceImpl implements ErpAccountBookService {

    @Resource
    private ErpAccountBookMapper accountBookMapper;

    @Resource
    private ErpGlVoucherMapper glVoucherMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccountBook(ErpAccountBookSaveReqVO createReqVO) {
        // 校验编码唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 校验主账簿唯一性（同一会计准则下只能有一个主账簿）
        validatePrimaryUnique(null, createReqVO.getAccountingStandard(), createReqVO.getIsPrimary());
        // 插入
        ErpAccountBookDO accountBook = BeanUtils.toBean(createReqVO, ErpAccountBookDO.class);
        if (accountBook.getIsPrimary() == null) {
            accountBook.setIsPrimary(false);
        }
        if (accountBook.getStatus() == null) {
            accountBook.setStatus(ErpAccountBookStatusEnum.ENABLED.getStatus());
        }
        accountBookMapper.insert(accountBook);
        return accountBook.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccountBook(ErpAccountBookSaveReqVO updateReqVO) {
        // 校验存在
        validateAccountBookExists(updateReqVO.getId());
        // 校验编码唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 校验主账簿唯一性
        validatePrimaryUnique(updateReqVO.getId(), updateReqVO.getAccountingStandard(), updateReqVO.getIsPrimary());
        // 更新
        ErpAccountBookDO updateObj = BeanUtils.toBean(updateReqVO, ErpAccountBookDO.class);
        accountBookMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccountBook(Long id) {
        // 校验存在
        validateAccountBookExists(id);
        // 校验是否被凭证引用
        Long voucherCount = glVoucherMapper.selectCountByAccountBookId(id);
        if (voucherCount != null && voucherCount > 0) {
            throw exception(ACCOUNT_BOOK_HAS_VOUCHERS);
        }
        // 删除
        accountBookMapper.deleteById(id);
    }

    @Override
    public ErpAccountBookDO getAccountBook(Long id) {
        return accountBookMapper.selectById(id);
    }

    @Override
    public PageResult<ErpAccountBookDO> getAccountBookPage(ErpAccountBookPageReqVO pageReqVO) {
        return accountBookMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpAccountBookDO> getEnabledAccountBookList() {
        return accountBookMapper.selectListByStatus(ErpAccountBookStatusEnum.ENABLED.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setPrimary(Long id) {
        // 校验存在
        ErpAccountBookDO accountBook = validateAccountBookExists(id);
        // 校验状态必须启用
        if (!ErpAccountBookStatusEnum.ENABLED.getStatus().equals(accountBook.getStatus())) {
            throw exception(ACCOUNT_BOOK_STATUS_INVALID);
        }
        // 将同一会计准则下原主账簿置为非主账簿
        ErpAccountBookDO oldPrimary = accountBookMapper.selectPrimaryByAccountingStandard(
                accountBook.getAccountingStandard());
        if (oldPrimary != null && !Objects.equals(oldPrimary.getId(), id)) {
            ErpAccountBookDO updateOld = new ErpAccountBookDO();
            updateOld.setId(oldPrimary.getId());
            updateOld.setIsPrimary(false);
            accountBookMapper.updateById(updateOld);
        }
        // 将当前账簿设置为主账簿
        ErpAccountBookDO updateNew = new ErpAccountBookDO();
        updateNew.setId(id);
        updateNew.setIsPrimary(true);
        accountBookMapper.updateById(updateNew);
    }

    // ==================== 内部辅助方法 ====================

    private ErpAccountBookDO validateAccountBookExists(Long id) {
        if (id == null) {
            throw exception(ACCOUNT_BOOK_NOT_EXISTS);
        }
        ErpAccountBookDO accountBook = accountBookMapper.selectById(id);
        if (accountBook == null) {
            throw exception(ACCOUNT_BOOK_NOT_EXISTS);
        }
        return accountBook;
    }

    private void validateCodeUnique(Long id, String code) {
        ErpAccountBookDO existing = accountBookMapper.selectByCode(code);
        if (existing != null && !Objects.equals(existing.getId(), id)) {
            throw exception(ACCOUNT_BOOK_CODE_DUPLICATE, code);
        }
    }

    /**
     * 校验主账簿唯一性：同一会计准则下最多一个主账簿
     */
    private void validatePrimaryUnique(Long id, Integer accountingStandard, Boolean isPrimary) {
        if (!Boolean.TRUE.equals(isPrimary)) {
            return;
        }
        ErpAccountBookDO primary = accountBookMapper.selectPrimaryByAccountingStandard(accountingStandard);
        if (primary != null && !Objects.equals(primary.getId(), id)) {
            throw exception(ACCOUNT_BOOK_PRIMARY_EXISTS, primary.getCode());
        }
    }

}
