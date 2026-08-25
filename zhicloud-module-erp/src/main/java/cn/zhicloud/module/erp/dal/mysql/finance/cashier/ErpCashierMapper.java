package cn.zhicloud.module.erp.dal.mysql.finance.cashier;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.cashier.vo.ErpCashierPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.cashier.ErpCashierDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 出纳单 Mapper
 *
 * @author 智云
 */
@Mapper
public interface ErpCashierMapper extends BaseMapperX<ErpCashierDO> {

    default PageResult<ErpCashierDO> selectPage(ErpCashierPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpCashierDO>()
                .likeIfPresent(ErpCashierDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpCashierDO::getCashierType, reqVO.getCashierType())
                .eqIfPresent(ErpCashierDO::getBankAccountId, reqVO.getBankAccountId())
                .eqIfPresent(ErpCashierDO::getPaymentMethod, reqVO.getPaymentMethod())
                .eqIfPresent(ErpCashierDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpCashierDO::getCounterpartyName, reqVO.getCounterpartyName())
                .eqIfPresent(ErpCashierDO::getBusinessOrderNo, reqVO.getBusinessOrderNo())
                .betweenIfPresent(ErpCashierDO::getPaymentDate, reqVO.getPaymentDate())
                .betweenIfPresent(ErpCashierDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ErpCashierDO::getId));
    }

    default ErpCashierDO selectByNo(String no) {
        return selectOne(ErpCashierDO::getNo, no);
    }

}
