package cn.zhicloud.module.wms.service.md.zone;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.md.zone.vo.WmsZonePageReqVO;
import cn.zhicloud.module.wms.controller.admin.md.zone.vo.WmsZoneSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.zhicloud.module.wms.dal.dataobject.md.zone.WmsZoneDO;
import cn.zhicloud.module.wms.dal.mysql.md.zone.WmsZoneMapper;
import cn.zhicloud.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.zhicloud.module.wms.service.md.warehouse.WmsWarehouseService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.wms.enums.ErrorCodeConstants.*;

/**
 * WMS 库区 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class WmsZoneServiceImpl implements WmsZoneService {

    @Resource
    private WmsZoneMapper zoneMapper;

    @Resource
    private WmsLocationMapper locationMapper;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private WmsWarehouseService warehouseService;

    @Override
    public Long createZone(WmsZoneSaveReqVO createReqVO) {
        // 校验数据
        validateZoneSaveData(createReqVO);

        // 插入
        WmsZoneDO zone = BeanUtils.toBean(createReqVO, WmsZoneDO.class);
        zoneMapper.insert(zone);
        return zone.getId();
    }

    @Override
    public void updateZone(WmsZoneSaveReqVO updateReqVO) {
        // 校验存在
        validateZoneExists(updateReqVO.getId());
        // 校验数据
        validateZoneSaveData(updateReqVO);

        // 更新
        WmsZoneDO updateObj = BeanUtils.toBean(updateReqVO, WmsZoneDO.class);
        zoneMapper.updateById(updateObj);
    }

    private void validateZoneSaveData(WmsZoneSaveReqVO reqVO) {
        // 校验仓库存在
        WmsWarehouseDO warehouse = warehouseService.validateWarehouseExists(reqVO.getWarehouseId());
        // 校验编码唯一（同一仓库下）
        validateZoneCodeUnique(reqVO.getId(), reqVO.getWarehouseId(), reqVO.getCode());
        // 校验名称唯一（同一仓库下）
        validateZoneNameUnique(reqVO.getId(), reqVO.getWarehouseId(), reqVO.getName());
    }

    private void validateZoneCodeUnique(Long id, Long warehouseId, String code) {
        WmsZoneDO zone = zoneMapper.selectByCode(warehouseId, code);
        if (zone == null) {
            return;
        }
        if (ObjUtil.notEqual(zone.getId(), id)) {
            throw exception(ZONE_CODE_DUPLICATE);
        }
    }

    private void validateZoneNameUnique(Long id, Long warehouseId, String name) {
        WmsZoneDO zone = zoneMapper.selectByName(warehouseId, name);
        if (zone == null) {
            return;
        }
        if (ObjUtil.notEqual(zone.getId(), id)) {
            throw exception(ZONE_NAME_DUPLICATE);
        }
    }

    @Override
    public void deleteZone(Long id) {
        // 校验存在
        validateZoneExists(id);
        // 校验库区下不存在库位
        if (locationMapper.selectCountByZoneId(id) > 0) {
            throw exception(ZONE_HAS_LOCATIONS);
        }

        // 删除
        zoneMapper.deleteById(id);
    }

    @Override
    public WmsZoneDO validateZoneExists(Long id) {
        WmsZoneDO zone = zoneMapper.selectById(id);
        if (zone == null) {
            throw exception(ZONE_NOT_EXISTS);
        }
        return zone;
    }

    @Override
    public WmsZoneDO getZone(Long id) {
        return zoneMapper.selectById(id);
    }

    @Override
    public PageResult<WmsZoneDO> getZonePage(WmsZonePageReqVO pageReqVO) {
        return zoneMapper.selectPage(pageReqVO);
    }

    @Override
    public List<WmsZoneDO> getZoneList() {
        return zoneMapper.selectList();
    }

    @Override
    public List<WmsZoneDO> getZoneListByWarehouseId(Long warehouseId) {
        return zoneMapper.selectListByWarehouseId(warehouseId);
    }

    @Override
    public List<WmsZoneDO> getZoneList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return zoneMapper.selectByIds(ids);
    }

}
