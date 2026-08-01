package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.budget.ErpBudgetPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpBudgetDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 预算主表 Mapper（P0-14）
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpBudgetMapper extends BaseMapperX<ErpBudgetDO> {

    default ErpBudgetDO selectByBudgetNo(String budgetNo) {
        return selectOne(ErpBudgetDO::getBudgetNo, budgetNo);
    }

    default PageResult<ErpBudgetDO> selectPage(ErpBudgetPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpBudgetDO>()
                .likeIfPresent(ErpBudgetDO::getBudgetNo, reqVO.getBudgetNo())
                .eqIfPresent(ErpBudgetDO::getBudgetYear, reqVO.getBudgetYear())
                .eqIfPresent(ErpBudgetDO::getPeriodId, reqVO.getPeriodId())
                .eqIfPresent(ErpBudgetDO::getDepartmentId, reqVO.getDepartmentId())
                .eqIfPresent(ErpBudgetDO::getBudgetType, reqVO.getBudgetType())
                .eqIfPresent(ErpBudgetDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpBudgetDO::getId));
    }

}
