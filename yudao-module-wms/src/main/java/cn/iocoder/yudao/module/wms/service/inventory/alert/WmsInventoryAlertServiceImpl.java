package cn.iocoder.yudao.module.wms.service.inventory.alert;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo.WmsInventoryAlertPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryAlertDO;
import cn.iocoder.yudao.module.wms.dal.mysql.inventory.WmsInventoryAlertMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.INVENTORY_ALERT_NOT_EXISTS;
import static cn.iocoder.yudao.module.wms.enums.ErrorCodeConstants.INVENTORY_ALERT_STATUS_INVALID;

/**
 * WMS 库存预警 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class WmsInventoryAlertServiceImpl implements WmsInventoryAlertService {

    private static final int STATUS_UNPROCESSED = 0;
    private static final int STATUS_CONFIRMED = 1;
    private static final int STATUS_PROCESSED = 2;

    @Resource
    private WmsInventoryAlertMapper inventoryAlertMapper;

    @Override
    public PageResult<WmsInventoryAlertDO> getInventoryAlertPage(WmsInventoryAlertPageReqVO pageReqVO) {
        return inventoryAlertMapper.selectPage(pageReqVO);
    }

    @Override
    public WmsInventoryAlertDO getInventoryAlert(Long id) {
        return inventoryAlertMapper.selectById(id);
    }

    @Override
    public Long createInventoryAlert(WmsInventoryAlertDO alert) {
        if (alert.getStatus() == null) {
            alert.setStatus(STATUS_UNPROCESSED);
        }
        inventoryAlertMapper.insert(alert);
        return alert.getId();
    }

    @Override
    public void createInventoryAlertList(List<WmsInventoryAlertDO> alerts) {
        if (CollUtil.isEmpty(alerts)) {
            return;
        }
        alerts.forEach(alert -> {
            if (alert.getStatus() == null) {
                alert.setStatus(STATUS_UNPROCESSED);
            }
        });
        inventoryAlertMapper.insertBatch(alerts);
    }

    @Override
    public void confirmInventoryAlert(Long id) {
        WmsInventoryAlertDO alert = validateInventoryAlertExists(id);
        if (!ObjectUtil.equal(alert.getStatus(), STATUS_UNPROCESSED)) {
            throw exception(INVENTORY_ALERT_STATUS_INVALID);
        }
        WmsInventoryAlertDO updateObj = new WmsInventoryAlertDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_CONFIRMED);
        inventoryAlertMapper.updateById(updateObj);
    }

    @Override
    public void processInventoryAlert(Long id) {
        WmsInventoryAlertDO alert = validateInventoryAlertExists(id);
        if (ObjectUtil.notEqual(alert.getStatus(), STATUS_CONFIRMED)) {
            throw exception(INVENTORY_ALERT_STATUS_INVALID);
        }
        WmsInventoryAlertDO updateObj = new WmsInventoryAlertDO();
        updateObj.setId(id);
        updateObj.setStatus(STATUS_PROCESSED);
        inventoryAlertMapper.updateById(updateObj);
    }

    @Override
    public WmsInventoryAlertDO validateInventoryAlertExists(Long id) {
        WmsInventoryAlertDO alert = inventoryAlertMapper.selectById(id);
        if (alert == null) {
            throw exception(INVENTORY_ALERT_NOT_EXISTS);
        }
        return alert;
    }

}