package cn.zhicloud.module.mes.service.pro.mrp;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpConvertLinkRespVO;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpConvertRespVO;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanCreateReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.mrp.vo.MesProMrpPlanPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.mrp.MesProMrpPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.mrp.MesProMrpResultDO;

import java.util.List;

/**
 * MES MRP 物料需求计划 Service 接口
 *
 * @author 智云
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
     * 生成计划订单建议（采购/生产类型在转建议时按 BOM 判定）
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

    /**
     * 确认 MRP 计划
     *
     * 仅「已计算」状态可确认；确认后计划锁定为「已确认」，不再可重新计算。
     *
     * @param id 计划编号
     */
    void confirmMrpPlan(Long id);

    /**
     * 转采购/生产建议
     *
     * 仅「已确认」状态可转；将结果行（计划订单量 > 0）逐行落地为草稿工单：
     * 有 BOM 的物料转生产型工单（自行生产），无 BOM 的物料转采购型工单（采购）。
     * 计划转为「已转建议」终态（一次性，不可重复转）。
     *
     * @param id 计划编号
     * @return 转换结果统计（总数 / 生产型 / 采购型）
     */
    MesProMrpConvertRespVO convertMrpPlan(Long id);

    /**
     * 获取 MRP 计划转建议的行级回链（结果行 → 工单）
     *
     * 回链于转建议时逐行落库（一次性转换）；历史已转计划无回链、返回空列表（不回填）。
     *
     * @param planId 计划编号
     * @return 行级回链列表（含工单编码/类型、产品名、计划订单量）
     */
    List<MesProMrpConvertLinkRespVO> getMrpConvertLinks(Long planId);

}
