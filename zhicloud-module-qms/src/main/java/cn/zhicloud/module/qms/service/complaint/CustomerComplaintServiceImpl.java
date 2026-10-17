package cn.zhicloud.module.qms.service.complaint;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.complaint.vo.CustomerComplaintPageReqVO;
import cn.zhicloud.module.qms.controller.admin.complaint.vo.CustomerComplaintSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.complaint.CustomerComplaintDO;
import cn.zhicloud.module.qms.dal.mysql.complaint.CustomerComplaintMapper;
import cn.zhicloud.module.qms.enums.qms.ComplaintStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.CUSTOMER_COMPLAINT_NOT_CLOSEABLE;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.CUSTOMER_COMPLAINT_NOT_EXISTS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.CUSTOMER_COMPLAINT_STATUS_INVALID;

/**
 * QMS 客户投诉 Service 实现类
 *
 * @author zhicloud
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
        // 状态无条件置为初始值，忽略调用方传入的任何 status。
        // §12.235.5：此前「仅在 null 时补默认」，调用方即可在**建单**这一步直接写入终态，
        // 跳过整个流转（该类可能连流转方法都没有），产生「未审即批/未做即完成」的记录。
        // 状态只能由本模块的流转方法产生。
        complaint.setStatus(ComplaintStatusEnum.REGISTERED.getStatus());
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
        // §12.253 前置态校验：只有处理中（HANDLING）可关闭。
        // 登记即关 = 跳过调查/处理流程；调查未出结论即关 = 流程跳转；
        // 已关闭重复关 = closeTime 被重写。三者共用同一拒绝理由（循 NCR/CAPA 专用关闭码先例）。
        if (!ComplaintStatusEnum.HANDLING.getStatus().equals(complaint.getStatus())) {
            throw exception(CUSTOMER_COMPLAINT_NOT_CLOSEABLE);
        }
        // §12.253 CAS 条件更新：读-判-写之间状态可能被 advanceStatus/并发 close 流转，
        // 只在仍处于读到的处理中时落已关闭 + 关闭时间；0 行即竞态失败，拒绝而非覆盖。
        if (customerComplaintMapper.update(null, new LambdaUpdateWrapper<CustomerComplaintDO>()
                .set(CustomerComplaintDO::getStatus, ComplaintStatusEnum.CLOSED.getStatus())
                .set(CustomerComplaintDO::getCloseTime, LocalDateTime.now())
                .eq(CustomerComplaintDO::getId, id)
                .eq(CustomerComplaintDO::getStatus, ComplaintStatusEnum.HANDLING.getStatus())) == 0) {
            throw exception(CUSTOMER_COMPLAINT_NOT_CLOSEABLE);
        }
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