package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.bankaccount.ErpBankAccountPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpBankAccountDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 银行账户 Mapper（P0-3）
 *
 * @author 智云
 */
@Mapper
public interface ErpBankAccountMapper extends BaseMapperX<ErpBankAccountDO> {

    default ErpBankAccountDO selectByAccountNo(String accountNo) {
        return selectOne(ErpBankAccountDO::getAccountNo, accountNo);
    }

    /**
     * 按账簿集合查账户（V195 账套视角现金流量表：账簿 → 账户 → 流水）
     *
     * <p>erp_cash_flow 只到 bank_account_id，给账户挂账簿（erp_bank_account.
     * account_book_id）后，按账套查流水 = 先按账簿集合查账户，再用账户集合
     * 过滤流水。空集合由调用方短路（避免 IN(空集合) 语义歧义）。
     */
    default List<ErpBankAccountDO> selectListByAccountBookIds(Collection<Long> accountBookIds) {
        return selectList(new LambdaQueryWrapperX<ErpBankAccountDO>()
                .inIfPresent(ErpBankAccountDO::getAccountBookId, accountBookIds)
                .orderByAsc(ErpBankAccountDO::getId));
    }

    default PageResult<ErpBankAccountDO> selectPage(ErpBankAccountPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpBankAccountDO>()
                .likeIfPresent(ErpBankAccountDO::getAccountNo, reqVO.getAccountNo())
                .likeIfPresent(ErpBankAccountDO::getAccountName, reqVO.getAccountName())
                .likeIfPresent(ErpBankAccountDO::getBankName, reqVO.getBankName())
                .eqIfPresent(ErpBankAccountDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpBankAccountDO::getId));
    }

}