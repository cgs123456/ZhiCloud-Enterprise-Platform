package cn.zhicloud.module.erp.dal.mysql.finance;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.zhicloud.module.erp.enums.finance.ErpAccountBookStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 账簿 Mapper（P1-多账簿）
 *
 * @author 智云
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

    /**
     * 查启用的主账簿（不限会计准则）
     *
     * <p>§12.215 修 V191 断链：凭证创建时未显式指定账簿的默认归属。
     * 不限定准则——租户只配了 IFRS 主账簿时按 CAS(10) 查会查不到，
     * 凭证又退化成 account_book_id 恒 NULL。多条主账簿取 sort 最小 + id 最大
     * （与 selectPage 的既有排序口径一致，保证确定性）。
     *
     * <p>注意状态口径：账簿用 {@link ErpAccountBookStatusEnum}（10 启用 / 20 禁用），
     * 不是 CommonStatusEnum 的 0/1——写错一个数字就等于查不到任何主账簿。
     */
    default ErpAccountBookDO selectEnabledPrimary() {
        List<ErpAccountBookDO> books = selectList(new LambdaQueryWrapperX<ErpAccountBookDO>()
                .eq(ErpAccountBookDO::getIsPrimary, true)
                .eq(ErpAccountBookDO::getStatus, ErpAccountBookStatusEnum.ENABLED.getStatus())
                .orderByAsc(ErpAccountBookDO::getSort)
                .orderByDesc(ErpAccountBookDO::getId));
        return books.isEmpty() ? null : books.get(0);
    }

    default List<ErpAccountBookDO> selectListByStatus(Integer status) {
        return selectList(ErpAccountBookDO::getStatus, status);
    }

    /**
     * 按 FMS 账套查账簿列表（V191 打通「账套 → 账簿」层级后供跨模块只读查询）
     *
     * <p>只读、不建平行表：FMS 账套详情页据此展示「本账套挂了哪些账簿」。
     */
    default List<ErpAccountBookDO> selectListByAccountSetId(Long accountSetId) {
        return selectList(new LambdaQueryWrapperX<ErpAccountBookDO>()
                .eq(ErpAccountBookDO::getAccountSetId, accountSetId)
                .orderByAsc(ErpAccountBookDO::getSort)
                .orderByDesc(ErpAccountBookDO::getId));
    }

    default PageResult<ErpAccountBookDO> selectPage(ErpAccountBookPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAccountBookDO>()
                .likeIfPresent(ErpAccountBookDO::getCode, reqVO.getCode())
                .eqIfPresent(ErpAccountBookDO::getAccountSetId, reqVO.getAccountSetId())
                .likeIfPresent(ErpAccountBookDO::getName, reqVO.getName())
                .eqIfPresent(ErpAccountBookDO::getAccountingStandard, reqVO.getAccountingStandard())
                .eqIfPresent(ErpAccountBookDO::getCurrencyId, reqVO.getCurrencyId())
                .eqIfPresent(ErpAccountBookDO::getIsPrimary, reqVO.getIsPrimary())
                .eqIfPresent(ErpAccountBookDO::getStatus, reqVO.getStatus())
                .orderByAsc(ErpAccountBookDO::getSort)
                .orderByDesc(ErpAccountBookDO::getId));
    }

}
