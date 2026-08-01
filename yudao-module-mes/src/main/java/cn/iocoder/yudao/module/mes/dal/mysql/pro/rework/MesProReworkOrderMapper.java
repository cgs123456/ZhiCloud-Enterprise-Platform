package cn.iocoder.yudao.module.mes.dal.mysql.pro.rework;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.rework.MesProReworkOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.List;

/**
 * MES 返工工单 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesProReworkOrderMapper extends BaseMapperX<MesProReworkOrderDO> {

    default PageResult<MesProReworkOrderDO> selectPage(MesProReworkOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProReworkOrderDO>()
                .likeIfPresent(MesProReworkOrderDO::getCode, reqVO.getCode())
                .eqIfPresent(MesProReworkOrderDO::getOriginalWorkOrderId, reqVO.getOriginalWorkOrderId())
                .likeIfPresent(MesProReworkOrderDO::getOriginalWorkOrderCode, reqVO.getOriginalWorkOrderCode())
                .eqIfPresent(MesProReworkOrderDO::getProductId, reqVO.getProductId())
                .eqIfPresent(MesProReworkOrderDO::getReworkType, reqVO.getReworkType())
                .eqIfPresent(MesProReworkOrderDO::getStatus, reqVO.getStatus())
                .eqIfPresent(MesProReworkOrderDO::getResponsiblePersonId, reqVO.getResponsiblePersonId())
                .betweenIfPresent(MesProReworkOrderDO::getPlannedStartTime, reqVO.getPlannedStartTime())
                .orderByDesc(MesProReworkOrderDO::getId));
    }

    default MesProReworkOrderDO selectByCode(String code) {
        return selectOne(MesProReworkOrderDO::getCode, code);
    }

    /**
     * 统计原工单已返工数量总和（仅统计非取消状态的返工工单）
     *
     * @param originalWorkOrderId 原工单 ID
     * @return 已返工数量总和（无记录时返回 0）
     */
    default BigDecimal selectReworkedQuantitySumByOriginalWorkOrderId(Long originalWorkOrderId) {
        List<MesProReworkOrderDO> list = selectList(new LambdaQueryWrapperX<MesProReworkOrderDO>()
                .eq(MesProReworkOrderDO::getOriginalWorkOrderId, originalWorkOrderId)
                .ne(MesProReworkOrderDO::getStatus, 40)); // 排除已取消
        BigDecimal sum = BigDecimal.ZERO;
        for (MesProReworkOrderDO item : list) {
            if (item.getReworkQuantity() != null) {
                sum = sum.add(item.getReworkQuantity());
            }
        }
        return sum;
    }

}
