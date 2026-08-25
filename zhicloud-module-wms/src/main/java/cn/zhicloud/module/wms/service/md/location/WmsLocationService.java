package cn.zhicloud.module.wms.service.md.location;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationPageReqVO;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * WMS 库位 Service 接口
 *
 * @author 智云
 */
public interface WmsLocationService {

    /**
     * 创建库位
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createLocation(@Valid WmsLocationSaveReqVO createReqVO);

    /**
     * 更新库位
     *
     * @param updateReqVO 更新信息
     */
    void updateLocation(@Valid WmsLocationSaveReqVO updateReqVO);

    /**
     * 删除库位
     *
     * @param id 编号
     */
    void deleteLocation(Long id);

    /**
     * 校验库位存在
     *
     * @param id 编号
     * @return 库位
     */
    WmsLocationDO validateLocationExists(Long id);

    /**
     * 获得库位
     *
     * @param id 编号
     * @return 库位
     */
    WmsLocationDO getLocation(Long id);

    /**
     * 获得库位分页
     *
     * @param pageReqVO 分页查询
     * @return 库位分页
     */
    PageResult<WmsLocationDO> getLocationPage(WmsLocationPageReqVO pageReqVO);

    /**
     * 获得库位列表
     *
     * @return 库位列表
     */
    List<WmsLocationDO> getLocationList();

    /**
     * 按库区 ID 获得库位列表
     *
     * @param zoneId 库区 ID
     * @return 库位列表
     */
    List<WmsLocationDO> getLocationListByZoneId(Long zoneId);

    /**
     * 按仓库 ID 获得库位列表
     *
     * @param warehouseId 仓库 ID
     * @return 库位列表
     */
    List<WmsLocationDO> getLocationListByWarehouseId(Long warehouseId);

    /**
     * 按编号集合获得库位列表
     *
     * @param ids 编号集合
     * @return 库位列表
     */
    List<WmsLocationDO> getLocationList(Collection<Long> ids);

}
