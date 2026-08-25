package cn.zhicloud.module.erp.dal.mysql.finance.tax;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.taxrate.ErpTaxRatePageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.tax.ErpTaxRateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpTaxRateMapper extends BaseMapperX<ErpTaxRateDO> {

    default ErpTaxRateDO selectByCode(String code) {
        return selectOne(ErpTaxRateDO::getCode, code);
    }

    default ErpTaxRateDO selectDefault() {
        return selectOne(ErpTaxRateDO::getIsDefault, 1);
    }

    default PageResult<ErpTaxRateDO> selectPage(ErpTaxRatePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpTaxRateDO>()
                .likeIfPresent(ErpTaxRateDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpTaxRateDO::getName, reqVO.getName())
                .eqIfPresent(ErpTaxRateDO::getRateType, reqVO.getRateType())
                .eqIfPresent(ErpTaxRateDO::getStatus, reqVO.getStatus())
                .orderByAsc(ErpTaxRateDO::getCode));
    }

}
