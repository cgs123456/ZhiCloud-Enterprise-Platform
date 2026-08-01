package cn.iocoder.yudao.module.wms.dal.mysql.order.dock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.wms.controller.admin.order.dock.vo.WmsDockPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.dock.WmsDockDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * WMS 月台 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface WmsDockMapper extends BaseMapperX<WmsDockDO> {

    default PageResult<WmsDockDO> selectPage(WmsDockPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WmsDockDO>()
                .eqIfPresent(WmsDockDO::getWarehouseId, reqVO.getWarehouseId())
                .likeIfPresent(WmsDockDO::getDockCode, reqVO.getDockCode())
                .likeIfPresent(WmsDockDO::getDockName, reqVO.getDockName())
                .eqIfPresent(WmsDockDO::getDockType, reqVO.getDockType())
                .eqIfPresent(WmsDockDO::getStatus, reqVO.getStatus())
                .orderByAsc(WmsDockDO::getDockCode));
    }

    default WmsDockDO selectByDockCode(String dockCode) {
        return selectOne(WmsDockDO::getDockCode, dockCode);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(WmsDockDO::getWarehouseId, warehouseId);
    }

}
