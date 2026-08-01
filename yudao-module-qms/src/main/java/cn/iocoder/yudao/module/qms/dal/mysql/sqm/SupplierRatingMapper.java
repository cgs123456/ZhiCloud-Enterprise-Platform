package cn.iocoder.yudao.module.qms.dal.mysql.sqm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.sqm.vo.SupplierRatingPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.sqm.SupplierRatingDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 供应商评级 Mapper
 *
 * @author yudao
 */
@Mapper
public interface SupplierRatingMapper extends BaseMapperX<SupplierRatingDO> {

    default PageResult<SupplierRatingDO> selectPage(SupplierRatingPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SupplierRatingDO>()
                .likeIfPresent(SupplierRatingDO::getRatingNo, reqVO.getRatingNo())
                .eqIfPresent(SupplierRatingDO::getSupplierId, reqVO.getSupplierId())
                .likeIfPresent(SupplierRatingDO::getRatingPeriod, reqVO.getRatingPeriod())
                .eqIfPresent(SupplierRatingDO::getGrade, reqVO.getGrade())
                .orderByDesc(SupplierRatingDO::getId));
    }

    default SupplierRatingDO selectByRatingNo(String ratingNo) {
        return selectOne(SupplierRatingDO::getRatingNo, ratingNo);
    }

}