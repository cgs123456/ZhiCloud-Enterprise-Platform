package cn.zhicloud.module.wms.service.md.location;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationPageReqVO;
import cn.zhicloud.module.wms.controller.admin.md.location.vo.WmsLocationSaveReqVO;
import cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.dataobject.md.zone.WmsZoneDO;
import cn.zhicloud.module.wms.dal.mysql.inventory.WmsInventoryMapper;
import cn.zhicloud.module.wms.dal.mysql.md.location.WmsLocationMapper;
import cn.zhicloud.module.wms.enums.md.WmsLocationStatusEnum;
import cn.zhicloud.module.wms.service.md.zone.WmsZoneService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    private WmsInventoryMapper inventoryMapper; // V205：库存行计数/冲洗直查（ADR 259 D9，免 service 循环依赖）

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
        // 校验状态取值合法（IDLE/OCCUPIED/LOCKED/DISABLED）
        validateLocationStatus(reqVO.getStatus());
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

    private void validateLocationStatus(Integer status) {
        if (!Arrays.asList(WmsLocationStatusEnum.ARRAYS).contains(status)) {
            throw exception(LOCATION_STATUS_INVALID);
        }
    }

    @Override
    public void deleteLocation(Long id) {
        // 校验存在
        validateLocationExists(id);
        // 库位库存校验（V205 库位维度接入，ADR 259 D9；休眠码 LOCATION_HAS_INVENTORY 激活收口）：
        // 有货（quantity > 0）或持有锁/冻占用（locked/frozen > 0）的行拒删；
        // 纯零行（三维全 0）物理冲洗——锁冻成立要求 available ≥ 冲减量 > 0，纯零行不可能持有占用。
        List<WmsInventoryDO> stockRows = inventoryMapper.selectListByLocationId(id);
        for (WmsInventoryDO row : stockRows) {
            BigDecimal quantity = row.getQuantity() == null ? BigDecimal.ZERO : row.getQuantity();
            BigDecimal locked = row.getLockedQuantity() == null ? BigDecimal.ZERO : row.getLockedQuantity();
            BigDecimal frozen = row.getFrozenQuantity() == null ? BigDecimal.ZERO : row.getFrozenQuantity();
            if (quantity.compareTo(BigDecimal.ZERO) > 0
                    || locked.compareTo(BigDecimal.ZERO) > 0
                    || frozen.compareTo(BigDecimal.ZERO) > 0) {
                throw exception(LOCATION_HAS_INVENTORY);
            }
        }
        if (CollUtil.isNotEmpty(stockRows)) {
            // 物理删除走 @Delete 原生 SQL 绕过 @TableLogic：逻辑删留下的 (sku, warehouse, location)
            // tombstone 会占用三元唯一键，导致同位置重上架撞键（V205 唯一键三元扩展）
            List<Long> washingIds = new ArrayList<>();
            for (WmsInventoryDO row : stockRows) {
                washingIds.add(row.getId());
            }
            inventoryMapper.deleteByIdListPhysical(washingIds);
        }

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
    public WmsLocationDO validateLocationUsable(Long id) {
        WmsLocationDO location = validateLocationExists(id);
        // 锁定/禁用库位视为冻结：禁止货物进出（IDLE/OCCUPIED 正常放行）
        if (WmsLocationStatusEnum.LOCKED.getStatus().equals(location.getStatus())) {
            throw exception(LOCATION_LOCKED);
        }
        if (WmsLocationStatusEnum.DISABLED.getStatus().equals(location.getStatus())) {
            throw exception(LOCATION_DISABLED);
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

    @Override
    public Set<Long> selectFrozenLocationIds(Collection<Long> locationIds) {
        if (CollUtil.isEmpty(locationIds)) {
            return Collections.emptySet();
        }
        // LOCKED/DISABLED = 库存分配与主行选择共同排除的冻结面（ADR 259 D3/D8）
        Set<Long> frozenLocationIds = new java.util.LinkedHashSet<>();
        for (WmsLocationDO location : locationMapper.selectByIds(locationIds)) {
            if (WmsLocationStatusEnum.LOCKED.getStatus().equals(location.getStatus())
                    || WmsLocationStatusEnum.DISABLED.getStatus().equals(location.getStatus())) {
                frozenLocationIds.add(location.getId());
            }
        }
        return frozenLocationIds;
    }

    @Override
    public void refreshOccupancyStatus(Collection<Long> locationIds) {
        if (CollUtil.isEmpty(locationIds)) {
            return;
        }
        for (Long locationId : locationIds) {
            // 0 = 未定位哨兵（ADR 259 D1/D4）；库位被删的悬挂行跳过
            if (locationId == null || locationId == 0L) {
                continue;
            }
            WmsLocationDO location = locationMapper.selectById(locationId);
            if (location == null) {
                continue;
            }
            // 管理态优先于派生：LOCKED/DISABLED 为人工裁决，派生只负责 空闲/占用 互换
            if (WmsLocationStatusEnum.LOCKED.getStatus().equals(location.getStatus())
                    || WmsLocationStatusEnum.DISABLED.getStatus().equals(location.getStatus())) {
                continue;
            }
            Long stockedCount = inventoryMapper.selectCountByLocationIdAndQuantityPositive(locationId);
            Integer desiredStatus = stockedCount != null && stockedCount > 0
                    ? WmsLocationStatusEnum.OCCUPIED.getStatus()
                    : WmsLocationStatusEnum.IDLE.getStatus();
            if (!desiredStatus.equals(location.getStatus())) {
                locationMapper.updateById(new WmsLocationDO().setId(locationId).setStatus(desiredStatus));
            }
        }
    }

}
