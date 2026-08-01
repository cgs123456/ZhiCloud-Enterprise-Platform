package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.bankaccount.ErpBankAccountPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpBankAccountDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 银行账户 Mapper（P0-3）
 *
 * @author 芋道源码
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