package cn.iocoder.yudao.module.mes.service.md.ecn;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.vo.MesBomDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.bom.vo.MesBomSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderItemSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.md.ecn.vo.MesMdEcnOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.bom.MesBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.bom.MesBomDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.ecn.MesMdEcnOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.ecn.MesMdEcnOrderItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.ecn.MesMdEcnOrderItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.md.ecn.MesMdEcnOrderMapper;
import cn.iocoder.yudao.module.mes.service.md.bom.MesBomDetailService;
import cn.iocoder.yudao.module.mes.service.md.bom.MesBomService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_ECN_ORDER_BOM_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_ECN_ORDER_NO_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_ECN_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.MD_ECN_ORDER_STATUS_INVALID;

/**
 * MES ECN 工程变更单 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class MesMdEcnOrderServiceImpl implements MesMdEcnOrderService {

    /**
     * 变更类型：新增 BOM
     */
    private static final int CHANGE_TYPE_CREATE_BOM = 10;
    /**
     * 变更类型：修改 BOM
     */
    private static final int CHANGE_TYPE_UPDATE_BOM = 20;
    /**
     * 变更类型：删除 BOM
     */
    private static final int CHANGE_TYPE_DELETE_BOM = 30;
    /**
     * 变更类型：替换物料
     */
    private static final int CHANGE_TYPE_REPLACE_ITEM = 40;

    /**
     * ECN 状态：草稿
     */
    private static final int STATUS_DRAFT = 10;
    /**
     * ECN 状态：审核中
     */
    private static final int STATUS_APPROVING = 20;
    /**
     * ECN 状态：已批准
     */
    private static final int STATUS_APPROVED = 30;
    /**
     * ECN 状态：已驳回
     */
    private static final int STATUS_REJECTED = 40;
    /**
     * ECN 状态：已执行
     */
    private static final int STATUS_EXECUTED = 50;

    /**
     * 变更项：物料
     */
    private static final int CHANGE_ITEM_PRODUCT = 10;
    /**
     * 变更项：数量
     */
    private static final int CHANGE_ITEM_QUANTITY = 20;

    @Resource
    private MesMdEcnOrderMapper ecnOrderMapper;
    @Resource
    private MesMdEcnOrderItemMapper ecnOrderItemMapper;
    @Resource
    private MesBomService bomService;
    @Resource
    private MesBomDetailService bomDetailService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEcnOrder(MesMdEcnOrderSaveReqVO createReqVO) {
        // 1. 校验 ECN 单号唯一
        validateNoUnique(null, createReqVO.getNo());
        // 2. 校验变更类型与 BOM 关系
        validateBomRelation(createReqVO);
        // 3. 插入主表
        MesMdEcnOrderDO ecnOrder = BeanUtils.toBean(createReqVO, MesMdEcnOrderDO.class)
                .setStatus(STATUS_DRAFT);
        ecnOrderMapper.insert(ecnOrder);
        // 4. 插入明细
        saveItems(ecnOrder.getId(), createReqVO.getItems());
        return ecnOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEcnOrder(MesMdEcnOrderSaveReqVO updateReqVO) {
        // 1.1 校验存在 + 草稿状态
        MesMdEcnOrderDO existed = validateEcnOrderStatus(updateReqVO.getId(), STATUS_DRAFT);
        // 1.2 校验 ECN 单号唯一
        validateNoUnique(updateReqVO.getId(), updateReqVO.getNo());
        // 1.3 校验变更类型与 BOM 关系
        validateBomRelation(updateReqVO);
        // 2. 更新主表
        MesMdEcnOrderDO updateObj = BeanUtils.toBean(updateReqVO, MesMdEcnOrderDO.class);
        ecnOrderMapper.updateById(updateObj);
        // 3. 重置明细：先删后插
        ecnOrderItemMapper.deleteByEcnOrderId(updateReqVO.getId());
        saveItems(updateReqVO.getId(), updateReqVO.getItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEcnOrder(Long id) {
        // 1. 校验存在 + 草稿状态
        validateEcnOrderStatus(id, STATUS_DRAFT);
        // 2. 删除明细
        ecnOrderItemMapper.deleteByEcnOrderId(id);
        // 3. 删除主表
        ecnOrderMapper.deleteById(id);
    }

    @Override
    public MesMdEcnOrderDO getEcnOrder(Long id) {
        return ecnOrderMapper.selectById(id);
    }

    @Override
    public MesMdEcnOrderDO validateEcnOrderExists(Long id) {
        MesMdEcnOrderDO ecnOrder = ecnOrderMapper.selectById(id);
        if (ecnOrder == null) {
            throw exception(MD_ECN_ORDER_NOT_EXISTS);
        }
        return ecnOrder;
    }

    @Override
    public PageResult<MesMdEcnOrderDO> getEcnOrderPage(MesMdEcnOrderPageReqVO pageReqVO) {
        return ecnOrderMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitEcnOrder(Long id) {
        // 1. 校验存在 + 草稿状态
        validateEcnOrderStatus(id, STATUS_DRAFT);
        // 2. 更新状态为审核中
        ecnOrderMapper.updateById(new MesMdEcnOrderDO().setId(id).setStatus(STATUS_APPROVING));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveEcnOrder(Long id, boolean approved, Long approveUserId) {
        // 1. 校验存在 + 审核中状态
        validateEcnOrderStatus(id, STATUS_APPROVING);
        if (approveUserId == null) {
            throw exception(MD_ECN_ORDER_BOM_REQUIRED);
        }
        // 2. 更新状态为已批准/已驳回，并回写审批人 + 审批日期
        ecnOrderMapper.updateById(new MesMdEcnOrderDO().setId(id)
                .setStatus(approved ? STATUS_APPROVED : STATUS_REJECTED)
                .setApproveUserId(approveUserId)
                .setApproveDate(LocalDateTime.now()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeEcnOrder(Long id) {
        // 1. 校验存在 + 已批准状态
        MesMdEcnOrderDO ecnOrder = validateEcnOrderStatus(id, STATUS_APPROVED);

        // 2. 根据 changeType 执行对应操作
        Integer changeType = ecnOrder.getChangeType();
        if (changeType != null) {
            switch (changeType) {
                case CHANGE_TYPE_CREATE_BOM:
                    executeCreateBom(ecnOrder);
                    break;
                case CHANGE_TYPE_UPDATE_BOM:
                    executeUpdateBom(ecnOrder);
                    break;
                case CHANGE_TYPE_DELETE_BOM:
                    executeDeleteBom(ecnOrder);
                    break;
                case CHANGE_TYPE_REPLACE_ITEM:
                    executeReplaceItem(ecnOrder);
                    break;
                default:
                    log.warn("[executeEcnOrder][未知的变更类型 {}]", changeType);
            }
        }

        // 3. 更新状态为已执行
        ecnOrderMapper.updateById(new MesMdEcnOrderDO().setId(id).setStatus(STATUS_EXECUTED));
    }

    // ==================== 变更执行方法 ====================

    /**
     * 执行新增 BOM：启用新 BOM（如有）
     */
    private void executeCreateBom(MesMdEcnOrderDO ecnOrder) {
        if (ecnOrder.getNewBomId() == null) {
            return;
        }
        MesBomDO newBom = bomService.getBom(ecnOrder.getNewBomId());
        if (newBom != null && !CommonStatusEnum.ENABLE.getStatus().equals(newBom.getStatus())) {
            updateBomStatus(newBom, CommonStatusEnum.ENABLE.getStatus());
        }
    }

    /**
     * 执行修改 BOM：停用原 BOM、启用新 BOM
     */
    private void executeUpdateBom(MesMdEcnOrderDO ecnOrder) {
        // 1. 停用原 BOM
        if (ecnOrder.getBomId() != null) {
            MesBomDO oldBom = bomService.getBom(ecnOrder.getBomId());
            if (oldBom != null) {
                updateBomStatus(oldBom, CommonStatusEnum.DISABLE.getStatus());
            }
        }
        // 2. 启用新 BOM
        if (ecnOrder.getNewBomId() != null) {
            MesBomDO newBom = bomService.getBom(ecnOrder.getNewBomId());
            if (newBom != null) {
                updateBomStatus(newBom, CommonStatusEnum.ENABLE.getStatus());
            }
        }
    }

    /**
     * 执行删除 BOM：删除原 BOM（同时级联删除明细，由 BomService 保证）
     */
    private void executeDeleteBom(MesMdEcnOrderDO ecnOrder) {
        if (ecnOrder.getBomId() == null) {
            return;
        }
        bomService.deleteBom(ecnOrder.getBomId());
    }

    /**
     * 执行替换物料：按变更明细更新 BOM 明细
     *
     * <p>changeItem=10 物料：将 bomDetailId 的 productId 改为 newValue
     * <p>changeItem=20 数量：将 bomDetailId 的 quantity 改为 newValue
     */
    private void executeReplaceItem(MesMdEcnOrderDO ecnOrder) {
        List<MesMdEcnOrderItemDO> items = ecnOrderItemMapper.selectListByEcnOrderId(ecnOrder.getId());
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Long bomId = ecnOrder.getBomId();
        if (bomId == null) {
            log.warn("[executeReplaceItem][ECN {} 未配置 bomId，跳过执行]", ecnOrder.getId());
            return;
        }
        for (MesMdEcnOrderItemDO item : items) {
            if (item.getBomDetailId() == null) {
                continue;
            }
            MesBomDetailDO bomDetail = bomDetailService.validateBomDetailExists(item.getBomDetailId());
            MesBomDetailSaveReqVO updateVO = BeanUtils.toBean(bomDetail, MesBomDetailSaveReqVO.class);
            if (item.getChangeItem() != null) {
                switch (item.getChangeItem()) {
                    case CHANGE_ITEM_PRODUCT:
                        // 替换物料：newValue 是新的 productId
                        if (ObjectUtil.isNotEmpty(item.getNewValue())) {
                            updateVO.setProductId(Long.valueOf(item.getNewValue()));
                        }
                        break;
                    case CHANGE_ITEM_QUANTITY:
                        // 替换数量：newValue 是新的 quantity
                        if (ObjectUtil.isNotEmpty(item.getNewValue())) {
                            updateVO.setQuantity(new BigDecimal(item.getNewValue()));
                        }
                        break;
                    default:
                        // 工序/备注变更不影响 BOM 明细，跳过
                        break;
                }
            }
            bomDetailService.updateBomDetail(updateVO);
        }
    }

    // ==================== 校验方法 ====================

    private void validateNoUnique(Long id, String no) {
        if (no == null) {
            return;
        }
        MesMdEcnOrderDO existed = ecnOrderMapper.selectByNo(no);
        if (existed != null && ObjUtil.notEqual(existed.getId(), id)) {
            throw exception(MD_ECN_ORDER_NO_DUPLICATE, no);
        }
    }

    /**
     * 校验变更类型与 BOM 关系：修改/删除/替换物料时必须填原 BOM
     */
    private void validateBomRelation(MesMdEcnOrderSaveReqVO reqVO) {
        Integer changeType = reqVO.getChangeType();
        if (changeType == null) {
            return;
        }
        if (changeType == CHANGE_TYPE_UPDATE_BOM
                || changeType == CHANGE_TYPE_DELETE_BOM
                || changeType == CHANGE_TYPE_REPLACE_ITEM) {
            if (reqVO.getBomId() == null) {
                throw exception(MD_ECN_ORDER_BOM_REQUIRED);
            }
        }
    }

    private MesMdEcnOrderDO validateEcnOrderStatus(Long id, Integer expectedStatus) {
        MesMdEcnOrderDO ecnOrder = validateEcnOrderExists(id);
        if (ObjUtil.notEqual(ecnOrder.getStatus(), expectedStatus)) {
            throw exception(MD_ECN_ORDER_STATUS_INVALID);
        }
        return ecnOrder;
    }

    // ==================== 工具方法 ====================

    private void saveItems(Long ecnOrderId, List<MesMdEcnOrderItemSaveReqVO> items) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        for (MesMdEcnOrderItemSaveReqVO item : items) {
            MesMdEcnOrderItemDO itemDO = BeanUtils.toBean(item, MesMdEcnOrderItemDO.class)
                    .setEcnOrderId(ecnOrderId);
            itemDO.setId(null); // 重置 ID，由数据库生成
            ecnOrderItemMapper.insert(itemDO);
        }
    }

    private void updateBomStatus(MesBomDO bom, Integer status) {
        MesBomSaveReqVO updateVO = BeanUtils.toBean(bom, MesBomSaveReqVO.class);
        updateVO.setStatus(status);
        bomService.updateBom(updateVO);
    }

}
