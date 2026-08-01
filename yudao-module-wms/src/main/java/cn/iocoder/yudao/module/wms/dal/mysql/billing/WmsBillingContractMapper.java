package cn.iocoder.yudao.module.wms.dal.mysql.billing;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.wms.controller.admin.billing.vo.contract.WmsBillingContractPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.billing.WmsBillingContractDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * WMS 3PL 计费合同 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface WmsBillingContractMapper extends BaseMapperX<WmsBillingContractDO> {

    default PageResult<WmsBillingContractDO> selectPage(WmsBillingContractPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsBillingContractDO>()
                .likeIfPresent(WmsBillingContractDO::getContractNo, reqVO.getContractNo())
                .likeIfPresent(WmsBillingContractDO::getContractName, reqVO.getContractName())
                .eqIfPresent(WmsBillingContractDO::getOwnerId, reqVO.getOwnerId())
                .eqIfPresent(WmsBillingContractDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(WmsBillingContractDO::getStartDate, reqVO.getStartDate())
                .betweenIfPresent(WmsBillingContractDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WmsBillingContractDO::getId));
    }

    default WmsBillingContractDO selectByNo(String contractNo) {
        return selectOne(WmsBillingContractDO::getContractNo, contractNo);
    }

    default Long selectCountByOwnerId(Long ownerId) {
        return selectCount(WmsBillingContractDO::getOwnerId, ownerId);
    }

}
