package cn.zhicloud.module.erp.service.production.mrp;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.production.mrp.vo.ErpMrpPlanPageReqVO;
import cn.zhicloud.module.erp.controller.admin.production.mrp.vo.ErpMrpPlanSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.production.mrp.ErpMrpPlanDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 物料需求计划 Service 接口
 *
 * @author 智云
 */
public interface ErpMrpPlanService {

    /**
     * 创建 MRP 计划
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMrpPlan(@Valid ErpMrpPlanSaveReqVO createReqVO);

    /**
     * 更新 MRP 计划
     *
     * @param updateReqVO 更新信息
     */
    void updateMrpPlan(@Valid ErpMrpPlanSaveReqVO updateReqVO);

    /**
     * 删除 MRP 计划
     *
     * @param ids 编号数组
     */
    void deleteMrpPlan(List<Long> ids);

    /**
     * 获得 MRP 计划
     *
     * @param id 编号
     * @return MRP 计划
     */
    ErpMrpPlanDO getMrpPlan(Long id);

    /**
     * 获得 MRP 计划分页
     *
     * @param pageReqVO 分页查询
     * @return MRP 计划分页
     */
    PageResult<ErpMrpPlanDO> getMrpPlanPage(ErpMrpPlanPageReqVO pageReqVO);

    /**
     * 执行 MRP 计算
     *
     * <p>计算逻辑：
     * 1. 读取关联 MPS 计划及其明细，作为独立需求
     * 2. 按产品展开 BOM（通过 {@code ErpBomProvider}），计算相关需求
     * 3. 减去当前库存（{@code ErpStockService}）
     * 4. 生成净需求和计划订单（采购 / 生产）
     * 5. 结果写入 {@code ErpMrpResultDO}
     *
     * @param id MRP 计划编号
     */
    void executeMrp(Long id);

    /**
     * 确认 MRP 计划：状态 20 已计算 -> 30 已确认
     *
     * @param id MRP 计划编号
     */
    void confirmMrpPlan(Long id);

    /**
     * 校验 MRP 计划存在
     *
     * @param id 编号
     * @return MRP 计划
     */
    ErpMrpPlanDO validateMrpPlan(Long id);

}
