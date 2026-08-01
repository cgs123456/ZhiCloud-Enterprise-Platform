package cn.iocoder.yudao.module.qms.service.complaint;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.complaint.vo.CustomerComplaintPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.complaint.vo.CustomerComplaintSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.complaint.CustomerComplaintDO;
import cn.iocoder.yudao.module.qms.dal.mysql.complaint.CustomerComplaintMapper;
import cn.iocoder.yudao.module.qms.enums.qms.ComplaintStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.CUSTOMER_COMPLAINT_NOT_EXISTS;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.CUSTOMER_COMPLAINT_STATUS_INVALID;

/**
 * QMS 客户投诉 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class CustomerComplaintServiceImpl implements CustomerComplaintService {

    @Resource
    private CustomerComplaintMapper customerComplaintMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCustomerComplaint(CustomerComplaintSaveReqVO createReqVO) {
        CustomerComplaintDO complaint = BeanUtils.toBean(createReqVO, CustomerComplaintDO.class);
        if (complaint.getStatus() == null) {
            complaint.setStatus(ComplaintStatusEnum.REGISTERED.getStatus());
        }
        customerComplaintMapper.insert(complaint);
        return complaint.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerComplaint(CustomerComplaintSaveReqVO updateReqVO) {
        validateCustomerComplaintExists(updateReqVO.getId());
        CustomerComplaintDO updateObj = BeanUtils.toBean(updateReqVO, CustomerComplaintDO.class);
        // 禁止通过通用更新修改状态，状态变更必须走 advanceStatus/closeComplaint 等状态流转方法
        updateObj.setStatus(null);
        customerComplaintMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCustomerComplaint(Long id) {
        validateCustomerComplaintExists(id);
        customerComplaintMapper.deleteById(id);
    }

    private void validateCustomerComplaintExists(Long id) {
        if (customerComplaintMapper.selectById(id) == null) {
            throw exception(CUSTOMER_COMPLAINT_NOT_EXISTS);
        }
    }

    @Override
    public CustomerComplaintDO getCustomerComplaint(Long id) {
        return customerComplaintMapper.selectById(id);
    }

    @Override
    public PageResult<CustomerComplaintDO> getCustomerComplaintPage(CustomerComplaintPageReqVO pageReqVO) {
        return customerComplaintMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advanceStatus(Long id) {
        CustomerComplaintDO complaint = customerComplaintMapper.selectById(id);
        if (complaint == null) {
            throw exception(CUSTOMER_COMPLAINT_NOT_EXISTS);
        }
        Integer currentStatus = complaint.getStatus();
        Integer nextStatus = nextStatus(currentStatus);
        if (nextStatus == null) {
            throw exception(CUSTOMER_COMPLAINT_STATUS_INVALID);
        }
        CustomerComplaintDO updateObj = new CustomerComplaintDO();
        updateObj.setId(id);
        updateObj.setStatus(nextStatus);
        customerComplaintMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeComplaint(Long id) {
        CustomerComplaintDO complaint = customerComplaintMapper.selectById(id);
        if (complaint == null) {
            throw exception(CUSTOMER_COMPLAINT_NOT_EXISTS);
        }
        CustomerComplaintDO updateObj = new CustomerComplaintDO();
        updateObj.setId(id);
        updateObj.setStatus(ComplaintStatusEnum.CLOSED.getStatus());
        updateObj.setCloseTime(LocalDateTime.now());
        customerComplaintMapper.updateById(updateObj);
    }

    /**
     * 获取下一状态：已登记 -> 调查中 -> 处理中（处理中之后由 closeComplaint 关闭）
     */
    private Integer nextStatus(Integer currentStatus) {
        if (currentStatus == null) {
            return null;
        }
        if (ComplaintStatusEnum.REGISTERED.getStatus().equals(currentStatus)) {
            return ComplaintStatusEnum.INVESTIGATING.getStatus();
        }
        if (ComplaintStatusEnum.INVESTIGATING.getStatus().equals(currentStatus)) {
            return ComplaintStatusEnum.HANDLING.getStatus();
        }
        return null;
    }

}