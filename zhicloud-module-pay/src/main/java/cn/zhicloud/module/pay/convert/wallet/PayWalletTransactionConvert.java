package cn.zhicloud.module.pay.convert.wallet;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.pay.controller.admin.wallet.vo.transaction.PayWalletTransactionRespVO;
import cn.zhicloud.module.pay.controller.app.wallet.vo.transaction.AppPayWalletTransactionRespVO;
import cn.zhicloud.module.pay.dal.dataobject.wallet.PayWalletTransactionDO;
import cn.zhicloud.module.pay.service.wallet.bo.WalletTransactionCreateReqBO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface PayWalletTransactionConvert {

    PayWalletTransactionConvert INSTANCE = Mappers.getMapper(PayWalletTransactionConvert.class);

    PageResult<PayWalletTransactionRespVO> convertPage2(PageResult<PayWalletTransactionDO> page);

    PayWalletTransactionDO convert(WalletTransactionCreateReqBO bean);

}
