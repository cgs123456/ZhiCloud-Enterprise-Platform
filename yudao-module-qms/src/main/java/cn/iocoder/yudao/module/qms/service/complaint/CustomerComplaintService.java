package cn.iocoder.yudao.module.qms.service.complaint;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.complaint.vo.CustomerComplaintPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.complaint.vo.CustomerComplaintSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.complaint.CustomerComplaintDO;
import jakarta.validation.Valid;

/**
 * QMS 客户投诉 Service 接口
 *
 * @author yudao
 */
public interface CustomerComplaintService {

    Long createCustomerComplaint(@Valid CustomerComplaintSaveReqVO createReqVO);

    void updateCustomerComplaint(@Valid CustomerComplaintSaveReqVO updateReqVO);

    void deleteCustomerComplaint(Long id);

    CustomerComplaintDO getCustomerComplaint(Long id);

    PageResult<CustomerComplaintDO> getCustomerComplaintPage(CustomerComplaintPageReqVO pageReqVO);

    /**
     * 推进投诉状态（已登记 -> 调查中 -> 处理中 -> 已关闭）
     *
     * @param id 编号
     */
    void advanceStatus(Long id);

    /**
     * 关闭投诉
     *
     * @param id 编号
     */
    void closeComplaint(Long id);

}