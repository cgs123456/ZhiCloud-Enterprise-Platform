package cn.iocoder.yudao.module.erp.service.finance.cost;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costitem.ErpCostItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costitem.ErpCostItemSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpCostItemDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 成本项目 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpCostItemService {

    /**
     * 创建成本项目
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createCostItem(@Valid ErpCostItemSaveReqVO createReqVO);

    /**
     * 更新成本项目
     *
     * @param updateReqVO 更新信息
     */
    void updateCostItem(@Valid ErpCostItemSaveReqVO updateReqVO);

    /**
     * 删除成本项目
     *
     * @param id 编号
     */
    void deleteCostItem(Long id);

    /**
     * 获得成本项目
     *
     * @param id 编号
     * @return 成本项目
     */
    ErpCostItemDO getCostItem(Long id);

    /**
     * 获得成本项目分页
     *
     * @param pageReqVO 分页查询
     * @return 成本项目分页
     */
    PageResult<ErpCostItemDO> getCostItemPage(ErpCostItemPageReqVO pageReqVO);

    /**
     * 获得启用的成本项目列表
     *
     * @return 成本项目列表
     */
    List<ErpCostItemDO> getCostItemListByStatus(Integer status);

}
