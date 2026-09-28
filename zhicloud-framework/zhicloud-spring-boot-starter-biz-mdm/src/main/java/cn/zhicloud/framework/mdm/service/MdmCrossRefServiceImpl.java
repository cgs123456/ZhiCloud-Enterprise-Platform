package cn.zhicloud.framework.mdm.service;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.mdm.api.MdmCrossRefUpsertReq;
import cn.zhicloud.framework.mdm.dal.dataobject.MdmCrossRefDO;
import cn.zhicloud.framework.mdm.dal.mysql.MdmCrossRefMapper;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_ARG_INVALID;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_NOT_FOUND;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_TARGET_ID_INVALID;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_TENANT_MISMATCH;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_TENANT_REQUIRED;
import static cn.zhicloud.framework.mdm.enums.MdmConstants.STATUS_INVALID;
import static cn.zhicloud.framework.mdm.enums.MdmConstants.STATUS_VALID;

/**
 * 主数据跨域对照 Service 实现（V101）
 *
 * @author 智云集成治理
 */
@Service
public class MdmCrossRefServiceImpl implements MdmCrossRefService {


    @Resource
    private MdmCrossRefMapper mdmCrossRefMapper;

    @Override
    public Long upsert(MdmCrossRefUpsertReq req) {
        validateUpsertReq(req);
        Long tenantId = resolveTenant(req.getTenantId());
        String status = StrUtil.isBlank(req.getStatus()) ? STATUS_VALID : req.getStatus();

        MdmCrossRefDO existing = mdmCrossRefMapper.selectByRef(tenantId, req.getSourceSystem(), req.getSourceEntity(),
                req.getSourceId(), req.getTargetSystem(), req.getTargetEntity());
        if (existing == null) {
            MdmCrossRefDO insertObj = MdmCrossRefDO.builder()
                    .sourceSystem(req.getSourceSystem())
                    .sourceEntity(req.getSourceEntity())
                    .sourceId(req.getSourceId())
                    .targetSystem(req.getTargetSystem())
                    .targetEntity(req.getTargetEntity())
                    .targetId(req.getTargetId())
                    .status(status)
                    .remark(req.getRemark())
                    .build();
            insertObj.setTenantId(tenantId);
            mdmCrossRefMapper.insert(insertObj);
            return insertObj.getId();
        }
        // 已存在则更新为最新事实：对照表表达「当前映射」而不是历史流水
        MdmCrossRefDO updateObj = MdmCrossRefDO.builder()
                .id(existing.getId())
                .targetId(req.getTargetId())
                .status(status)
                .remark(req.getRemark())
                .build();
        mdmCrossRefMapper.updateById(updateObj);
        return existing.getId();
    }

    @Override
    public String mapTargetId(Long tenantId, String sourceSystem, String sourceEntity, String sourceId,
                              String targetSystem, String targetEntity) {
        MdmCrossRefDO ref = selectValidRef(tenantId, sourceSystem, sourceEntity, sourceId, targetSystem, targetEntity);
        if (ref == null) {
            throw exception(MDM_CROSS_REF_NOT_FOUND, sourceSystem, sourceEntity, sourceId, targetSystem, targetEntity);
        }
        return ref.getTargetId();
    }

    @Override
    public Long mapTargetIdAsLong(Long tenantId, String sourceSystem, String sourceEntity, String sourceId,
                                  String targetSystem, String targetEntity) {
        String targetId = mapTargetId(tenantId, sourceSystem, sourceEntity, sourceId, targetSystem, targetEntity);
        try {
            return Long.valueOf(targetId);
        } catch (NumberFormatException e) {
            throw exception(MDM_CROSS_REF_TARGET_ID_INVALID, targetId);
        }
    }

    @Override
    public String findTargetId(Long tenantId, String sourceSystem, String sourceEntity, String sourceId,
                               String targetSystem, String targetEntity) {
        MdmCrossRefDO ref = selectValidRef(tenantId, sourceSystem, sourceEntity, sourceId, targetSystem, targetEntity);
        return ref == null ? null : ref.getTargetId();
    }

    @Override
    public boolean invalidate(Long tenantId, String sourceSystem, String sourceEntity, String sourceId,
                              String targetSystem, String targetEntity) {
        MdmCrossRefDO existing = mdmCrossRefMapper.selectByRef(resolveTenant(tenantId), sourceSystem, sourceEntity,
                sourceId, targetSystem, targetEntity);
        if (existing == null) {
            return false;
        }
        mdmCrossRefMapper.updateById(MdmCrossRefDO.builder()
                .id(existing.getId())
                .status(STATUS_INVALID)
                .build());
        return true;
    }

    private MdmCrossRefDO selectValidRef(Long tenantId, String sourceSystem, String sourceEntity, String sourceId,
                                         String targetSystem, String targetEntity) {
        MdmCrossRefDO ref = mdmCrossRefMapper.selectByRef(resolveTenant(tenantId), sourceSystem, sourceEntity,
                sourceId, targetSystem, targetEntity);
        // 失效映射等同于缺失：让调用方显式失败，而不是用一条已废弃的映射继续写下游单据
        if (ref == null || !STATUS_VALID.equals(ref.getStatus())) {
            return null;
        }
        return ref;
    }

    private void validateUpsertReq(MdmCrossRefUpsertReq req) {
        if (req == null
                || StrUtil.isBlank(req.getSourceSystem())
                || StrUtil.isBlank(req.getSourceEntity())
                || StrUtil.isBlank(req.getSourceId())
                || StrUtil.isBlank(req.getTargetSystem())
                || StrUtil.isBlank(req.getTargetEntity())
                || StrUtil.isBlank(req.getTargetId())) {
            throw exception(MDM_CROSS_REF_ARG_INVALID);
        }
    }

    /**
     * 统一解析租户：上下文缺失时 fail-closed；请求租户为空时使用上下文；
     * 请求租户与上下文不一致时拒绝，禁止任何调用点静默落到默认租户 0。
     */
    private static Long resolveTenant(Long requestTenantId) {
        Long contextTenantId = TenantContextHolder.getTenantId();
        if (contextTenantId == null) {
            throw exception(MDM_CROSS_REF_TENANT_REQUIRED);
        }
        if (requestTenantId != null && !Objects.equals(contextTenantId, requestTenantId)) {
            throw exception(MDM_CROSS_REF_TENANT_MISMATCH, contextTenantId, requestTenantId);
        }
        return contextTenantId;
    }

}
