package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 会计凭证分录 Mapper（P0-7）
 *
 * @author 智云
 */
@Mapper
public interface ErpGlVoucherEntryMapper extends BaseMapperX<ErpGlVoucherEntryDO> {

    default List<ErpGlVoucherEntryDO> selectListByVoucherId(Long voucherId) {
        return selectList(new LambdaQueryWrapperX<ErpGlVoucherEntryDO>()
                .eq(ErpGlVoucherEntryDO::getVoucherId, voucherId)
                .orderByAsc(ErpGlVoucherEntryDO::getSort));
    }

    default List<ErpGlVoucherEntryDO> selectListByVoucherIds(List<Long> voucherIds) {
        return selectList(new LambdaQueryWrapperX<ErpGlVoucherEntryDO>()
                .in(ErpGlVoucherEntryDO::getVoucherId, voucherIds)
                .orderByAsc(ErpGlVoucherEntryDO::getVoucherId)
                .orderByAsc(ErpGlVoucherEntryDO::getSort));
    }

    default Long selectCountByAccountId(Long accountId) {
        return selectCount(ErpGlVoucherEntryDO::getAccountId, accountId);
    }

    default int deleteByVoucherId(Long voucherId) {
        return delete(new LambdaQueryWrapperX<ErpGlVoucherEntryDO>()
                .eq(ErpGlVoucherEntryDO::getVoucherId, voucherId));
    }

}
