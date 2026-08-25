package cn.zhicloud.module.crm.service.servicecloud;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.util.object.ObjectUtils;
import cn.zhicloud.module.crm.controller.admin.servicecloud.vo.CrmWorkOrderPageReqVO;
import cn.zhicloud.module.crm.controller.admin.servicecloud.vo.CrmWorkOrderSaveReqVO;
import cn.zhicloud.module.crm.dal.mysql.servicecloud.CrmWorkOrderMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.servicecloud.CrmWorkOrderStatusEnum;
import cn.zhicloud.module.crm.service.customer.CrmCustomerService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.WORK_ORDER_ASSIGN_FAIL;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.WORK_ORDER_CLOSE_FAIL;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.WORK_ORDER_NOT_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.WORK_ORDER_NO_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.WORK_ORDER_RESOLVE_FAIL;

/**
 * CRM 售后工单 Service 实现类
 *
 * @author dhb52
 */
@Service
@Validated
@Slf4j
public class CrmWorkOrderServiceImpl implements CrmWorkOrderService {

    @Resource
    private CrmWorkOrderMapper workOrderMapper;

    @Resource
    private CrmNoRedisDAO noRedisDAO;

    @Resource
    @Lazy
    private CrmCustomerService customerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWorkOrder(CrmWorkOrderSaveReqVO createReqVO) {
        // 1.1 校验客户存在
        if (createReqVO.getCustomerId() != null) {
            customerService.validateCustomer(createReqVO.getCustomerId());
        }
        // 1.2 生成序号
        String no = noRedisDAO.generate(CrmNoRedisDAO.WORK_ORDER_NO_PREFIX);
        if (workOrderMapper.selectByNo(no) != null) {
            throw exception(WORK_ORDER_NO_EXISTS);
        }
        // 2. 插入工单
        CrmWorkOrderDO workOrder = BeanUtils.toBean(createReqVO, CrmWorkOrderDO.class).setNo(no);
        workOrder.setStatus(CrmWorkOrderStatusEnum.UNASSIGNED.getStatus());
        workOrderMapper.insert(workOrder);
        return workOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWorkOrder(CrmWorkOrderSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "售后工单编号不能为空");
        // 1. 校验存在
        validateWorkOrderExists(updateReqVO.getId());
        // 2. 更新
        CrmWorkOrderDO updateObj = BeanUtils.toBean(updateReqVO, CrmWorkOrderDO.class);
        workOrderMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkOrder(Long id) {
        // 1. 校验存在
        validateWorkOrderExists(id);
        // 2. 删除
        workOrderMapper.deleteById(id);
    }

    private CrmWorkOrderDO validateWorkOrderExists(Long id) {
        CrmWorkOrderDO workOrder = workOrderMapper.selectById(id);
        if (workOrder == null) {
            throw exception(WORK_ORDER_NOT_EXISTS);
        }
        return workOrder;
    }

    @Override
    public CrmWorkOrderDO getWorkOrder(Long id) {
        return workOrderMapper.selectById(id);
    }

    @Override
    public PageResult<CrmWorkOrderDO> getWorkOrderPage(CrmWorkOrderPageReqVO pageReqVO) {
        return workOrderMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignWorkOrder(Long id, Long assigneeUserId) {
        // 1. 校验存在
        CrmWorkOrderDO workOrder = validateWorkOrderExists(id);
        // 2. 只有待分配状态，可以分配
        if (ObjUtil.notEqual(workOrder.getStatus(), CrmWorkOrderStatusEnum.UNASSIGNED.getStatus())) {
            throw exception(WORK_ORDER_ASSIGN_FAIL);
        }
        // 3. 更新状态为已分配，记录响应时间
        workOrderMapper.updateById(new CrmWorkOrderDO().setId(id)
                .setAssigneeUserId(assigneeUserId)
                .setStatus(CrmWorkOrderStatusEnum.ASSIGNED.getStatus())
                .setRespondTime(LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveWorkOrder(Long id, String resolution) {
        // 1. 校验存在
        CrmWorkOrderDO workOrder = validateWorkOrderExists(id);
        // 2. 只有已分配或处理中状态，可以解决
        if (!ObjectUtils.equalsAny(workOrder.getStatus(), CrmWorkOrderStatusEnum.ASSIGNED.getStatus(),
                CrmWorkOrderStatusEnum.PROCESSING.getStatus())) {
            throw exception(WORK_ORDER_RESOLVE_FAIL);
        }
        // 3. 更新状态为已解决，记录解决时间和解决方案
        workOrderMapper.updateById(new CrmWorkOrderDO().setId(id)
                .setResolution(resolution)
                .setStatus(CrmWorkOrderStatusEnum.RESOLVED.getStatus())
                .setResolveTime(LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeWorkOrder(Long id) {
        // 1. 校验存在
        CrmWorkOrderDO workOrder = validateWorkOrderExists(id);
        // 2. 只有已解决状态，可以关闭
        if (ObjUtil.notEqual(workOrder.getStatus(), CrmWorkOrderStatusEnum.RESOLVED.getStatus())) {
            throw exception(WORK_ORDER_CLOSE_FAIL);
        }
        // 3. 更新状态为已关闭
        workOrderMapper.updateById(new CrmWorkOrderDO().setId(id)
                .setStatus(CrmWorkOrderStatusEnum.CLOSED.getStatus()));
    }

}
