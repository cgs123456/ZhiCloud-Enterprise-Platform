package cn.zhicloud.module.mes.service.pro.rework;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderCreateReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderPageReqVO;
import cn.zhicloud.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.pro.rework.MesProReworkOrderDO;
import jakarta.validation.Valid;

/**
 * MES 返工工单 Service 接口
 *
 * @author 智云
 */
public interface MesProReworkOrderService {

    /**
     * 基于原工单创建返工工单
     *
     * 校验：原工单状态为 FINISHED/CLOSED；返工数量 <= 原工单已生产数量 - 已返工数量
     *
     * @param createReqVO 创建信息
     * @return 返工工单编号
     */
    Long createReworkOrder(@Valid MesProReworkOrderCreateReqVO createReqVO);

    /**
     * 更新返工工单
     *
     * @param updateReqVO 更新信息
     */
    void updateReworkOrder(@Valid MesProReworkOrderSaveReqVO updateReqVO);

    /**
     * 删除返工工单
     *
     * @param id 编号
     */
    void deleteReworkOrder(Long id);

    /**
     * 校验返工工单存在
     *
     * @param id 编号
     * @return 返工工单
     */
    MesProReworkOrderDO validateReworkOrderExists(Long id);

    /**
     * 获得返工工单
     *
     * @param id 编号
     * @return 返工工单
     */
    MesProReworkOrderDO getReworkOrder(Long id);

    /**
     * 获得返工工单分页
     *
     * @param pageReqVO 分页查询
     * @return 返工工单分页
     */
    PageResult<MesProReworkOrderDO> getReworkOrderPage(MesProReworkOrderPageReqVO pageReqVO);

    /**
     * 开工：状态 10 待返工 → 20 返工中
     *
     * @param id 编号
     */
    void startRework(Long id);

    /**
     * 完工：状态 20 返工中 → 30 已完成，校验所有明细已处理
     *
     * @param id 编号
     */
    void completeRework(Long id);

    /**
     * 取消：状态 → 40 已取消
     *
     * @param id 编号
     */
    void cancelRework(Long id);

}
