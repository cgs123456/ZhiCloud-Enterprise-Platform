package cn.zhicloud.module.crm.service.followup;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.crm.controller.admin.followup.vo.CrmFollowUpRecordPageReqVO;
import cn.zhicloud.module.crm.controller.admin.followup.vo.CrmFollowUpRecordSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.followup.CrmFollowUpRecordDO;
import cn.zhicloud.module.crm.dal.mysql.followup.CrmFollowUpRecordMapper;
import cn.zhicloud.module.crm.enums.common.CrmBizTypeEnum;
import cn.zhicloud.module.crm.enums.permission.CrmPermissionLevelEnum;
import cn.zhicloud.module.crm.framework.permission.core.annotations.CrmPermission;
import cn.zhicloud.module.crm.service.business.CrmBusinessService;
import cn.zhicloud.module.crm.service.clue.CrmClueService;
import cn.zhicloud.module.crm.service.contact.CrmContactService;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.customer.CrmCustomerService;
import cn.zhicloud.module.crm.service.followup.bo.CrmFollowUpCreateReqBO;
import cn.zhicloud.module.crm.service.permission.CrmPermissionService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collection;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.FOLLOW_UP_RECORD_DELETE_DENIED;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.FOLLOW_UP_RECORD_NOT_EXISTS;

/**
 * 跟进记录 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class CrmFollowUpRecordServiceImpl implements CrmFollowUpRecordService {

    @Resource
    private CrmFollowUpRecordMapper crmFollowUpRecordMapper;

    @Resource
    @Lazy
    private CrmPermissionService permissionService;
    @Resource
    @Lazy
    private CrmBusinessService businessService;
    @Resource
    @Lazy
    private CrmClueService clueService;
    @Resource
    @Lazy
    private CrmContactService contactService;
    @Resource
    @Lazy
    private CrmContractService contractService;
    @Resource
    @Lazy
    private CrmCustomerService customerService;

    @Override
    // 原子性修复：本方法最多写入 8 个目标（跟进记录 + 客户/商机/线索/联系人/合同 之一 + contactIds/businessIds 的 nextTime），
    // 且由 Controller 直接调用、外层无事务。缺少事务时若中途异常，会出现「跟进记录已插入但业务对象跟进时间未更新」
    // 或「联系人 nextTime 已更新但商机未更新」的脏数据，且无任何补偿机制。
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizTypeValue = "#createReqVO.bizType", bizId = "#createReqVO.bizId", level = CrmPermissionLevelEnum.WRITE)
    public Long createFollowUpRecord(CrmFollowUpRecordSaveReqVO createReqVO) {
        // 1. 创建更进记录
        CrmFollowUpRecordDO record = BeanUtils.toBean(createReqVO, CrmFollowUpRecordDO.class);
        crmFollowUpRecordMapper.insert(record);

        // 2. 更新 bizId 对应的记录
        if (ObjUtil.equal(CrmBizTypeEnum.CRM_CUSTOMER.getType(), record.getBizType())) { // 更新客户跟进信息
            customerService.updateCustomerFollowUp(record.getBizId(), record.getNextTime(), record.getContent());
        }
        if (ObjUtil.equal(CrmBizTypeEnum.CRM_BUSINESS.getType(), record.getBizType())) { // 更新商机跟进信息
            businessService.updateBusinessFollowUp(record.getBizId(), record.getNextTime(), record.getContent());
        }
        if (ObjUtil.equal(CrmBizTypeEnum.CRM_CLUE.getType(), record.getBizType())) { // 更新线索跟进信息
            clueService.updateClueFollowUp(record.getBizId(), record.getNextTime(), record.getContent());
        }
        if (ObjUtil.equal(CrmBizTypeEnum.CRM_CONTACT.getType(), record.getBizType())) { // 更新联系人跟进信息
            contactService.updateContactFollowUp(record.getBizId(), record.getNextTime(), record.getContent());
        }
        if (ObjUtil.equal(CrmBizTypeEnum.CRM_CONTRACT.getType(), record.getBizType())) { // 更新合同跟进信息
            contractService.updateContractFollowUp(record.getBizId(), record.getNextTime(), record.getContent());
        }

        // 3.1 更新 contactIds 对应的记录，只更新 nextTime
        if (CollUtil.isNotEmpty(createReqVO.getContactIds())) {
            contactService.updateContactContactNextTime(createReqVO.getContactIds(), createReqVO.getNextTime());
        }
        // 3.2 需要更新 businessIds 对应的记录，只更新 nextTime
        if (CollUtil.isNotEmpty(createReqVO.getBusinessIds())) {
            businessService.updateBusinessContactNextTime(createReqVO.getBusinessIds(), createReqVO.getNextTime());
        }
        return record.getId();
    }

    @Override
    public void createFollowUpRecordBatch(List<CrmFollowUpCreateReqBO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        crmFollowUpRecordMapper.insertBatch(BeanUtils.toBean(list, CrmFollowUpRecordDO.class));
    }

    @Override
    public void deleteFollowUpRecord(Long id, Long userId) {
        // 校验存在
        CrmFollowUpRecordDO followUpRecord = validateFollowUpRecordExists(id);
        // 校验权限
        if (!permissionService.hasPermission(followUpRecord.getBizType(), followUpRecord.getBizId(), userId, CrmPermissionLevelEnum.OWNER)) {
            throw exception(FOLLOW_UP_RECORD_DELETE_DENIED);
        }

        // 删除
        crmFollowUpRecordMapper.deleteById(id);
    }

    @Override
    public void deleteFollowUpRecordByBiz(Integer bizType, Long bizId) {
        crmFollowUpRecordMapper.deleteByBiz(bizType, bizId);
    }

    private CrmFollowUpRecordDO validateFollowUpRecordExists(Long id) {
        CrmFollowUpRecordDO followUpRecord = crmFollowUpRecordMapper.selectById(id);
        if (followUpRecord == null) {
            throw exception(FOLLOW_UP_RECORD_NOT_EXISTS);
        }
        return followUpRecord;
    }

    @Override
    public CrmFollowUpRecordDO getFollowUpRecord(Long id) {
        return crmFollowUpRecordMapper.selectById(id);
    }

    @Override
    @CrmPermission(bizTypeValue = "#pageReqVO.bizType", bizId = "#pageReqVO.bizId", level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmFollowUpRecordDO> getFollowUpRecordPage(CrmFollowUpRecordPageReqVO pageReqVO) {
        return crmFollowUpRecordMapper.selectPage(pageReqVO);
    }

    @Override
    public List<CrmFollowUpRecordDO> getFollowUpRecordByBiz(Integer bizType, Collection<Long> bizIds) {
        return crmFollowUpRecordMapper.selectListByBiz(bizType, bizIds);
    }

}