package cn.iocoder.yudao.module.erp.service.bi.inventory;

import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiAgingRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiInventoryTurnoverRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.bi.vo.ScmBiSlowMovingRespVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 供应链 BI 库存分析 Service 接口
 *
 * @author 芋道源码
 */
public interface ScmInventoryBiService {

    /**
     * 获取库存周转率 = 出库总成本 / 平均库存金额
     *
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return 库存周转率
     */
    ScmBiInventoryTurnoverRespVO getInventoryTurnoverRate(LocalDateTime beginTime, LocalDateTime endTime);

    /**
     * 库龄分析
     *
     * @return 库龄分析列表
     */
    List<ScmBiAgingRespVO> getInventoryAging();

    /**
     * 呆滞库存分析
     *
     * @param idleDays 停滞天数阈值
     * @return 呆滞库存列表
     */
    List<ScmBiSlowMovingRespVO> getSlowMovingInventory(Integer idleDays);

}
