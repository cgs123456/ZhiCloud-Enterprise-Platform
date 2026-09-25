package cn.zhicloud.framework.mdm.service;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.mdm.api.MdmCrossRefUpsertReq;
import cn.zhicloud.framework.mdm.dal.dataobject.MdmCrossRefDO;
import cn.zhicloud.framework.mdm.dal.mysql.MdmCrossRefMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_ARG_INVALID;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_NOT_FOUND;
import static cn.zhicloud.framework.mdm.enums.ErrorCodeConstants.MDM_CROSS_REF_TARGET_ID_INVALID;
import static cn.zhicloud.framework.mdm.enums.MdmConstants.STATUS_INVALID;
import static cn.zhicloud.framework.mdm.enums.MdmConstants.STATUS_VALID;

/**
 * 主数据跨域对照 Service 实现（V101）
 *
 * @author 智云集成治理
 */
@Service
public class MdmCrossRefServiceImpl implements MdmCrossRefService {

    /** 未显式传租户时的默认租户：与可靠集成数据面（V100）对 tenantId 的默认处理保持一致 */
    private static final Long DEFAULT_TENANT_ID = 0L;

    @Resource
    private MdmCrossRefMapper mdmCrossRefMapper;

    @Override
    public Long upsert(MdmCrossRefUpsertReq req) {
        validateUpsertReq(req);
        Long tenantId = defaultTenant(req.getTenantId());
        String status = StrUtil.isBlank(req.getStatus()) ? STATUS_VALID : req.getStatus();

        MdmCrossRefDO existing = mdmCrossRefMapper.selectByRef(tenantId, req.getSourceSystem(), req.getSourceEntity(),
                req.getSourceId(), req.getTargetSystem(), req.getTargetEntity());
        if (existing == null) {
            MdmCrossRefDO insertObj = MdmCrossRefDO.builder()
                    .tenantId(tenantId)
                    .sourceSystem(req.getSourceSystem())
                    .sourceEntity(req.getSourceEntity())
                    .sourceId(req.getSourceId())
                    .targetSystem(req.getTargetSystem())
                    .targetEntity(req.getTargetEntity())
                    .targetId(req.getTargetId())
                    .status(status)
                    .remark(req.getRemark())
                    .build();
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
        MdmCrossRefDO existing = mdmCrossRefMapper.selectByRef(defaultTenant(tenantId), sourceSystem, sourceEntity,
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
        MdmCrossRefDO ref = mdmCrossRefMapper.selectByRef(defaultTenant(tenantId), sourceSystem, sourceEntity,
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

    private static Long defaultTenant(Long tenantId) {
        return tenantId == null ? DEFAULT_TENANT_ID : tenantId;
    }

}
