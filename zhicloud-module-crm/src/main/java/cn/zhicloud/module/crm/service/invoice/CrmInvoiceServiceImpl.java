package cn.zhicloud.module.crm.service.invoice;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.crm.controller.admin.invoice.vo.CrmInvoicePageReqVO;
import cn.zhicloud.module.crm.controller.admin.invoice.vo.CrmInvoiceSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.invoice.CrmInvoiceDO;
import cn.zhicloud.module.crm.dal.dataobject.invoice.CrmInvoiceLineDO;
import cn.zhicloud.module.crm.dal.mysql.invoice.CrmInvoiceLineMapper;
import cn.zhicloud.module.crm.dal.mysql.invoice.CrmInvoiceMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.INVOICE_NOT_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.INVOICE_NO_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.INVOICE_SUBMIT_FAIL_NOT_DRAFT;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.INVOICE_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.INVOICE_UPDATE_FAIL_EDITING_PROHIBITED;
import static cn.zhicloud.module.crm.util.CrmAuditStatusUtils.convertBpmResultToAuditStatus;

/**
 * CRM 开票 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class CrmInvoiceServiceImpl implements CrmInvoiceService {

    /**
     * BPM 开票审批流程标识
     */
    public static final String BPM_PROCESS_DEFINITION_KEY = "crm-invoice-audit";

    @Resource
    private CrmInvoiceMapper invoiceMapper;
    @Resource
    private CrmInvoiceLineMapper invoiceLineMapper;
    @Resource
    private CrmNoRedisDAO noRedisDAO;
    @Resource
    private CrmContractService contractService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInvoice(CrmInvoiceSaveReqVO createReqVO) {
        // 1.1 校验关联数据存在
        validateRelationDataExists(createReqVO);
        // 1.2 生成开票单号
        String no = noRedisDAO.generate(CrmNoRedisDAO.INVOICE_PREFIX);
        if (invoiceMapper.selectByNo(no) != null) {
            throw exception(INVOICE_NO_EXISTS);
        }

        // 2.1 插入开票
        CrmInvoiceDO invoice = BeanUtils.toBean(createReqVO, CrmInvoiceDO.class)
                .setNo(no).setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus());
        invoiceMapper.insert(invoice);
        // 2.2 插入开票明细
        if (CollUtil.isNotEmpty(createReqVO.getLines())) {
            List<CrmInvoiceLineDO> lines = BeanUtils.toBean(createReqVO.getLines(), CrmInvoiceLineDO.class);
            lines.forEach(line -> line.setInvoiceId(invoice.getId()));
            invoiceLineMapper.insertBatch(lines);
        }
        return invoice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInvoice(CrmInvoiceSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "开票编号不能为空");
        updateReqVO.setCustomerId(null).setContractId(null); // 不允许修改的字段
        // 1.1 校验存在
        CrmInvoiceDO oldInvoice = validateInvoiceExists(updateReqVO.getId());
        updateReqVO.setCustomerId(oldInvoice.getCustomerId()).setContractId(oldInvoice.getContractId()); // 设置已存在的值
        // 1.2 校验关联数据
        validateRelationDataExists(updateReqVO);
        // 1.3 只有草稿、审批中，可以编辑；
        if (!ObjectUtil.equal(oldInvoice.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())
                && !ObjectUtil.equal(oldInvoice.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            throw exception(INVOICE_UPDATE_FAIL_EDITING_PROHIBITED);
        }

        // 2. 更新开票
        CrmInvoiceDO updateObj = BeanUtils.toBean(updateReqVO, CrmInvoiceDO.class);
        invoiceMapper.updateById(updateObj);

        // 3. 更新开票明细
        invoiceLineMapper.deleteByInvoiceId(updateReqVO.getId());
        if (CollUtil.isNotEmpty(updateReqVO.getLines())) {
            List<CrmInvoiceLineDO> lines = BeanUtils.toBean(updateReqVO.getLines(), CrmInvoiceLineDO.class);
            lines.forEach(line -> line.setInvoiceId(updateReqVO.getId()));
            invoiceLineMapper.insertBatch(lines);
        }
    }

    private void validateRelationDataExists(CrmInvoiceSaveReqVO reqVO) {
        if (reqVO.getOwnerUserId() != null) {
            adminUserApi.validateUser(reqVO.getOwnerUserId()); // 校验负责人存在
        }
        if (reqVO.getContractId() != null) {
            CrmContractDO contract = contractService.validateContract(reqVO.getContractId());
            reqVO.setCustomerId(contract.getCustomerId()); // 设置客户编号
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInvoice(Long id) {
        // 1. 校验存在
        validateInvoiceExists(id);

        // 2.1 删除开票
        invoiceMapper.deleteById(id);
        // 2.2 删除开票明细
        invoiceLineMapper.deleteByInvoiceId(id);
    }

    @Override
    public void submitInvoice(Long id, Long userId) {
        // 1. 校验开票是否在审批
        CrmInvoiceDO invoice = validateInvoiceExists(id);
        if (ObjUtil.notEqual(invoice.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            throw exception(INVOICE_SUBMIT_FAIL_NOT_DRAFT);
        }

        // 2. 创建开票审批流程实例
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId, new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_DEFINITION_KEY).setBusinessKey(String.valueOf(id)));

        // 3. 更新开票工作流编号
        invoiceMapper.updateById(new CrmInvoiceDO().setId(id).setProcessInstanceId(processInstanceId)
                .setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus()));
    }

    @Override
    public void updateInvoiceAuditStatus(Long id, Integer bpmResult) {
        // 1.1 校验存在
        CrmInvoiceDO invoice = validateInvoiceExists(id);
        // 1.2 只有审批中，可以更新审批结果
        if (ObjUtil.notEqual(invoice.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            log.error("[updateInvoiceAuditStatus][invoice({}) 不处于审批中，无法更新审批结果({})]",
                    invoice.getId(), bpmResult);
            throw exception(INVOICE_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS);
        }

        // 2. 更新开票审批状态
        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);
        invoiceMapper.updateById(new CrmInvoiceDO().setId(id).setAuditStatus(auditStatus));
    }

    private CrmInvoiceDO validateInvoiceExists(Long id) {
        CrmInvoiceDO invoice = invoiceMapper.selectById(id);
        if (invoice == null) {
            throw exception(INVOICE_NOT_EXISTS);
        }
        return invoice;
    }

    @Override
    public CrmInvoiceDO getInvoice(Long id) {
        return invoiceMapper.selectById(id);
    }

    @Override
    public List<CrmInvoiceDO> getInvoiceList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return List.of();
        }
        return invoiceMapper.selectByIds(ids);
    }

    @Override
    public PageResult<CrmInvoiceDO> getInvoicePage(CrmInvoicePageReqVO pageReqVO) {
        return invoiceMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<CrmInvoiceDO> getInvoicePageByContractId(CrmInvoicePageReqVO pageReqVO) {
        return invoiceMapper.selectPageByContractId(pageReqVO);
    }

    @Override
    public Map<Long, BigDecimal> getInvoicePriceMapByContractId(Collection<Long> contractIds) {
        return invoiceMapper.selectInvoicePriceMapByContractId(contractIds);
    }

}
