package cn.zhicloud.module.wms.service.md.zone;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.wms.controller.admin.md.zone.vo.WmsZonePageReqVO;
import cn.zhicloud.module.wms.controller.admin.md.zone.vo.WmsZoneSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.zone.WmsZoneDO;
import jakarta.validation.Valid;

import java.util.Collection;
import java.util.List;

/**
 * WMS 库区 Service 接口
 *
 * @author 智云
 */
public interface WmsZoneService {

    /**
     * 创建库区
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createZone(@Valid WmsZoneSaveReqVO createReqVO);

    /**
     * 更新库区
     *
     * @param updateReqVO 更新信息
     */
    void updateZone(@Valid WmsZoneSaveReqVO updateReqVO);

    /**
     * 删除库区
     *
     * @param id 编号
     */
    void deleteZone(Long id);

    /**
     * 校验库区存在
     *
     * @param id 编号
     * @return 库区
     */
    WmsZoneDO validateZoneExists(Long id);

    /**
     * 获得库区
     *
     * @param id 编号
     * @return 库区
     */
    WmsZoneDO getZone(Long id);

    /**
     * 获得库区分页
     *
     * @param pageReqVO 分页查询
     * @return 库区分页
     */
    PageResult<WmsZoneDO> getZonePage(WmsZonePageReqVO pageReqVO);

    /**
     * 获得库区列表
     *
     * @return 库区列表
     */
    List<WmsZoneDO> getZoneList();

    /**
     * 按仓库 ID 获得库区列表
     *
     * @param warehouseId 仓库 ID
     * @return 库区列表
     */
    List<WmsZoneDO> getZoneListByWarehouseId(Long warehouseId);

    /**
     * 按编号集合获得库区列表
     *
     * @param ids 编号集合
     * @return 库区列表
     */
    List<WmsZoneDO> getZoneList(Collection<Long> ids);

}
