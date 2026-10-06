package cn.zhicloud.module.crm.dal.mysql.receivable;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.zhicloud.module.crm.controller.admin.receivable.vo.receivable.CrmReceivablePageReqVO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import cn.zhicloud.module.crm.enums.common.CrmBizTypeEnum;
import cn.zhicloud.module.crm.enums.common.CrmSceneTypeEnum;
import cn.zhicloud.module.crm.util.CrmPermissionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * 回款 Mapper
 *
 * @author 赤焰
 */
@Mapper
public interface CrmReceivableMapper extends BaseMapperX<CrmReceivableDO> {

    default CrmReceivableDO selectByNo(String no) {
        return selectOne(CrmReceivableDO::getNo, no);
    }

    default CrmReceivableDO selectByIdempotencyKey(String idempotencyKey) {
        return selectOne(CrmReceivableDO::getIdempotencyKey, idempotencyKey);
    }

    default CrmReceivableDO selectByIdForUpdate(Long id) {
        return selectOneForUpdate(CrmReceivableDO::getId, id);
    }

    default CrmReceivableDO selectByIdempotencyKeyForUpdate(String idempotencyKey) {
        return selectOneForUpdate(CrmReceivableDO::getIdempotencyKey, idempotencyKey);
    }

    default PageResult<CrmReceivableDO> selectPageByCustomerId(CrmReceivablePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CrmReceivableDO>()
                .eq(CrmReceivableDO::getCustomerId, reqVO.getCustomerId()) // 必须传递
                .eqIfPresent(CrmReceivableDO::getNo, reqVO.getNo())
                .eqIfPresent(CrmReceivableDO::getContractId, reqVO.getContractId())
                .eqIfPresent(CrmReceivableDO::getPlanId, reqVO.getPlanId())
                .orderByDesc(CrmReceivableDO::getId));
    }

    default PageResult<CrmReceivableDO> selectPage(CrmReceivablePageReqVO pageReqVO, Long userId) {
        MPJLambdaWrapperX<CrmReceivableDO> query = new MPJLambdaWrapperX<>();
        // 拼接数据权限的查询条件
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE.getType(),
                CrmReceivableDO::getId, userId, pageReqVO.getSceneType());
        // 拼接自身的查询条件
        query.selectAll(CrmReceivableDO.class)
                .eqIfPresent(CrmReceivableDO::getNo, pageReqVO.getNo())
                .eqIfPresent(CrmReceivableDO::getPlanId, pageReqVO.getPlanId())
                .eqIfPresent(CrmReceivableDO::getContractId, pageReqVO.getContractId())
                .eqIfPresent(CrmReceivableDO::getAuditStatus, pageReqVO.getAuditStatus())
                .orderByDesc(CrmReceivableDO::getId);
        return selectJoinPage(pageReqVO, CrmReceivableDO.class, query);
    }

    default Long selectCountByAudit(Long userId) {
        MPJLambdaWrapperX<CrmReceivableDO> query = new MPJLambdaWrapperX<>();
        // 我负责的 + 非公海
        CrmPermissionUtils.appendPermissionCondition(query, CrmBizTypeEnum.CRM_RECEIVABLE.getType(),
                CrmReceivableDO::getId, userId, CrmSceneTypeEnum.OWNER.getType());
        // 未审核
        query.eq(CrmReceivableDO::getAuditStatus, CrmAuditStatusEnum.PROCESS.getStatus());
        return selectCount(query);
    }

    default List<CrmReceivableDO> selectListByContractIdAndStatus(Long contractId, Collection<Integer> auditStatuses) {
        return selectList(new LambdaQueryWrapperX<CrmReceivableDO>()
                .eq(CrmReceivableDO::getContractId, contractId)
                .in(CrmReceivableDO::getAuditStatus, auditStatuses));
    }

    default List<CrmReceivableDO> selectListByContractIdAndStatusForUpdate(Long contractId, Collection<Integer> auditStatuses) {
        // 并发超收修复的另一半：合同行的 FOR UPDATE 只保证"同时只有一个事务在做校验"，
        // 但 RR 隔离下先到事务的普通一致性读快照可能早于后到事务的提交建立——
        // 锁读（FOR UPDATE）永远读最新已提交版本，先到事务刚提交的回款必须可见
        return selectList(new LambdaQueryWrapperX<CrmReceivableDO>()
                .eq(CrmReceivableDO::getContractId, contractId)
                .in(CrmReceivableDO::getAuditStatus, auditStatuses)
                .last("FOR UPDATE"));
    }

    default Map<Long, BigDecimal> selectReceivablePriceMapByContractId(Collection<Long> contractIds) {
        if (CollUtil.isEmpty(contractIds)) {
            return Collections.emptyMap();
        }
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<CrmReceivableDO>()
                .select("contract_id, SUM(price) AS total_price")
                .in("audit_status", CrmAuditStatusEnum.DRAFT.getStatus(), // 草稿 + 审批中 + 审批通过
                        CrmAuditStatusEnum.PROCESS.getStatus(), CrmAuditStatusEnum.APPROVE.getStatus())
                .groupBy("contract_id")
                .in("contract_id", contractIds));
        // 获得金额
        return convertMap(result, obj -> (Long) obj.get("contract_id"), obj -> (BigDecimal) obj.get("total_price"));
    }

    default Long selectCountByContractId(Long contractId) {
        return selectCount(CrmReceivableDO::getContractId, contractId);
    }

}
