package cn.zhicloud.module.wms.dal.mysql.md.location;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationPageReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * WMS 库位 Mapper
 *
 * @author 智云
 */
@Mapper
public interface WmsLocationMapper extends BaseMapperX<WmsLocationDO> {

    default PageResult<WmsLocationDO> selectPage(WmsLocationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsLocationDO>()
                .eqIfPresent(WmsLocationDO::getWarehouseId, reqVO.getWarehouseId())
                .eqIfPresent(WmsLocationDO::getZoneId, reqVO.getZoneId())
                .eqIfPresent(WmsLocationDO::getCode, reqVO.getCode())
                .likeIfPresent(WmsLocationDO::getName, reqVO.getName())
                .eqIfPresent(WmsLocationDO::getBarcode, reqVO.getBarcode())
                .eqIfPresent(WmsLocationDO::getType, reqVO.getType())
                .eqIfPresent(WmsLocationDO::getStatus, reqVO.getStatus())
                .orderByAsc(WmsLocationDO::getSort)
                .orderByDesc(WmsLocationDO::getId));
    }

    default WmsLocationDO selectByCode(Long zoneId, String code) {
        return selectOne(new LambdaQueryWrapperX<WmsLocationDO>()
                .eq(WmsLocationDO::getZoneId, zoneId)
                .eq(WmsLocationDO::getCode, code));
    }

    default WmsLocationDO selectByBarcode(String barcode) {
        return selectOne(WmsLocationDO::getBarcode, barcode);
    }

    default List<WmsLocationDO> selectListByZoneId(Long zoneId) {
        return selectList(new LambdaQueryWrapperX<WmsLocationDO>()
                .eqIfPresent(WmsLocationDO::getZoneId, zoneId)
                .orderByAsc(WmsLocationDO::getSort)
                .orderByDesc(WmsLocationDO::getId));
    }

    default List<WmsLocationDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<WmsLocationDO>()
                .eqIfPresent(WmsLocationDO::getWarehouseId, warehouseId)
                .orderByAsc(WmsLocationDO::getSort)
                .orderByDesc(WmsLocationDO::getId));
    }

    default Long selectCountByZoneId(Long zoneId) {
        return selectCount(WmsLocationDO::getZoneId, zoneId);
    }

}
