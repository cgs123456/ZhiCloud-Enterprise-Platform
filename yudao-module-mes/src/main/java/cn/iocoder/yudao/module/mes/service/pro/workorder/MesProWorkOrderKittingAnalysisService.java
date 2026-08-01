package cn.iocoder.yudao.module.mes.service.pro.workorder;

import cn.iocoder.yudao.module.mes.controller.admin.pro.workorder.vo.kitting.MesProWorkOrderKittingSummaryRespVO;

import jakarta.validation.Valid;

/**
 * MES 工单齐套分析 Service 接口（P0-5）
 *
 * <p>对生产工单进行齐套分析：基于工单 BOM 展开物料需求，结合当前库存 + 在途数量，
 * 计算每个 BOM 行的齐套状态（齐套 / 部分齐套 / 短缺），输出整体齐套率与缺口明细。
 *
 * <h3>核心计算公式</h3>
 * <ul>
 *   <li>物料总需求量 = BOM 单位用量 × 工单生产数量</li>
 *   <li>可用量 = 当前未冻结库存 + 在途数量（待入库的采购入库单行）</li>
 *   <li>缺口量 = max(0, 需求量 - 可用量)</li>
 *   <li>齐套状态：
 *     <ul>
 *       <li>齐套：库存 ≥ 需求量</li>
 *       <li>部分齐套：库存 &lt; 需求量 ≤ 库存 + 在途</li>
 *       <li>短缺：库存 + 在途 &lt; 需求量</li>
 *     </ul>
 *   </li>
 *   <li>齐套率 = (齐套 + 部分齐套) / 总行数 × 100</li>
 * </ul>
 *
 * @author yudao
 */
public interface MesProWorkOrderKittingAnalysisService {

    /**
     * 工单齐套分析
     *
     * @param workOrderId 工单编号
     * @return 齐套分析汇总（含 BOM 行明细）
     */
    MesProWorkOrderKittingSummaryRespVO analyzeKitting(@Valid Long workOrderId);

}
