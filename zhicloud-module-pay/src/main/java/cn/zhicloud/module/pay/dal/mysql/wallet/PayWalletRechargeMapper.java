package cn.zhicloud.module.pay.dal.mysql.wallet;

import cn.zhicloud.framework.common.pojo.PageParam;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.pay.dal.dataobject.wallet.PayWalletRechargeDO;
import cn.zhicloud.module.pay.dal.dataobject.wallet.PayWalletRechargePackageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayWalletRechargeMapper extends BaseMapperX<PayWalletRechargeDO> {

    default int updateByIdAndPaid(Long id, boolean wherePayStatus, PayWalletRechargeDO updateObj) {
        return update(updateObj, new LambdaQueryWrapperX<PayWalletRechargeDO>()
                .eq(PayWalletRechargeDO::getId, id).eq(PayWalletRechargeDO::getPayStatus, wherePayStatus));
    }

    default int updateByIdAndRefunded(Long id, Integer whereRefundStatus, PayWalletRechargeDO updateObj) {
        return update(updateObj, new LambdaQueryWrapperX<PayWalletRechargeDO>()
                .eq(PayWalletRechargeDO::getId, id).eq(PayWalletRechargeDO::getRefundStatus, whereRefundStatus));
    }

    default PageResult<PayWalletRechargeDO> selectPage(PageParam pageReqVO, Long walletId, Boolean payStatus) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<PayWalletRechargeDO>()
                .eq(PayWalletRechargeDO::getWalletId, walletId)
                .eq(PayWalletRechargeDO::getPayStatus, payStatus)
                .orderByDesc(PayWalletRechargeDO::getId));
    }

}