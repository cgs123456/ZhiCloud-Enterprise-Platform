package cn.iocoder.yudao.module.mes.service.pro.rework;

import cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo.MesProReworkOrderDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.rework.MesProReworkOrderDetailDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * MES 返工工单明细 Service 接口
 *
 * @author 芋道源码
 */
public interface MesProReworkOrderDetailService {

    /**
     * 新增返工明细
     *
     * @param createReqVO 创建信息
     * @return 明细编号
     */
    Long addDetail(@Valid MesProReworkOrderDetailSaveReqVO createReqVO);

    /**
     * 更新返工明细
     *
     * @param updateReqVO 更新信息
     */
    void updateDetail(@Valid MesProReworkOrderDetailSaveReqVO updateReqVO);

    /**
     * 删除返工明细
     *
     * @param id 编号
     */
    void deleteDetail(Long id);

    /**
     * 获得返工明细
     *
     * @param id 编号
     * @return 返工明细
     */
    MesProReworkOrderDetailDO getDetail(Long id);

    /**
     * 根据返工工单编号，获得明细列表
     *
     * @param reworkOrderId 返工工单编号
     * @return 明细列表
     */
    List<MesProReworkOrderDetailDO> listByReworkOrderId(Long reworkOrderId);

    /**
     * 校验返工工单所有明细已处理（repairedQuantity >= defectQuantity）
     *
     * @param reworkOrderId 返工工单编号
     * @return 是否全部已处理
     */
    boolean validateAllDetailsProcessed(Long reworkOrderId);

}
