package cn.iocoder.yudao.module.erp.dal.mysql.purchase.inquiry;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.inquiry.vo.compare.ErpPurchaseComparePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry.ErpPurchaseCompareDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 采购比价单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseCompareMapper extends BaseMapperX<ErpPurchaseCompareDO> {

    default PageResult<ErpPurchaseCompareDO> selectPage(ErpPurchaseComparePageReqVO reqVO) {
        return selectJoinPage(reqVO, ErpPurchaseCompareDO.class, new MPJLambdaWrapperX<ErpPurchaseCompareDO>()
                .likeIfPresent(ErpPurchaseCompareDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpPurchaseCompareDO::getInquiryId, reqVO.getInquiryId())
                .eqIfPresent(ErpPurchaseCompareDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpPurchaseCompareDO::getRecommendSupplierId, reqVO.getRecommendSupplierId())
                .orderByDesc(ErpPurchaseCompareDO::getId));
    }

    default ErpPurchaseCompareDO selectByNo(String no) {
        return selectOne(ErpPurchaseCompareDO::getNo, no);
    }

    default ErpPurchaseCompareDO selectByInquiryId(Long inquiryId) {
        return selectOne(ErpPurchaseCompareDO::getInquiryId, inquiryId);
    }

}
