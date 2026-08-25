package cn.zhicloud.module.crm.service.clue;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.extra.spring.SpringUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.crm.controller.admin.clue.vo.CrmCluePageReqVO;
import cn.zhicloud.module.crm.controller.admin.clue.vo.CrmClueSaveReqVO;
import cn.zhicloud.module.crm.controller.admin.clue.vo.CrmClueTransferReqVO;
import cn.zhicloud.module.crm.controller.admin.customer.vo.customer.CrmCustomerSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.clue.CrmClueDO;
import cn.zhicloud.module.crm.dal.dataobject.clue.CrmCluePoolConfigDO;
import cn.zhicloud.module.crm.dal.dataobject.followup.CrmFollowUpRecordDO;
import cn.zhicloud.module.crm.dal.mysql.clue.CrmClueMapper;
import cn.zhicloud.module.crm.enums.common.CrmBizTypeEnum;
import cn.zhicloud.module.crm.enums.permission.CrmPermissionLevelEnum;
import cn.zhicloud.module.crm.framework.permission.core.annotations.CrmPermission;
import cn.zhicloud.module.crm.service.customer.CrmCustomerService;
import cn.zhicloud.module.crm.service.customer.bo.CrmCustomerCreateReqBO;
import cn.zhicloud.module.crm.service.followup.CrmFollowUpRecordService;
import cn.zhicloud.module.crm.service.followup.bo.CrmFollowUpCreateReqBO;
import cn.zhicloud.module.crm.service.permission.CrmOwnerRecordService;
import cn.zhicloud.module.crm.service.permission.CrmPermissionService;
import cn.zhicloud.module.crm.service.permission.bo.CrmOwnerRecordCreateReqBO;
import cn.zhicloud.module.crm.service.permission.bo.CrmPermissionCreateReqBO;
import cn.zhicloud.module.crm.service.permission.bo.CrmPermissionTransferReqBO;
import cn.zhicloud.module.system.api.user.AdminUserApi;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertList;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.singleton;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CLUE_NOT_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CLUE_NOT_OWNER;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CLUE_OWNER_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CLUE_TRANSFORM_FAIL_ALREADY;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CRM_OPTIMISTIC_LOCK_FAIL;
import static cn.zhicloud.module.crm.enums.LogRecordConstants.*;
import static cn.zhicloud.module.system.enums.ErrorCodeConstants.USER_NOT_EXISTS;
import static java.util.Collections.singletonList;

/**
 * 线索 Service 实现类
 *
 * @author Wanwan
 */
@Service
@Validated
@Slf4j
public class CrmClueServiceImpl implements CrmClueService {

    @Resource
    private CrmClueMapper clueMapper;

    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmPermissionService crmPermissionService;
    @Resource
    private CrmFollowUpRecordService followUpRecordService;
    @Resource
    private CrmOwnerRecordService ownerRecordService;
    @Resource
    @Lazy
    private CrmCluePoolConfigService cluePoolConfigService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_CREATE_SUB_TYPE, bizNo = "{{#clue.id}}",
            success = CRM_CLUE_CREATE_SUCCESS)
    public Long createClue(CrmClueSaveReqVO createReqVO) {
        // 1.1 校验关联数据
        validateRelationDataExists(createReqVO);
        // 1.2 校验负责人是否存在
        adminUserApi.validateUser(createReqVO.getOwnerUserId());

        // 2. 插入线索
        CrmClueDO clue = BeanUtils.toBean(createReqVO, CrmClueDO.class);
        clueMapper.insert(clue);

        // 3. 创建数据权限
        CrmPermissionCreateReqBO createReqBO = new CrmPermissionCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CLUE.getType())
                .setBizId(clue.getId()).setUserId(clue.getOwnerUserId()).setLevel(CrmPermissionLevelEnum.OWNER.getLevel());
        crmPermissionService.createPermission(createReqBO);

        // 4. 记录操作日志上下文
        LogRecordContext.putVariable("clue", clue);
        return clue.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = CRM_CLUE_UPDATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#updateReqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void updateClue(CrmClueSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "线索编号不能为空");
        // 1.1 校验线索是否存在
        CrmClueDO oldClue = validateClueExists(updateReqVO.getId());
        // 1.2 校验关联数据
        validateRelationDataExists(updateReqVO);

        // 2. 更新线索（乐观锁：设置 version，失败抛异常）
        CrmClueDO updateObj = BeanUtils.toBean(updateReqVO, CrmClueDO.class);
        updateObj.setVersion(oldClue.getVersion());
        int updated = clueMapper.updateById(updateObj);
        if (updated == 0) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }

        // 3. 记录操作日志上下文
        updateReqVO.setOwnerUserId(oldClue.getOwnerUserId()); // 避免操作日志出现“删除负责人”的情况
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldClue, CrmClueSaveReqVO.class));
        LogRecordContext.putVariable("clueName", oldClue.getName());
    }

    private void validateRelationDataExists(CrmClueSaveReqVO reqVO) {
        // 校验负责人
        if (Objects.nonNull(reqVO.getOwnerUserId()) &&
                Objects.isNull(adminUserApi.getUser(reqVO.getOwnerUserId()))) {
            throw exception(USER_NOT_EXISTS);
        }
    }

    @Override
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_FOLLOW_UP_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CLUE_FOLLOW_UP_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#id", level = CrmPermissionLevelEnum.WRITE)
    public void updateClueFollowUp(Long id, LocalDateTime contactNextTime, String contactLastContent) {
        // 校验线索是否存在
        CrmClueDO oldClue = validateClueExists(id);

        // 更新线索
        CrmClueDO updateObj = new CrmClueDO().setId(id).setFollowUpStatus(true).setContactNextTime(contactNextTime)
                .setContactLastTime(LocalDateTime.now()).setContactLastContent(contactLastContent);
        updateObj.setVersion(oldClue.getVersion());
        int updated = clueMapper.updateById(updateObj);
        if (updated == 0) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("clueName", oldClue.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CLUE_DELETE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void deleteClue(Long id) {
        // 1. 校验存在
        CrmClueDO clue = validateClueExists(id);

        // 2. 删除
        clueMapper.deleteById(id);

        // 3. 删除数据权限
        crmPermissionService.deletePermission(CrmBizTypeEnum.CRM_CLUE.getType(), id);

        // 4. 删除跟进
        followUpRecordService.deleteFollowUpRecordByBiz(CrmBizTypeEnum.CRM_CLUE.getType(), id);

        // 5. 记录操作日志上下文
        LogRecordContext.putVariable("clueName", clue.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_TRANSFER_SUB_TYPE, bizNo = "{{#reqVO.id}}",
            success = CRM_CLUE_TRANSFER_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#reqVO.id", level = CrmPermissionLevelEnum.OWNER)
    public void transferClue(CrmClueTransferReqVO reqVO, Long userId) {
        // 1 校验线索是否存在
        CrmClueDO clue = validateClueExists(reqVO.getId());

        // 2.1 数据权限转移
        crmPermissionService.transferPermission(new CrmPermissionTransferReqBO(userId, CrmBizTypeEnum.CRM_CLUE.getType(),
                        reqVO.getId(), reqVO.getNewOwnerUserId(), reqVO.getOldOwnerPermissionLevel()));
        // 2.2 设置新的负责人
        CrmClueDO updateObj = new CrmClueDO().setId(reqVO.getId()).setOwnerUserId(reqVO.getNewOwnerUserId());
        updateObj.setVersion(clue.getVersion());
        int updated = clueMapper.updateById(updateObj);
        if (updated == 0) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }

        // 3. 记录转移日志
        LogRecordContext.putVariable("clue", clue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = CRM_CLUE_TYPE, subType = CRM_CLUE_TRANSLATE_SUB_TYPE, bizNo = "{{#id}}",
            success = CRM_CLUE_TRANSLATE_SUCCESS)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void transformClue(Long id, Long userId) {
        // 1.1 校验线索都存在
        CrmClueDO clue = validateClueExists(id);
        // 1.2 存在已经转化的
        if (clue.getTransformStatus()) {
            throw exception(CLUE_TRANSFORM_FAIL_ALREADY);
        }

        // 2.1 遍历线索(未转化的线索)，创建对应的客户
        Long customerId = customerService.createCustomer(BeanUtils.toBean(clue, CrmCustomerCreateReqBO.class), userId);
        // 2.2 更新线索
        CrmClueDO updateObj = new CrmClueDO().setId(id).setTransformStatus(Boolean.TRUE).setCustomerId(customerId);
        updateObj.setVersion(clue.getVersion());
        int updated = clueMapper.updateById(updateObj);
        if (updated == 0) {
            throw exception(CRM_OPTIMISTIC_LOCK_FAIL);
        }
        // 2.3 复制跟进记录
        List<CrmFollowUpRecordDO> followUpRecords = followUpRecordService.getFollowUpRecordByBiz(
                CrmBizTypeEnum.CRM_CLUE.getType(), singleton(clue.getId()));
        if (CollUtil.isNotEmpty(followUpRecords)) {
            followUpRecordService.createFollowUpRecordBatch(convertList(followUpRecords, record ->
                    BeanUtils.toBean(record, CrmFollowUpCreateReqBO.class)
                            .setBizType(CrmBizTypeEnum.CRM_CUSTOMER.getType()).setBizId(customerId)));
        }

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("clueName", clue.getName());
    }

    private CrmClueDO validateClueExists(Long id) {
        CrmClueDO crmClueDO = clueMapper.selectById(id);
        if (crmClueDO == null) {
            throw exception(CLUE_NOT_EXISTS);
        }
        return crmClueDO;
    }

    @Override
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#id", level = CrmPermissionLevelEnum.READ)
    public CrmClueDO getClue(Long id) {
        return clueMapper.selectById(id);
    }

    @Override
    public PageResult<CrmClueDO> getCluePage(CrmCluePageReqVO pageReqVO, Long userId) {
        return clueMapper.selectPage(pageReqVO, userId);
    }

    @Override
    public Long getFollowClueCount(Long userId) {
        return clueMapper.selectCountByFollow(userId);
    }

    // ==================== 公海相关操作 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CrmPermission(bizType = CrmBizTypeEnum.CRM_CLUE, bizId = "#id", level = CrmPermissionLevelEnum.OWNER)
    public void putCluePool(Long id) {
        // 1. 校验存在
        CrmClueDO clue = clueMapper.selectById(id);
        if (clue == null) {
            throw exception(CLUE_NOT_EXISTS);
        }
        // 1.1 校验是否为公海数据
        if (clue.getOwnerUserId() == null) {
            throw exception(CLUE_NOT_OWNER, clue.getName());
        }

        // 2. 线索放入公海
        putCluePool(clue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveClue(List<Long> ids, Long ownerUserId, Boolean isReceive) {
        // 1.1 校验存在
        List<CrmClueDO> clues = clueMapper.selectByIds(ids);
        if (clues.size() != ids.size()) {
            throw exception(CLUE_NOT_EXISTS);
        }
        // 1.2 校验负责人是否存在
        adminUserApi.validateUserList(singletonList(ownerUserId));
        // 1.3 校验状态
        clues.forEach(clue -> {
            // 校验是否已有负责人
            if (clue.getOwnerUserId() != null) {
                throw exception(CLUE_OWNER_EXISTS, clue.getName());
            }
        });

        // 2. 领取公海数据
        List<CrmClueDO> updateClues = new ArrayList<>();
        List<CrmPermissionCreateReqBO> createPermissions = new ArrayList<>();
        List<CrmOwnerRecordCreateReqBO> ownerRecords = new ArrayList<>();
        clues.forEach(clue -> {
            // 2.1. 设置负责人 + 领取次数 +1
            updateClues.add(new CrmClueDO().setId(clue.getId())
                    .setOwnerUserId(ownerUserId)
                    .setReceiveCount(clue.getReceiveCount() == null ? 1 : clue.getReceiveCount() + 1));
            // 2.2. 创建负责人数据权限
            createPermissions.add(new CrmPermissionCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CLUE.getType())
                    .setBizId(clue.getId()).setUserId(ownerUserId).setLevel(CrmPermissionLevelEnum.OWNER.getLevel()));
            // 2.3. 记录负责人从公海变更为指定人员
            ownerRecords.add(new CrmOwnerRecordCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CLUE.getType())
                    .setBizId(clue.getId()).setPreOwnerUserId(clue.getOwnerUserId()).setPostOwnerUserId(ownerUserId));
        });
        // 2.4 更新线索负责人
        clueMapper.updateBatch(updateClues);
        // 2.5 创建负责人数据权限
        crmPermissionService.createPermissionBatch(createPermissions);
        // 2.6 记录负责人变更历史
        ownerRecordService.createOwnerRecordList(ownerRecords);
    }

    @Override
    public int autoPutCluePool() {
        CrmCluePoolConfigDO poolConfig = cluePoolConfigService.getCluePoolConfig();
        if (poolConfig == null || !poolConfig.getEnabled()) {
            return 0;
        }
        // 1. 获得需要放到公海的线索列表
        List<CrmClueDO> clueList = clueMapper.selectListByAutoPool(poolConfig);
        // 2. 逐个放入公海
        int count = 0;
        for (CrmClueDO clue : clueList) {
            try {
                getSelf().putCluePool(clue.getId());
                count++;
            } catch (Throwable e) {
                log.error("[autoPutCluePool][线索({}) 放入公海异常]", clue.getId(), e);
            }
        }
        return count;
    }

    @Transactional(rollbackFor = Exception.class) // 需要 protected 修饰，因为需要在事务中调用
    protected void putCluePool(CrmClueDO clue) {
        // 1. 设置负责人为 NULL
        int updateOwnerUserIncr = clueMapper.updateOwnerUserIdById(clue.getId(), null);
        if (updateOwnerUserIncr == 0) {
            throw exception(CLUE_NOT_OWNER, clue.getName());
        }
        ownerRecordService.createOwnerRecord(new CrmOwnerRecordCreateReqBO().setBizType(CrmBizTypeEnum.CRM_CLUE.getType())
                .setBizId(clue.getId()).setPreOwnerUserId(clue.getOwnerUserId()).setPostOwnerUserId(null));

        // 2. 删除负责人数据权限
        crmPermissionService.deletePermission(CrmBizTypeEnum.CRM_CLUE.getType(), clue.getId(),
                CrmPermissionLevelEnum.OWNER.getLevel());
    }

    /**
     * 获得自身的代理对象，解决 AOP 生效问题
     *
     * @return 自己
     */
    private CrmClueServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }

}
