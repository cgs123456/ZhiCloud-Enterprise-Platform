package cn.iocoder.yudao.module.wms.service.inventory.alert;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.alert.vo.WmsInventoryAlertPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryAlertDO;

import java.util.List;

/**
 * WMS 库存预警 Service 接口
 *
 * @author 芋道源码
 */
public interface WmsInventoryAlertService {

    /**
     * 获得库存预警分页
     */
    PageResult<WmsInventoryAlertDO> getInventoryAlertPage(WmsInventoryAlertPageReqVO pageReqVO);

    /**
     * 获得库存预警
     */
    WmsInventoryAlertDO getInventoryAlert(Long id);

    /**
     * 创建库存预警（Job 调用）
     */
    Long createInventoryAlert(WmsInventoryAlertDO alert);

    /**
     * 批量创建库存预警（Job 调用）
     */
    void createInventoryAlertList(List<WmsInventoryAlertDO> alerts);

    /**
     * 确认预警
     */
    void confirmInventoryAlert(Long id);

    /**
     * 处理预警
     */
    void processInventoryAlert(Long id);

    /**
     * 校验库存预警存在
     */
    WmsInventoryAlertDO validateInventoryAlertExists(Long id);

}