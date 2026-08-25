package cn.zhicloud.module.erp.service.stock.vmi;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.stock.vmi.vo.ErpVmiInventoryPageReqVO;
import cn.zhicloud.module.erp.controller.admin.stock.vmi.vo.ErpVmiInventorySaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.stock.vmi.ErpVmiInventoryDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP VMI 供应商管理库存 Service 接口
 *
 * @author 智云
 */
public interface ErpVmiInventoryService {

    /**
     * 创建 VMI 库存
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createVmiInventory(@Valid ErpVmiInventorySaveReqVO createReqVO);

    /**
     * 更新 VMI 库存
     *
     * @param updateReqVO 更新信息
     */
    void updateVmiInventory(@Valid ErpVmiInventorySaveReqVO updateReqVO);

    /**
     * 删除 VMI 库存
     *
     * @param id 编号
     */
    void deleteVmiInventory(Long id);

    /**
     * 获得 VMI 库存
     *
     * @param id 编号
     * @return VMI 库存
     */
    ErpVmiInventoryDO getVmiInventory(Long id);

    /**
     * 获得 VMI 库存分页
     *
     * @param pageReqVO 分页查询
     * @return VMI 库存分页
     */
    PageResult<ErpVmiInventoryDO> getVmiInventoryPage(ErpVmiInventoryPageReqVO pageReqVO);

    /**
     * 检查补货点，返回需要补货的库存列表
     *
     * @return 需要补货的库存列表
     */
    List<ErpVmiInventoryDO> checkReplenishment();

}
