package cn.iocoder.yudao.module.wms.service.inventory.batch;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.wms.controller.admin.inventory.batch.vo.WmsBatchExpiryAlertPageReqVO;
import cn.iocoder.yudao.module.wms.dal.dataobject.inventory.WmsInventoryAlertDO;

/**
 * WMS 批次效期预警 Service 接口
 *
 * <p>负责扫描批次效期、更新批次状态、生成效期预警记录。
 *
 * @author 芋道源码
 */
public interface WmsBatchExpiryAlertService {

    /**
     * 扫描所有临期/已过期批次，更新批次状态并生成预警
     *
     * <p>计算逻辑：
     * <ul>
     *   <li>距过期 ≤ 30 天 → 临期预警（NEAR_EXPIRY）</li>
     *   <li>已过期 → 已过期状态（EXPIRED）</li>
     *   <li>距过期 > 30 天 → 正常（AVAILABLE，重置曾经的预警状态）</li>
     *   <li>已冻结批次不参与扫描</li>
     * </ul>
     *
     * @return 生成预警条数
     */
    int scanExpiryAlerts();

    /**
     * 分页查询批次效期预警
     *
     * @param pageReqVO 分页查询条件
     * @return 预警分页
     */
    PageResult<WmsInventoryAlertDO> getExpiryAlertPage(WmsBatchExpiryAlertPageReqVO pageReqVO);

}
