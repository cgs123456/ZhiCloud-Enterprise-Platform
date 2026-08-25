package cn.zhicloud.module.wms.dal.mysql.billing;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.controller.admin.billing.vo.bill.WmsBillingBillPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.billing.WmsBillingBillDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * WMS 3PL 计费账单 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsBillingBillMapper extends BaseMapperX<WmsBillingBillDO> {

    default PageResult<WmsBillingBillDO> selectPage(WmsBillingBillPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsBillingBillDO>()
                .likeIfPresent(WmsBillingBillDO::getBillNo, reqVO.getBillNo())
                .eqIfPresent(WmsBillingBillDO::getOwnerId, reqVO.getOwnerId())
                .eqIfPresent(WmsBillingBillDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(WmsBillingBillDO::getBillingPeriodStart, reqVO.getBillingPeriod())
                .betweenIfPresent(WmsBillingBillDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WmsBillingBillDO::getId));
    }

    default WmsBillingBillDO selectByNo(String billNo) {
        return selectOne(WmsBillingBillDO::getBillNo, billNo);
    }

}
