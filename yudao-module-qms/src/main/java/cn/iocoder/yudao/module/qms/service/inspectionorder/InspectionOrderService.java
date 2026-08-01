package cn.iocoder.yudao.module.qms.service.inspectionorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.FqcInspectionOrderCreateReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderSaveReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS 检验单 Service 接口
 *
 * @author 芋道源码
 */
public interface InspectionOrderService {

    /**
     * 创建检验单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createInspectionOrder(@Valid InspectionOrderSaveReqVO createReqVO);

    /**
     * 创建 FQC 成品检验单
     *
     * <p>固定 type=FQC(35)，必须关联成品工单 ID 与产品 ID。
     *
     * @param createReqVO 成品检验单创建信息
     * @return 检验单编号
     */
    Long createFqcInspectionOrder(@Valid FqcInspectionOrderCreateReqVO createReqVO);

    /**
     * 更新检验单
     *
     * @param updateReqVO 更新信息
     */
    void updateInspectionOrder(@Valid InspectionOrderSaveReqVO updateReqVO);

    /**
     * 删除检验单
     *
     * @param id 编号
     */
    void deleteInspectionOrder(Long id);

    /**
     * 获得检验单
     *
     * @param id 编号
     * @return 检验单
     */
    InspectionOrderDO getInspectionOrder(Long id);

    /**
     * 获得检验单分页
     *
     * @param pageReqVO 分页查询
     * @return 检验单分页
     */
    PageResult<InspectionOrderDO> getInspectionOrderPage(InspectionOrderPageReqVO pageReqVO);

    /**
     * 提交检验结果，自动计算 PASS/FAIL
     *
     * @param orderId 检验单 ID
     * @param records 检验记录列表
     */
    void submitInspection(Long orderId, @Valid List<InspectionRecordSaveReqVO> records);

}