package cn.zhicloud.module.mes.dal.mysql.pro.workorder;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.MesProWorkOrderPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * MES 生产工单 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesProWorkOrderMapper extends BaseMapperX<MesProWorkOrderDO> {

    default PageResult<MesProWorkOrderDO> selectPage(MesProWorkOrderPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProWorkOrderDO>()
                .likeIfPresent(MesProWorkOrderDO::getCode, reqVO.getCode())
                .likeIfPresent(MesProWorkOrderDO::getName, reqVO.getName())
                .eqIfPresent(MesProWorkOrderDO::getType, reqVO.getType())
                .likeIfPresent(MesProWorkOrderDO::getOrderSourceCode, reqVO.getOrderSourceCode())
                .eqIfPresent(MesProWorkOrderDO::getProductId, reqVO.getProductId())
                .eqIfPresent(MesProWorkOrderDO::getClientId, reqVO.getClientId())
                .eqIfPresent(MesProWorkOrderDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesProWorkOrderDO::getRequestDate, reqVO.getRequestDate())
                .orderByDesc(MesProWorkOrderDO::getId));
    }

    default MesProWorkOrderDO selectByCode(String code) {
        return selectOne(MesProWorkOrderDO::getCode, code);
    }

    /**
     * 累加工单已生产数量（带超产上限 CAS）
     * <p>
     * 通过 {@code WHERE ... AND IFNULL(quantity_produced, 0) + #{incr} <= quantity} 在数据库层原子校验，
     * 当累计产量超过工单计划产量时影响行数为 0，由 Service 层据此抛出超产异常。
     * 使用 {@code #{incr}} 参数绑定，避免 BigDecimal 拼接产生的科学计数法（如 1E+3）导致 SQL 异常。
     *
     * @return 影响行数（0 表示超产被拦截）
     */
    @Update("UPDATE mes_pro_work_order "
            + "SET quantity_produced = IFNULL(quantity_produced, 0) + #{incr} "
            + "WHERE id = #{id} AND IFNULL(quantity_produced, 0) + #{incr} <= quantity")
    int updateProducedQuantity(@Param("id") Long id, @Param("incr") BigDecimal incrQuantityProduced);

    default Long selectCountByVendorId(Long vendorId) {
        return selectCount(MesProWorkOrderDO::getVendorId, vendorId);
    }

}
