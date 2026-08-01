package cn.iocoder.yudao.module.erp.service.production.mps;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.production.mps.vo.ErpMpsPlanGenerateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.mps.vo.ErpMpsPlanPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.mps.vo.ErpMpsPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.mps.ErpMpsPlanDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.mps.ErpMpsPlanDetailDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * ERP 主生产计划 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpMpsPlanService {

    /**
     * 创建主生产计划
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMpsPlan(@Valid ErpMpsPlanSaveReqVO createReqVO);

    /**
     * 更新主生产计划
     *
     * @param updateReqVO 更新信息
     */
    void updateMpsPlan(@Valid ErpMpsPlanSaveReqVO updateReqVO);

    /**
     * 删除主生产计划
     *
     * @param ids 编号数组
     */
    void deleteMpsPlan(List<Long> ids);

    /**
     * 获得主生产计划
     *
     * @param id 编号
     * @return 主生产计划
     */
    ErpMpsPlanDO getMpsPlan(Long id);

    /**
     * 获得主生产计划分页
     *
     * @param pageReqVO 分页查询
     * @return 主生产计划分页
     */
    PageResult<ErpMpsPlanDO> getMpsPlanPage(ErpMpsPlanPageReqVO pageReqVO);

    /**
     * 生成 MPS 计划（核心算法）
     *
     * 根据销售订单 + 预测 + 安全库存生成 MPS 计划：
     * 1. 毛需求 = max(销售订单, 预测)（取大值策略，保守）
     * 2. 计划接收 = 已下达订单的预计入库
     * 3. 预计可用库存 = 期初库存 + 计划接收 - 毛需求
     * 4. 当预计可用库存 < 安全库存时，生成计划订单
     * 5. 计划订单下达时间 = 计划订单接收 - 提前期
     *
     * @param reqVO 生成参数
     * @return 计划编号
     */
    Long generateMpsPlan(@Valid ErpMpsPlanGenerateReqVO reqVO);

    /**
     * 确认计划：状态 10 草稿 -> 20 已确认
     *
     * @param id 计划编号
     */
    void confirmPlan(Long id);

    /**
     * 下发 MRP：状态 20 已确认 -> 30 已下发 MRP
     *
     * 触发 MRP 运算（如 MRP Service 可调用，则调用）
     *
     * @param id 计划编号
     */
    void releaseToMrp(Long id);

    /**
     * 关闭计划：状态 30 已下发 MRP -> 40 已关闭
     *
     * @param id 计划编号
     */
    void closePlan(Long id);

    /**
     * 校验主生产计划存在
     *
     * @param id 编号
     * @return 主生产计划
     */
    ErpMpsPlanDO validateMpsPlan(Long id);

    /**
     * 获得主生产计划明细列表
     *
     * @param planId 计划编号
     * @return 明细列表
     */
    List<ErpMpsPlanDetailDO> getMpsPlanDetailListByPlanId(Long planId);

}