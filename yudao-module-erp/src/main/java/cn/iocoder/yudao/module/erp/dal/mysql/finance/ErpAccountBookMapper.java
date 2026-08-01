package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 账簿 Mapper（P1-多账簿）
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpAccountBookMapper extends BaseMapperX<ErpAccountBookDO> {

    default ErpAccountBookDO selectByCode(String code) {
        return selectOne(ErpAccountBookDO::getCode, code);
    }

    default ErpAccountBookDO selectPrimaryByAccountingStandard(Integer accountingStandard) {
        return selectOne(new LambdaQueryWrapperX<ErpAccountBookDO>()
                .eq(ErpAccountBookDO::getAccountingStandard, accountingStandard)
                .eq(ErpAccountBookDO::getIsPrimary, true));
    }

    default List<ErpAccountBookDO> selectListByStatus(Integer status) {
        return selectList(ErpAccountBookDO::getStatus, status);
    }

    default PageResult<ErpAccountBookDO> selectPage(ErpAccountBookPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAccountBookDO>()
                .likeIfPresent(ErpAccountBookDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpAccountBookDO::getName, reqVO.getName())
                .eqIfPresent(ErpAccountBookDO::getAccountingStandard, reqVO.getAccountingStandard())
                .eqIfPresent(ErpAccountBookDO::getCurrencyId, reqVO.getCurrencyId())
                .eqIfPresent(ErpAccountBookDO::getIsPrimary, reqVO.getIsPrimary())
                .eqIfPresent(ErpAccountBookDO::getStatus, reqVO.getStatus())
                .orderByAsc(ErpAccountBookDO::getSort)
                .orderByDesc(ErpAccountBookDO::getId));
    }

}
