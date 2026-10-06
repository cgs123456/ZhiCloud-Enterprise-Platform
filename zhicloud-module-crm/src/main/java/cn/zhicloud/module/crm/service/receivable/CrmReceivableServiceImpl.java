package cn.zhicloud.module.crm.service.receivable;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.collection.CollectionUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.util.object.ObjectUtils;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.crm.controller.admin.receivable.vo.receivable.CrmReceivablePageReqVO;
import cn.zhicloud.module.crm.controller.admin.receivable.vo.receivable.CrmReceivableSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.dal.redis.no.CrmNoRedisDAO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.enums.common.CrmBizTypeEnum;
import cn.zhicloud.module.crm.enums.permission.CrmPermissionLevelEnum;
import cn.zhicloud.module.crm.framework.permission.core.annotations.CrmPermission;
import cn.zhicloud.module.crm.service.contract.CrmContractService;
import cn.zhicloud.module.crm.service.permission.CrmPermissionService;
import cn.zhicloud.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.*;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.*;
import static cn.zhicloud.module.crm.enums.LogRecordConstants.*;
import static cn.zhicloud.module.crm.util.CrmAuditStatusUtils.convertBpmResultToAuditStatus;

/**
 * CRM 回款 Service 实现类
 *
 * @author 赤焰
 */
@Service
@Validated
@Slf4j
public class CrmReceivableServiceImpl implements CrmReceivableService {

    /**
     * BPM 合同审批流程标识
     */
    public static final String BPM_PROCESS_DEFINITION_KEY = "crm-receivable-audit";

    @Resource
    private CrmReceivableMapper receivableMapper;

    @Resource
    private CrmContractMapper contractMapper;

    @Resource
    private CrmNoRedisDAO noRedisDAO;

    @Resource
    private CrmContractService contractService;
    @Resource
    @Lazy // 延迟加载，避免循环依赖
    private CrmReceivablePlanService receivablePlanService;
    @Resource
    private CrmPermissionService permissionService;

    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_RECEIVABLE_TYPE, subType = CRM_RECEIVABLE_CREATE_SUB_TYPE, bizNo = "{{#receivable.id}}",
            success = CRM_RECEIVABLE_CREATE_SUCCESS)
    public Long createReceivable(CrmReceivableSaveReqVO createReqVO) {
        // 1. 幂等键与载荷指纹必须先于任何业务副作用计算和校验
        String idempotencyKey = validateAndNormalizeIdempotencyKey(createReqVO.getIdempotencyKey());
        String payloadHash = buildReceivablePayloadHash(createReqVO);
        CrmReceivableDO existing = receivableMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return resolveIdempotentReceivable(existing, payloadHash);
        }

        // 2.1 校验可回款金额超过上限（同时锁定合同行）
        validateReceivablePriceExceedsLimit(createReqVO);
        // 2.2 校验关联数据存在
        validateRelationDataExists(createReqVO);
        // 2.3 获得合同行锁后再次检查幂等键，覆盖同一合同并发重试窗口
        existing = receivableMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return resolveIdempotentReceivable(existing, payloadHash);
        }
        // 2.4 生成回款编号
        String no = noRedisDAO.generate(CrmNoRedisDAO.RECEIVABLE_PREFIX);
        if (receivableMapper.selectByNo(no) != null) {
            throw exception(RECEIVABLE_NO_EXISTS);
        }

        // 3. 插入回款
        CrmReceivableDO receivable = BeanUtils.toBean(createReqVO, CrmReceivableDO.class)
                .setNo(no).setAuditStatus(CrmAuditStatusEnum.DRAFT.getStatus())
                .setIdempotencyKey(idempotencyKey).setPayloadHash(payloadHash).setVersion(0);
        try {
            receivableMapper.insert(receivable);
        } catch (DuplicateKeyException e) {
            // 唯一键并发兜底：只按幂等键回查，禁止按 contractId 猜测并返回其他业务单据。
            existing = receivableMapper.selectByIdempotencyKeyForUpdate(idempotencyKey);
            if (existing == null) {
                throw e;
            }
            log.warn("[createReceivable][幂等键={} 并发重复提交，返回已有 id={}]",
                    idempotencyKey, existing.getId());
            return resolveIdempotentReceivable(existing, payloadHash);
        }

        // 4. 创建数据权限
        permissionService.createPermission(new CrmPermissionCreateReqBO().setBizType(CrmBizTypeEnum.CRM_RECEIVABLE.getType())
                .setBizId(receivable.getId()).setUserId(createReqVO.getOwnerUserId())
                .setLevel(CrmPermissionLevelEnum.OWNER.getLevel())); // 设置当前操作的人为负责人

        // 5. 更新关联的回款计划
        if (createReqVO.getPlanId() != null) {
            receivablePlanService.updateReceivablePlanReceivableId(receivable.getPlanId(), receivable.getId());
        }

        // 6. 记录操作日志上下文
        LogRecordContext.putVariable("receivable", receivable);
        LogRecordContext.putVariable("period", getReceivablePeriod(receivable.getPlanId()));
        return receivable.getId();
    }

    private String validateAndNormalizeIdempotencyKey(String idempotencyKey) {
        String normalized = StrUtil.trimToNull(idempotencyKey);
        if (normalized == null) {
            throw exception(RECEIVABLE_IDEMPOTENCY_KEY_REQUIRED);
        }
        if (normalized.length() > 128) {
            throw exception(RECEIVABLE_IDEMPOTENCY_KEY_INVALID);
        }
        return normalized;
    }

    private Long resolveIdempotentReceivable(CrmReceivableDO existing, String payloadHash) {
        if (!Objects.equals(existing.getPayloadHash(), payloadHash)) {
            throw exception(RECEIVABLE_IDEMPOTENCY_KEY_CONFLICT);
        }
        return existing.getId();
    }

    private String buildReceivablePayloadHash(CrmReceivableSaveReqVO reqVO) {
        return DigestUtil.sha256Hex(String.join("|",
                hashValue(reqVO.getContractId()),
                hashValue(reqVO.getPlanId()),
                hashValue(reqVO.getOwnerUserId()),
                hashValue(reqVO.getReturnType()),
                hashValue(normalizeAmount(reqVO.getPrice())),
                hashValue(reqVO.getReturnTime()),
                hashValue(reqVO.getRemark())));
    }

    private String hashValue(Object value) {
        return value == null ? "<null>" : value.toString();
    }

    private String normalizeAmount(BigDecimal amount) {
        return amount == null ? null : amount.stripTrailingZeros().toPlainString();
    }

    private void validateReceivablePriceExceedsLimit(CrmReceivableSaveReqVO reqVO) {
        validateReceivableAmount(reqVO.getPrice());
        // P2 TOCTOU 修复：使用 FOR UPDATE 锁合同行，防止并发超收
        // 在同一事务内先锁定合同，再计算剩余可回款金额，确保原子性
        CrmContractDO contract = contractMapper.selectByIdForUpdate(reqVO.getContractId());
        if (contract == null) {
            throw exception(CONTRACT_NOT_EXISTS);
        }
        if (contract.getTotalPrice() == null) {
            throw exception(RECEIVABLE_CREATE_FAIL_CONTRACT_AMOUNT_INVALID);
        }
        // 草稿、审批中、审批通过均占用合同回款额度，避免重复创建导致超额。
        // 这里必须是锁读：真机并发实测（codes [0,0]、累计 1600>1000），合同行的 FOR UPDATE
        // 只串行化校验者，RR 快照读看不到先到事务刚提交的回款；锁读才保证看到最新已提交额度
        List<CrmReceivableDO> receivables = receivableMapper.selectListByContractIdAndStatusForUpdate(reqVO.getContractId(),
                Arrays.asList(CrmAuditStatusEnum.DRAFT.getStatus(), CrmAuditStatusEnum.PROCESS.getStatus(),
                        CrmAuditStatusEnum.APPROVE.getStatus()));
        if (reqVO.getId() != null) {
            receivables.removeIf(receivable -> ObjectUtil.equal(receivable.getId(), reqVO.getId()));
        }
        BigDecimal notReceivablePrice = contract.getTotalPrice().subtract(
                CollectionUtils.getSumValue(receivables, CrmReceivableDO::getPrice, BigDecimal::add, BigDecimal.ZERO));
        // 2. 校验金额是否超过
        if (reqVO.getPrice().compareTo(notReceivablePrice) > 0) {
            throw exception(RECEIVABLE_CREATE_FAIL_PRICE_EXCEEDS_LIMIT, notReceivablePrice);
        }
    }

    private void validateReceivableAmount(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(RECEIVABLE_AMOUNT_INVALID);
        }
        if (price.stripTrailingZeros().scale() > 2) {
            throw exception(RECEIVABLE_AMOUNT_SCALE_INVALID);
        }
    }

    private void validateRelationDataExists(CrmReceivableSaveReqVO reqVO) {
        if (reqVO.getOwnerUserId() != null) {
            adminUserApi.validateUser(reqVO.getOwnerUserId()); // 校验负责人存在
        }
        if (reqVO.getContractId() != null) {
            CrmContractDO contract = contractService.validateContract(reqVO.getContractId());
            if (ObjectUtil.notEqual(contract.getAuditStatus(), CrmAuditStatusEnum.APPROVE.getStatus())) {
                throw exception(RECEIVABLE_CREATE_FAIL_CONTRACT_NOT_APPROVE);
            }
            reqVO.setCustomerId(contract.getCustomerId()); // 设置客户编号
        }
        if (reqVO.getPlanId() != null) {
            CrmReceivablePlanDO receivablePlan = receivablePlanService.getReceivablePlan(reqVO.getPlanId());
            if (receivablePlan == null) {
                throw exception(RECEIVABLE_PLAN_NOT_EXISTS);
            }
            if (receivablePlan.getReceivableId() != null) {
                throw exception(RECEIVABLE_PLAN_EXISTS_RECEIVABLE);
            }
            // 防止跨合同/跨客户关联计划，导致后续回款归属和额度校验失真
            if (ObjectUtil.notEqual(receivablePlan.getContractId(), reqVO.getContractId())
                    || ObjectUtil.notEqual(receivablePlan.getCustomerId(), reqVO.getCustomerId())) {
                throw exception(RECEIVABLE_PLAN_RELATION_MISMATCH);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_RECEIVABLE_TYPE, subType = CRM_RECEIVABLE_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = CRM_RECEIVABLE_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.WRITE)
    public void updateReceivable(CrmReceivableSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "回款编号不能为空");
        updateReqVO.setOwnerUserId(null).setCustomerId(null).setContractId(null).setPlanId(null); // 不允许修改的字段
        // 1.1 校验存在
        CrmReceivableDO oldReceivable = validateReceivableExists(updateReqVO.getId());
        updateReqVO.setOwnerUserId(oldReceivable.getOwnerUserId()).setCustomerId(oldReceivable.getCustomerId())
                .setContractId(oldReceivable.getContractId()).setPlanId(oldReceivable.getPlanId()); // 设置已存在的值
        // 1.2 校验可回款金额超过上限
        validateReceivablePriceExceedsLimit(updateReqVO);

        // 1.3 只有草稿、审批中，可以编辑；
        if (!ObjectUtils.equalsAny(oldReceivable.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus(),
                CrmAuditStatusEnum.PROCESS.getStatus())) {
            throw exception(RECEIVABLE_UPDATE_FAIL_EDITING_PROHIBITED);
        }

        // 2. 更新回款（显式携带旧版本，0 行代表并发修改，必须失败关闭）
        CrmReceivableDO updateObj = BeanUtils.toBean(updateReqVO, CrmReceivableDO.class)
                .setVersion(oldReceivable.getVersion());
        int updated = receivableMapper.updateById(updateObj);
        if (updated == 0) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }

        // 3. 记录操作日志上下文
        updateReqVO.setOwnerUserId(oldReceivable.getOwnerUserId()); // 避免操作日志出现“删除负责人”的情况
        LogRecordContext.putVariable("oldReceivable", oldReceivable);
        LogRecordContext.putVariable("period", getReceivablePeriod(oldReceivable.getPlanId()));
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldReceivable, CrmReceivableSaveReqVO.class));
    }

    private Integer getReceivablePeriod(Long planId) {
        if (Objects.isNull(planId)) {
            return null;
        }
        CrmReceivablePlanDO receivablePlan = receivablePlanService.getReceivablePlan(planId);
        return receivablePlan.getPeriod();
    }

    @Override
    public void updateReceivableAuditStatus(Long id, Integer bpmResult) {
        // 1.1 校验存在
        CrmReceivableDO receivable = validateReceivableExists(id);
        // 1.2 只有审批中，可以更新审批结果
        if (ObjUtil.notEqual(receivable.getAuditStatus(), CrmAuditStatusEnum.PROCESS.getStatus())) {
            log.error("[updateReceivableAuditStatus][receivable({}) 不处于审批中，无法更新审批结果({})]",
                    receivable.getId(), bpmResult);
            throw exception(RECEIVABLE_UPDATE_AUDIT_STATUS_FAIL_NOT_PROCESS);
        }

        // 2. 更新回款审批状态（乐观锁：设置 version，失败抛异常）
        Integer auditStatus = convertBpmResultToAuditStatus(bpmResult);
        CrmReceivableDO updateAuditObj = new CrmReceivableDO().setId(id).setAuditStatus(auditStatus);
        updateAuditObj.setVersion(receivable.getVersion());
        int updated = receivableMapper.updateById(updateAuditObj);
        if (updated == 0) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_RECEIVABLE_TYPE, subType = CRM_RECEIVABLE_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_RECEIVABLE_DELETE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void deleteReceivable(Long id) {
        // 1.1 校验存在
        CrmReceivableDO receivable = validateReceivableExists(id);
        // 1.2 如果被 CrmReceivablePlanDO 所使用，则不允许删除
        if (receivable.getPlanId() != null && receivablePlanService.getReceivablePlan(receivable.getPlanId()) != null) {
            throw exception(RECEIVABLE_DELETE_FAIL);
        }
        // 1.3 审批通过时，不允许删除
        if (ObjUtil.equal(receivable.getAuditStatus(), CrmAuditStatusEnum.APPROVE.getStatus())) {
            throw exception(RECEIVABLE_DELETE_FAIL_IS_APPROVE);
        }

        // 2.1 删除回款
        receivableMapper.deleteById(id);
        // 2.2 删除数据权限
        permissionService.deletePermission(CrmBizTypeEnum.CRM_RECEIVABLE.getType(), id);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("receivable", receivable);
        LogRecordContext.putVariable("period", getReceivablePeriod(receivable.getPlanId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_RECEIVABLE_TYPE, subType = CRM_RECEIVABLE_SUBMIT_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_RECEIVABLE_SUBMIT_SUCCESS)
    public void submitReceivable(Long id, Long userId) {
        // 1. 锁定回款行并校验状态，保证同一单据只有一个提交者能创建流程
        CrmReceivableDO receivable = receivableMapper.selectByIdForUpdate(id);
        if (receivable == null) {
            throw exception(RECEIVABLE_NOT_EXISTS);
        }
        if (ObjUtil.notEqual(receivable.getAuditStatus(), CrmAuditStatusEnum.DRAFT.getStatus())) {
            throw exception(RECEIVABLE_SUBMIT_FAIL_NOT_DRAFT);
        }
        if (receivable.getVersion() == null) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }

        // 2. 先 CAS 抢占 DRAFT -> PROCESS；失败时绝不调用外部 BPM，避免重复流程实例
        CrmReceivableDO claimObj = new CrmReceivableDO().setId(id)
                .setAuditStatus(CrmAuditStatusEnum.PROCESS.getStatus())
                .setVersion(receivable.getVersion());
        int claimed = receivableMapper.updateById(claimObj);
        if (claimed == 0) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }

        // 3. 创建回款审批流程实例。业务键由 BPM 侧负责幂等/去重，数据库锁只覆盖本地事务。
        String processInstanceId = bpmProcessInstanceApi.createProcessInstance(userId, new BpmProcessInstanceCreateReqDTO()
                .setProcessDefinitionKey(BPM_PROCESS_DEFINITION_KEY).setBusinessKey(String.valueOf(id)));

        // 4. 回写工作流编号；当前事务仍持有行锁，失败则回滚本地状态并暴露补偿问题
        CrmReceivableDO processObj = new CrmReceivableDO().setId(id).setProcessInstanceId(processInstanceId)
                .setVersion(receivable.getVersion() + 1);
        if (receivableMapper.updateById(processObj) == 0) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }

        // 5. 记录日志
        LogRecordContext.putVariable("receivableNo", receivable.getNo());
    }

    private CrmReceivableDO validateReceivableExists(Long id) {
        CrmReceivableDO receivable = receivableMapper.selectById(id);
        if (receivable == null) {
            throw exception(RECEIVABLE_NOT_EXISTS);
        }
        return receivable;
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_RECEIVABLE, bizId = "#id", level = CrmPermissionLevelEnum.READ)
    public CrmReceivableDO getReceivable(Long id) {
        return receivableMapper.selectById(id);
    }

    @Override
    public List<CrmReceivableDO> getReceivableList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return ListUtil.empty();
        }
        return receivableMapper.selectByIds(ids);
    }

    @Override
    public PageResult<CrmReceivableDO> getReceivablePage(CrmReceivablePageReqVO pageReqVO, Long userId) {
        return receivableMapper.selectPage(pageReqVO, userId);
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CUSTOMER, bizId = "#pageReqVO.customerId", level = CrmPermissionLevelEnum.READ)
    public PageResult<CrmReceivableDO> getReceivablePageByCustomerId(CrmReceivablePageReqVO pageReqVO) {
        return receivableMapper.selectPageByCustomerId(pageReqVO);
    }

    @Override
    public Long getAuditReceivableCount(Long userId) {
        return receivableMapper.selectCountByAudit(userId);
    }

    @Override
    public Map<Long, BigDecimal> getReceivablePriceMapByContractId(Collection<Long> contractIds) {
        return receivableMapper.selectReceivablePriceMapByContractId(contractIds);
    }

    @Override
    public Long getReceivableCountByContractId(Long contractId) {
        return receivableMapper.selectCountByContractId(contractId);
    }

}
