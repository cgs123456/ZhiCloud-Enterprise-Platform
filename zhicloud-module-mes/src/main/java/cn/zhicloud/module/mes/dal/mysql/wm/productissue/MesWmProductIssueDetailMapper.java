package cn.zhicloud.module.mes.dal.mysql.wm.productissue;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDetailDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * MES 领料出库明细 Mapper
 */
@Mapper
public interface MesWmProductIssueDetailMapper extends BaseMapperX<MesWmProductIssueDetailDO> {

    default List<MesWmProductIssueDetailDO> selectListByLineId(Long lineId) {
        return selectList(MesWmProductIssueDetailDO::getLineId, lineId);
    }

    default List<MesWmProductIssueDetailDO> selectListByIssueId(Long issueId) {
        return selectList(MesWmProductIssueDetailDO::getIssueId, issueId);
    }

    default void deleteByIssueId(Long issueId) {
        delete(MesWmProductIssueDetailDO::getIssueId, issueId);
    }

    default void deleteByLineId(Long lineId) {
        delete(MesWmProductIssueDetailDO::getLineId, lineId);
    }

    /**
     * 按工单统计各物料在历史「已完成」领料单中的累计已发数量
     * <p>
     * 用于发料 BOM 累计上限校验：本次发料 + 历史已发 不得超过工单 BOM 应发总量。
     *
     * @param workOrderId 生产工单编号
     * @param status      领料单状态（传入 FINISHED 表示只统计已领出的）
     * @return item_id → 累计已发数量 的映射
     */
    @Select("SELECT d.item_id AS item_id, SUM(d.quantity) AS issued_quantity "
            + "FROM mes_wm_product_issue_detail d "
            + "INNER JOIN mes_wm_product_issue i ON i.id = d.issue_id "
            + "WHERE i.work_order_id = #{workOrderId} AND i.status = #{status} "
            + "GROUP BY d.item_id")
    List<Map<String, Object>> selectIssuedQuantityByWorkOrderId(@Param("workOrderId") Long workOrderId,
                                                                @Param("status") Integer status);

}
