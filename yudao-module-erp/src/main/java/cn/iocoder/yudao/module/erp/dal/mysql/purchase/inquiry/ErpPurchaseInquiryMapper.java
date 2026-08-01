package cn.iocoder.yudao.module.erp.dal.mysql.purchase.inquiry;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.ErpPurchaseInquiryPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseInquiryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 采购询价单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseInquiryMapper extends BaseMapperX<ErpPurchaseInquiryDO> {

    default PageResult<ErpPurchaseInquiryDO> selectPage(ErpPurchaseInquiryPageReqVO reqVO) {
        return selectJoinPage(reqVO, ErpPurchaseInquiryDO.class, new MPJLambdaWrapperX<ErpPurchaseInquiryDO>()
                .likeIfPresent(ErpPurchaseInquiryDO::getNo, reqVO.getNo())
                .likeIfPresent(ErpPurchaseInquiryDO::getInquiryName, reqVO.getInquiryName())
                .eqIfPresent(ErpPurchaseInquiryDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpPurchaseInquiryDO::getExpectedDeliveryDate, reqVO.getExpectedDeliveryDate())
                .likeIfPresent(ErpPurchaseInquiryDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpPurchaseInquiryDO::getCreator, reqVO.getCreator())
                .orderByDesc(ErpPurchaseInquiryDO::getId));
    }

    default ErpPurchaseInquiryDO selectByNo(String no) {
        return selectOne(ErpPurchaseInquiryDO::getNo, no);
    }

}
