package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.api.ErpAccountBookApi;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountBookRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetInitializeReqVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.accountset.FmsAccountSetSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountSetDO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsAccountUserDO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsFinanceParameterDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsAccountSetMapper;
import cn.zhicloud.module.fms.dal.mysql.config.FmsAccountUserMapper;
import cn.zhicloud.module.fms.dal.mysql.config.FmsFinanceParameterMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.ACCOUNT_SET_COMPANY_CODE_DUPLICATE;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.ACCOUNT_SET_INITIALIZED;
import static cn.zhicloud.module.fms.enums.ErrorCodeConstants.ACCOUNT_SET_NOT_EXISTS;

/**
 * FMS 账套 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsAccountSetServiceImpl implements FmsAccountSetService {

    @Resource
    private FmsAccountSetMapper accountSetMapper;
    @Resource
    private FmsAccountUserMapper accountUserMapper;
    @Resource
    private FmsFinanceParameterMapper financeParameterMapper;
    /**
     * ERP 账簿只读查询（V191 跨模块契约，经 erp.api 边界包；不直接 import ERP 的 mapper/service */
    @Resource
    private ErpAccountBookApi erpAccountBookApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccountSet(Long userId, FmsAccountSetSaveReqVO createReqVO) {
        // 校验公司编码唯一
        if (accountSetMapper.selectByCompanyCode(createReqVO.getCompanyCode()) != null) {
            throw exception(ACCOUNT_SET_COMPANY_CODE_DUPLICATE);
        }
        FmsAccountSetDO accountSet = BeanUtils.toBean(createReqVO, FmsAccountSetDO.class);
        accountSet.setInitialized(false);
        accountSet.setDefaultStatus(false);
        accountSetMapper.insert(accountSet);
        // 创建人自动成为账套主管（OWNER）
        FmsAccountUserDO founder = FmsAccountUserDO.builder()
                .accountSetId(accountSet.getId()).userId(userId)
                .level(FmsAccountUserDO.LEVEL_ADMIN).defaultStatus(false)
                .build();
        accountUserMapper.insert(founder);
        return accountSet.getId();
    }

    @Override
    public void updateAccountSet(Long userId, FmsAccountSetSaveReqVO updateReqVO) {
        validateExists(updateReqVO.getId());
        FmsAccountSetDO updateObj = BeanUtils.toBean(updateReqVO, FmsAccountSetDO.class);
        accountSetMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initializeAccountSet(FmsAccountSetInitializeReqVO reqVO) {
        FmsAccountSetDO accountSet = validateExists(reqVO.getAccountSetId());
        if (Boolean.TRUE.equals(accountSet.getInitialized())) {
            throw exception(ACCOUNT_SET_INITIALIZED);
        }
        // 1.2 本位币改为直接记 code（2026-10-13 FMS/ERP 财务融合：删 fms_currency 表，
        //     本位币统一走 ERP 的 erp_currency 单一真相，此处只存编码快照）
        // 2. 回填账套
        FmsAccountSetDO updateObj = new FmsAccountSetDO();
        updateObj.setId(accountSet.getId());
        updateObj.setCurrencyId(0L);
        updateObj.setCurrencyCode(reqVO.getCurrencyCode());
        updateObj.setStartTime(reqVO.getStartTime());
        updateObj.setStandard(reqVO.getStandard());
        updateObj.setInitialized(true);
        accountSetMapper.updateById(updateObj);
        // 3. 创建账套参数
        FmsFinanceParameterDO parameter = FmsFinanceParameterDO.builder()
                .accountSetId(accountSet.getId()).standard(reqVO.getStandard())
                .level(reqVO.getLevel()).subjectCodeRule(reqVO.getSubjectCodeRule())
                .ledgerBalanceMode(reqVO.getLedgerBalanceMode()).voucherReviewRequired(true)
                .build();
        financeParameterMapper.insert(parameter);
    }

    @Override
    public FmsAccountSetDO getAccountSet(Long id) {
        return accountSetMapper.selectById(id);
    }

    @Override
    public List<FmsAccountBookRespVO> getAccountBookList(Long accountSetId) {
        if (accountSetId == null) {
            return List.of();
        }
        // 校验账套存在，避免为不存在的账套返回空列表掩盖了编号错误
        if (accountSetMapper.selectById(accountSetId) == null) {
            throw exception(ACCOUNT_SET_NOT_EXISTS);
        }
        // 跨模块只读查询，VO 在边界处独立转换（不把 ERP 的 RespVO 直接泄漏给 FMS 前端）
        List<cn.zhicloud.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookRespVO> books =
                erpAccountBookApi.getAccountBookListByAccountSetId(accountSetId);
        return BeanUtils.toBean(books, FmsAccountBookRespVO.class);
    }

    @Override
    public List<FmsAccountSetDO> getAccountSetList(Long userId) {
        List<FmsAccountUserDO> members = accountUserMapper.selectListByUser(userId);
        if (members.isEmpty()) {
            return List.of();
        }
        return accountSetMapper.selectListByIds(members.stream().map(FmsAccountUserDO::getAccountSetId).distinct().toList());
    }

    private FmsAccountSetDO validateExists(Long id) {
        FmsAccountSetDO accountSet = accountSetMapper.selectById(id);
        if (accountSet == null) {
            throw exception(ACCOUNT_SET_NOT_EXISTS);
        }
        return accountSet;
    }

}
