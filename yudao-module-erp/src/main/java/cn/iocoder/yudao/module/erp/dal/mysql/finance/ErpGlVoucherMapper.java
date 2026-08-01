package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * ERP 会计凭证 Mapper（P0-7）
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpGlVoucherMapper extends BaseMapperX<ErpGlVoucherDO> {

    default ErpGlVoucherDO selectByVoucherNo(String voucherNo) {
        return selectOne(ErpGlVoucherDO::getVoucherNo, voucherNo);
    }

    default List<ErpGlVoucherDO> selectListByPeriodId(Long periodId) {
        return selectList(ErpGlVoucherDO::getPeriodId, periodId);
    }

    default PageResult<ErpGlVoucherDO> selectPage(ErpGlVoucherPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpGlVoucherDO>()
                .likeIfPresent(ErpGlVoucherDO::getVoucherNo, reqVO.getVoucherNo())
                .eqIfPresent(ErpGlVoucherDO::getVoucherType, reqVO.getVoucherType())
                .eqIfPresent(ErpGlVoucherDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpGlVoucherDO::getPeriodId, reqVO.getPeriodId())
                .geIfPresent(ErpGlVoucherDO::getVoucherDate, reqVO.getVoucherDateStart())
                .leIfPresent(ErpGlVoucherDO::getVoucherDate, reqVO.getVoucherDateEnd())
                .orderByDesc(ErpGlVoucherDO::getVoucherDate)
                .orderByDesc(ErpGlVoucherDO::getId));
    }

    default Long selectCountByPeriodId(Long periodId) {
        return selectCount(ErpGlVoucherDO::getPeriodId, periodId);
    }

    default Long selectCountByPeriodIdAndStatus(Long periodId, Integer status) {
        return selectCount(new LambdaQueryWrapperX<ErpGlVoucherDO>()
                .eq(ErpGlVoucherDO::getPeriodId, periodId)
                .eq(ErpGlVoucherDO::getStatus, status));
    }

    default Long selectCountByAccountBookId(Long accountBookId) {
        return selectCount(ErpGlVoucherDO::getAccountBookId, accountBookId);
    }

}
