package cn.zhicloud.module.wms.dal.mysql.md.zone;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.controller.admin.md.zone.vo.WmsZonePageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.zone.WmsZoneDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * WMS 库区 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsZoneMapper extends BaseMapperX<WmsZoneDO> {

    default PageResult<WmsZoneDO> selectPage(WmsZonePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsZoneDO>()
                .eqIfPresent(WmsZoneDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsZoneDO::getCode, reqVO.getCode())
                .likeIfPresent(WmsZoneDO::getName, reqVO.getName())
                .eqIfPresent(WmsZoneDO::getType, reqVO.getType())
                .orderByAsc(WmsZoneDO::getSort)
                .orderByDesc(WmsZoneDO::getId));
    }

    default WmsZoneDO selectByCode(Long warehouseId, String code) {
        return selectOne(new LambdaQueryWrapperX<WmsZoneDO>()
                .eq(WmsZoneDO::getWarehouseId, warehouseId)
                .eq(WmsZoneDO::getCode, code));
    }

    default WmsZoneDO selectByName(Long warehouseId, String name) {
        return selectOne(new LambdaQueryWrapperX<WmsZoneDO>()
                .eq(WmsZoneDO::getWarehouseId, warehouseId)
                .eq(WmsZoneDO::getName, name));
    }

    default List<WmsZoneDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<WmsZoneDO>()
                .eqIfPresent(WmsZoneDO::getWarehouseId, warehouseId)
                .orderByAsc(WmsZoneDO::getSort)
                .orderByDesc(WmsZoneDO::getId));
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(WmsZoneDO::getWarehouseId, warehouseId);
    }

}
