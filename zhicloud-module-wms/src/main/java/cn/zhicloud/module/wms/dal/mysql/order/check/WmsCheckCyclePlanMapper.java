package cn.zhicloud.module.wms.dal.mysql.order.check;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.controller.admin.order.check.vo.cycle.WmsCheckCyclePlanPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.order.check.WmsCheckCyclePlanDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * WMS 循环盘点计划 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsCheckCyclePlanMapper extends BaseMapperX<WmsCheckCyclePlanDO> {

    default PageResult<WmsCheckCyclePlanDO> selectPage(WmsCheckCyclePlanPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsCheckCyclePlanDO>()
                .eqIfPresent(WmsCheckCyclePlanDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsCheckCyclePlanDO::getAbcClassification, reqVO.getAbcClassification())
                .eqIfPresent(WmsCheckCyclePlanDO::getEnabled, reqVO.getEnabled())
                .orderByDesc(WmsCheckCyclePlanDO::getId));
    }

    default WmsCheckCyclePlanDO selectByWarehouseAndAbc(Long warehouseId, String abcClassification) {
        return selectOne(new LambdaQueryWrapperX<WmsCheckCyclePlanDO>()
                .eq(WmsCheckCyclePlanDO::getWarehouseId, warehouseId)
                .eq(WmsCheckCyclePlanDO::getAbcClassification, abcClassification));
    }

    default List<WmsCheckCyclePlanDO> selectListDueToday(LocalDate today) {
        return selectList(new LambdaQueryWrapperX<WmsCheckCyclePlanDO>()
                .le(WmsCheckCyclePlanDO::getNextCheckDate, today)
                .eq(WmsCheckCyclePlanDO::getEnabled, 1));
    }

    default List<WmsCheckCyclePlanDO> selectListAllEnabled() {
        return selectList(new LambdaQueryWrapperX<WmsCheckCyclePlanDO>()
                .eq(WmsCheckCyclePlanDO::getEnabled, 1));
    }

}