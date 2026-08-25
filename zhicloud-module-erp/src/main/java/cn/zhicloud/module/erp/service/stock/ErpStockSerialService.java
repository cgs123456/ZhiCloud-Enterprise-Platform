package cn.zhicloud.module.erp.service.stock;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stockserial.ErpStockSerialPageReqVO;
import cn.zhicloud.module.erp.controller.admin.stock.vo.stockserial.ErpStockSerialSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.ErpStockSerialDO;
import jakarta.validation.Valid;

/**
 * ERP 库存序列号 Service 接口
 *
 * @author 智云
 */
public interface ErpStockSerialService {

    /**
     * 创建库存序列号
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStockSerial(@Valid ErpStockSerialSaveReqVO createReqVO);

    /**
     * 更新库存序列号
     *
     * @param updateReqVO 更新信息
     */
    void updateStockSerial(@Valid ErpStockSerialSaveReqVO updateReqVO);

    /**
     * 删除库存序列号
     *
     * @param id 编号
     */
    void deleteStockSerial(Long id);

    /**
     * 获得库存序列号
     *
     * @param id 编号
     * @return 库存序列号
     */
    ErpStockSerialDO getStockSerial(Long id);

    /**
     * 通过序列号扫码查询
     *
     * @param serialNo 序列号
     * @return 库存序列号
     */
    ErpStockSerialDO getStockSerialBySerialNo(String serialNo);

    /**
     * 获得库存序列号分页
     *
     * @param pageReqVO 分页查询
     * @return 库存序列号分页
     */
    PageResult<ErpStockSerialDO> getStockSerialPage(ErpStockSerialPageReqVO pageReqVO);

}
