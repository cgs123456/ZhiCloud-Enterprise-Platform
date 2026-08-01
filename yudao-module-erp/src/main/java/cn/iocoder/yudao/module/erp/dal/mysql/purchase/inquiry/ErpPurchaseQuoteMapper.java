package cn.iocoder.yudao.module.erp.dal.mysql.purchase.inquiry;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.quote.ErpPurchaseQuotePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseQuoteDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 采购报价单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseQuoteMapper extends BaseMapperX<ErpPurchaseQuoteDO> {

    default PageResult<ErpPurchaseQuoteDO> selectPage(ErpPurchaseQuotePageReqVO reqVO) {
        return selectJoinPage(reqVO, ErpPurchaseQuoteDO.class, new MPJLambdaWrapperX<ErpPurchaseQuoteDO>()
                .likeIfPresent(ErpPurchaseQuoteDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPurchaseQuoteDO::getInquiryId, reqVO.getInquiryId())
                .eqIfPresent(ErpPurchaseQuoteDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPurchaseQuoteDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpPurchaseQuoteDO::getQuoteDate, reqVO.getQuoteDate())
                .likeIfPresent(ErpPurchaseQuoteDO::getRemark, reqVO.getRemark())
                .orderByDesc(ErpPurchaseQuoteDO::getId));
    }

    default ErpPurchaseQuoteDO selectByNo(String no) {
        return selectOne(ErpPurchaseQuoteDO::getNo, no);
    }

    default List<ErpPurchaseQuoteDO> selectListByInquiryId(Long inquiryId) {
        return selectList(ErpPurchaseQuoteDO::getInquiryId, inquiryId);
    }

}
