package cn.iocoder.yudao.module.mes.service.pro.mrp;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mrp.MesProMrpPlanDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.mrp.MesProMrpResultDO;

import java.util.List;

/**
 * MES MRP 物料需求计划 Service 接口
 *
 * @author 芋道源码
 */
public interface MesProMrpService {

    /**
     * 创建 MRP 计划
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createMrpPlan(MesProMrpPlanCreateReqVO createReqVO);

    /**
     * MRP 展算
     *
     * 输入：生产计划（已确认工单） → 产品 BOM 递归展开 → 各层级物料需求
     * 减去当前库存（调用 WMS 查询库存）
     * 净需求 = 需求量 - 库存量
     * 生成计划采购订单建议
     *
     * @param planId MRP 计划编号
     * @return 计算结果列表
     */
    List<MesProMrpResultDO> calculateMrp(Long planId);

    /**
     * 获取 MRP 计算结果
     *
     * @param planId MRP 计划编号
     * @return 计算结果列表
     */
    List<MesProMrpResultDO> getMrpResult(Long planId);

    /**
     * 获得 MRP 计划
     *
     * @param id 编号
     * @return MRP 计划
     */
    MesProMrpPlanDO getMrpPlan(Long id);

    /**
     * 获得 MRP 计划分页
     *
     * @param pageReqVO 分页查询
     * @return MRP 计划分页
     */
    PageResult<MesProMrpPlanDO> getMrpPlanPage(MesProMrpPlanPageReqVO pageReqVO);

}
