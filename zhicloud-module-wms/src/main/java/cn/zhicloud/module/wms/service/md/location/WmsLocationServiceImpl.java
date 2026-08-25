package cn.zhicloud.module.wms.service.md.location;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationPageReqVO;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.dataobject.md.zone.WmsZoneDO;
import cn.zhicloud.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.zhicloud.module.wms.service.md.zone.WmsZoneService;
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
 * WMS 库位 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class WmsLocationServiceImpl implements WmsLocationService {

    @Resource
    private WmsLocationMapper locationMapper;

    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private WmsZoneService zoneService;

    @Override
    public Long createLocation(WmsLocationSaveReqVO createReqVO) {
        // 校验数据
        validateLocationSaveData(createReqVO);

        // 插入
        WmsLocationDO location = BeanUtils.toBean(createReqVO, WmsLocationDO.class);
        locationMapper.insert(location);
        return location.getId();
    }

    @Override
    public void updateLocation(WmsLocationSaveReqVO updateReqVO) {
        // 校验存在
        validateLocationExists(updateReqVO.getId());
        // 校验数据
        validateLocationSaveData(updateReqVO);

        // 更新
        WmsLocationDO updateObj = BeanUtils.toBean(updateReqVO, WmsLocationDO.class);
        locationMapper.updateById(updateObj);
    }

    private void validateLocationSaveData(WmsLocationSaveReqVO reqVO) {
        // 校验库区存在，并以库区所属仓库为准回填 warehouseId
        WmsZoneDO zone = zoneService.validateZoneExists(reqVO.getZoneId());
        reqVO.setWarehouseId(zone.getWarehouseId());
        // 校验编码唯一（同一库区下）
        validateLocationCodeUnique(reqVO.getId(), reqVO.getZoneId(), reqVO.getCode());
        // 校验条码唯一
        validateLocationBarcodeUnique(reqVO.getId(), reqVO.getBarcode());
    }

    private void validateLocationCodeUnique(Long id, Long zoneId, String code) {
        WmsLocationDO location = locationMapper.selectByCode(zoneId, code);
        if (location == null) {
            return;
        }
        if (ObjUtil.notEqual(location.getId(), id)) {
            throw exception(LOCATION_CODE_DUPLICATE);
        }
    }

    private void validateLocationBarcodeUnique(Long id, String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            return;
        }
        WmsLocationDO location = locationMapper.selectByBarcode(barcode);
        if (location == null) {
            return;
        }
        if (ObjUtil.notEqual(location.getId(), id)) {
            throw exception(LOCATION_BARCODE_DUPLICATE);
        }
    }

    @Override
    public void deleteLocation(Long id) {
        // 校验存在
        validateLocationExists(id);
        // TODO 库位库存校验：当前 WmsInventoryService 暂未提供按库位统计库存的方法，
        //      待库存模型扩展 location_id 后，在此处补充 LOCATION_HAS_INVENTORY 校验。

        // 删除
        locationMapper.deleteById(id);
    }

    @Override
    public WmsLocationDO validateLocationExists(Long id) {
        WmsLocationDO location = locationMapper.selectById(id);
        if (location == null) {
            throw exception(LOCATION_NOT_EXISTS);
        }
        return location;
    }

    @Override
    public WmsLocationDO getLocation(Long id) {
        return locationMapper.selectById(id);
    }

    @Override
    public PageResult<WmsLocationDO> getLocationPage(WmsLocationPageReqVO pageReqVO) {
        return locationMapper.selectPage(pageReqVO);
    }

    @Override
    public List<WmsLocationDO> getLocationList() {
        return locationMapper.selectList();
    }

    @Override
    public List<WmsLocationDO> getLocationListByZoneId(Long zoneId) {
        return locationMapper.selectListByZoneId(zoneId);
    }

    @Override
    public List<WmsLocationDO> getLocationListByWarehouseId(Long warehouseId) {
        return locationMapper.selectListByWarehouseId(warehouseId);
    }

    @Override
    public List<WmsLocationDO> getLocationList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return locationMapper.selectByIds(ids);
    }

}
