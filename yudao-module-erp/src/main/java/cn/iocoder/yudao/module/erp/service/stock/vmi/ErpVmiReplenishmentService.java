package cn.iocoder.yudao.module.erp.service.stock.vmi;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vmi.vo.ErpVmiReplenishmentPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.vmi.ErpVmiReplenishmentDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.vmi.ErpVmiReplenishmentItemDO;

import java.util.List;

/**
 * ERP VMI 补货建议 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpVmiReplenishmentService {

    /**
     * 删除补货建议
     *
     * @param id 编号
     */
    void deleteVmiReplenishment(Long id);

    /**
     * 获得补货建议
     *
     * @param id 编号
     * @return 补货建议
     */
    ErpVmiReplenishmentDO getVmiReplenishment(Long id);

    /**
     * 获得补货建议明细列表
     *
     * @param replenishmentId 补货建议编号
     * @return 明细列表
     */
    List<ErpVmiReplenishmentItemDO> getVmiReplenishmentItemList(Long replenishmentId);

    /**
     * 获得补货建议分页
     *
     * @param pageReqVO 分页查询
     * @return 补货建议分页
     */
    PageResult<ErpVmiReplenishmentDO> getVmiReplenishmentPage(ErpVmiReplenishmentPageReqVO pageReqVO);

    /**
     * 生成补货建议：扫描低于补货点的 VMI 库存，按供应商+仓库分组生成补货建议
     *
     * @return 生成的补货建议编号列表
     */
    List<Long> generateReplenishment();

    /**
     * 将补货建议转换为采购订单
     *
     * @param id 补货建议编号
     * @return 生成的采购订单号
     */
    String convertToPurchaseOrder(Long id);

}
