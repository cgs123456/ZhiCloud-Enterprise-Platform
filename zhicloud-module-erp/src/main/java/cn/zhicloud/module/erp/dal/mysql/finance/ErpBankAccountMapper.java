package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.bankaccount.ErpBankAccountPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpBankAccountDO;
import org.apache.ibatis.annotations.Mapper;

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

    default PageResult<ErpBankAccountDO> selectPage(ErpBankAccountPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpBankAccountDO>()
                .likeIfPresent(ErpBankAccountDO::getAccountNo, reqVO.getAccountNo())
                .likeIfPresent(ErpBankAccountDO::getAccountName, reqVO.getAccountName())
                .likeIfPresent(ErpBankAccountDO::getBankName, reqVO.getBankName())
                .eqIfPresent(ErpBankAccountDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpBankAccountDO::getId));
    }

}