package cn.zhicloud.module.mes.service.wm.productissue;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.collection.CollectionUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.util.object.ObjectUtils;
import cn.zhicloud.module.mes.controller.admin.wm.productissue.vo.MesWmProductIssuePageReqVO;
import cn.zhicloud.module.mes.controller.admin.wm.productissue.vo.MesWmProductIssueSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDetailDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueLineDO;
import cn.zhicloud.module.mes.dal.mysql.wm.productissue.MesWmProductIssueMapper;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.zhicloud.module.mes.dal.mysql.wm.productissue.MesWmProductIssueDetailMapper;
import cn.zhicloud.module.mes.enums.MesBizTypeConstants;
import cn.zhicloud.module.mes.enums.wm.MesWmProductIssueStatusEnum;
import cn.zhicloud.module.mes.enums.wm.MesWmTransactionTypeEnum;
import cn.zhicloud.module.mes.service.md.workstation.MesMdWorkstationService;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderService;
import cn.zhicloud.module.mes.service.pro.workorder.MesProWorkOrderBomService;
import cn.zhicloud.module.mes.service.wm.transaction.MesWmTransactionService;
import cn.zhicloud.module.mes.service.wm.transaction.dto.MesWmTransactionSaveReqDTO;
import cn.zhicloud.module.mes.service.wm.warehouse.MesWmWarehouseAreaService;
import cn.zhicloud.module.mes.service.wm.warehouse.MesWmWarehouseLocationService;
import cn.zhicloud.module.mes.service.wm.warehouse.MesWmWarehouseService;
import cn.zhicloud.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseAreaDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.warehouse.MesWmWarehouseLocationDO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 领料出库单 Service 实现类
 */
@Service
@Validated
public class MesWmProductIssueServiceImpl implements MesWmProductIssueService {

    @Resource
    private MesWmProductIssueMapper issueMapper;

    @Resource
    private MesWmProductIssueLineService issueLineService;
    @Resource
    private MesWmProductIssueDetailService issueDetailService;
    @Resource
    private MesMdWorkstationService workstationService;
    @Resource
    private MesProWorkOrderService workOrderService;
    @Resource
    private MesProWorkOrderBomService workOrderBomService;
    @Resource
    private MesWmProductIssueDetailMapper issueDetailMapper;
    @Resource
    private MesWmTransactionService wmTransactionService;
    @Resource
    private MesWmWarehouseService warehouseService;
    @Resource
    private MesWmWarehouseLocationService locationService;
    @Resource
    private MesWmWarehouseAreaService areaService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProductIssue(MesWmProductIssueSaveReqVO createReqVO) {
        // 1. 校验关联数据
        validateProductIssueSaveData(createReqVO);

        // 2. 插入主表
        MesWmProductIssueDO issue = BeanUtils.toBean(createReqVO, MesWmProductIssueDO.class);
        issue.setStatus(MesWmProductIssueStatusEnum.PREPARE.getStatus());
        issueMapper.insert(issue);
        return issue.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProductIssue(MesWmProductIssueSaveReqVO updateReqVO) {
        // 1.1 校验存在 + 准备中状态
        validateProductIssueExistsAndPrepare(updateReqVO.getId());
        // 1.2 校验关联数据
        validateProductIssueSaveData(updateReqVO);

        // 2. 更新主表
        MesWmProductIssueDO updateObj = BeanUtils.toBean(updateReqVO, MesWmProductIssueDO.class);
        issueMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteProductIssue(Long id) {
        // 1. 校验存在 + 准备中状态
        validateProductIssueExistsAndPrepare(id);

        // 2.1 级联删除明细
        issueDetailService.deleteProductIssueDetailByIssueId(id);
        // 2.2 级联删除行
        issueLineService.deleteProductIssueLineByIssueId(id);
        // 2.3 删除主表
        issueMapper.deleteById(id);
    }

    @Override
    public MesWmProductIssueDO getProductIssue(Long id) {
        return issueMapper.selectById(id);
    }

    @Override
    public PageResult<MesWmProductIssueDO> getProductIssuePage(MesWmProductIssuePageReqVO pageReqVO) {
        return issueMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitProductIssue(Long id) {
        // 校验存在 + 草稿状态
        validateProductIssueExistsAndPrepare(id);
        // 校验至少有一条行
        List<MesWmProductIssueLineDO> lines = issueLineService.getProductIssueLineListByIssueId(id);
        if (CollUtil.isEmpty(lines)) {
            throw exception(WM_PRODUCT_ISSUE_NO_LINE);
        }

        // 提交（草稿 → 待拣货）
        issueMapper.updateById(new MesWmProductIssueDO()
                .setId(id).setStatus(MesWmProductIssueStatusEnum.APPROVING.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stockProductIssue(Long id) {
        // 校验存在
        MesWmProductIssueDO issue = validateProductIssueExists(id);
        if (ObjUtil.notEqual(MesWmProductIssueStatusEnum.APPROVING.getStatus(), issue.getStatus())) {
            throw exception(WM_PRODUCT_ISSUE_STATUS_INVALID);
        }
        // 校验拣货明细闭环：行数量 = 明细数量
        if (!checkProductIssueQuantity(id)) {
            throw exception(WM_PRODUCT_ISSUE_DETAIL_QUANTITY_MISMATCH);
        }

        // 执行拣货（待拣货 → 待执行领出）
        issueMapper.updateById(new MesWmProductIssueDO()
                .setId(id).setStatus(MesWmProductIssueStatusEnum.APPROVED.getStatus()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishProductIssue(Long id) {
        // 1. 校验存在
        MesWmProductIssueDO issue = validateProductIssueExists(id);
        if (ObjUtil.notEqual(MesWmProductIssueStatusEnum.APPROVED.getStatus(), issue.getStatus())) {
            throw exception(WM_PRODUCT_ISSUE_STATUS_INVALID);
        }
        // 校验至少有一条明细
        List<MesWmProductIssueDetailDO> details = issueDetailService.getProductIssueDetailListByIssueId(id);
        if (CollUtil.isEmpty(details)) {
            throw exception(WM_PRODUCT_ISSUE_NO_DETAIL);
        }
        // 校验行数量 = 明细数量
        if (!checkProductIssueQuantity(id)) {
            throw exception(WM_PRODUCT_ISSUE_DETAIL_QUANTITY_MISMATCH);
        }

        // 2.1 发料 BOM 累计上限校验：本次发料 + 历史已发 不得超过工单 BOM 应发总量
        validateIssueNotExceedBom(issue);

        // 2. 遍历所有明细，创建库存事务（扣减库存 + 记录流水）
        createTransactionList(issue);

        // 3. 更新出库单状态 + 领料日期
        issueMapper.updateById(new MesWmProductIssueDO()
                .setId(id).setStatus(MesWmProductIssueStatusEnum.FINISHED.getStatus())
                .setIssueDate(LocalDateTime.now()));
    }

    private void createTransactionList(MesWmProductIssueDO issue) {
        // 1. 查询虚拟线边库
        MesWmWarehouseDO virtualWarehouse = warehouseService.getWarehouseByCode(MesWmWarehouseDO.WIP_VIRTUAL_WAREHOUSE);
        MesWmWarehouseLocationDO virtualLocation = locationService.getWarehouseLocationByCode(MesWmWarehouseLocationDO.WIP_VIRTUAL_LOCATION);
        MesWmWarehouseAreaDO virtualArea = areaService.getWarehouseAreaByCode(MesWmWarehouseAreaDO.WIP_VIRTUAL_AREA);

        // 2. 遍历明细，每条明细产生 OUT（实际仓库扣减）+ IN（虚拟线边库增加）
        List<MesWmProductIssueDetailDO> details = issueDetailService.getProductIssueDetailListByIssueId(issue.getId());
        for (MesWmProductIssueDetailDO detail : details) {
            // 2.1 先从实际仓库出库（库存减少）
            Long outTransactionId = wmTransactionService.createTransaction(new MesWmTransactionSaveReqDTO()
                    .setType(MesWmTransactionTypeEnum.OUT.getType()).setItemId(detail.getItemId())
                    .setQuantity(detail.getQuantity().negate()) // 库存减少
                    .setBatchId(detail.getBatchId()).setBatchCode(detail.getBatchCode())
                    .setWarehouseId(detail.getWarehouseId()).setLocationId(detail.getLocationId()).setAreaId(detail.getAreaId())
                    .setBizType(MesBizTypeConstants.WM_ISSUE).setBizId(issue.getId())
                    .setBizCode(issue.getCode()).setBizLineId(detail.getLineId()));
            // 2.2 再入虚拟线边库（库存增加）
            wmTransactionService.createTransaction(new MesWmTransactionSaveReqDTO()
                    .setType(MesWmTransactionTypeEnum.IN.getType()).setItemId(detail.getItemId())
                    .setQuantity(detail.getQuantity()) // 库存增加
                    .setBatchId(detail.getBatchId()).setBatchCode(detail.getBatchCode())
                    .setWarehouseId(virtualWarehouse.getId()).setLocationId(virtualLocation.getId()).setAreaId(virtualArea.getId())
                    .setBizType(MesBizTypeConstants.WM_ISSUE).setBizId(issue.getId())
                    .setBizCode(issue.getCode()).setBizLineId(detail.getLineId())
                    .setRelatedTransactionId(outTransactionId)); // 关联出库事务
        }
    }

    /**
     * 发料 BOM 累计上限校验
     * <p>
     * 工单 BOM 的 {@code quantity} 已在生成时乘以工单计划产量（见 MesProWorkOrderBomServiceImpl#generateWorkOrderBom），
     * 即为该物料的「应发总量」。累计已发数量来自历史「已完成」领料单的明细。
     * 当「历史已发 + 本次待发」超过应发总量时抛出 {@link ErrorCodeConstants#WM_PRODUCT_ISSUE_BOM_QUANTITY_EXCEED}。
     * <p>
     * 兼容策略：工单无 BOM（如非生产领料）时跳过校验，避免误拦截。
     */
    private void validateIssueNotExceedBom(MesWmProductIssueDO issue) {
        Long workOrderId = issue.getWorkOrderId();
        if (workOrderId == null) {
            return;
        }
        // 1. 工单 BOM 应发总量（按物料聚合）
        List<MesProWorkOrderBomDO> bomList = workOrderBomService.getWorkOrderBomListByWorkOrderId(workOrderId);
        if (CollUtil.isEmpty(bomList)) {
            return;
        }
        Map<Long, BigDecimal> requiredMap = bomList.stream()
                .collect(Collectors.toMap(MesProWorkOrderBomDO::getItemId, MesProWorkOrderBomDO::getQuantity, BigDecimal::add));
        // 2. 历史已发（已完成领料单）
        Map<Long, BigDecimal> issuedMap = issueDetailMapper
                .selectIssuedQuantityByWorkOrderId(workOrderId, MesWmProductIssueStatusEnum.FINISHED.getStatus())
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row.get("item_id")).longValue(),
                        row -> (BigDecimal) row.get("issued_quantity"),
                        BigDecimal::add));
        // 3. 本次待发明细校验
        List<MesWmProductIssueDetailDO> details = issueDetailService.getProductIssueDetailListByIssueId(issue.getId());
        for (MesWmProductIssueDetailDO detail : details) {
            BigDecimal required = requiredMap.getOrDefault(detail.getItemId(), BigDecimal.ZERO);
            BigDecimal issued = issuedMap.getOrDefault(detail.getItemId(), BigDecimal.ZERO);
            BigDecimal afterIssue = issued.add(detail.getQuantity());
            if (afterIssue.compareTo(required) > 0) {
                throw exception(WM_PRODUCT_ISSUE_BOM_QUANTITY_EXCEED, detail.getItemId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelProductIssue(Long id) {
        // 校验存在
        MesWmProductIssueDO issue = validateProductIssueExists(id);
        // 已完成和已取消不允许取消
        if (ObjectUtils.equalsAny(issue.getStatus(),
                MesWmProductIssueStatusEnum.FINISHED.getStatus(),
                MesWmProductIssueStatusEnum.CANCELED.getStatus())) {
            throw exception(WM_PRODUCT_ISSUE_CANCEL_NOT_ALLOWED);
        }

        // 取消
        issueMapper.updateById(new MesWmProductIssueDO()
                .setId(id).setStatus(MesWmProductIssueStatusEnum.CANCELED.getStatus()));
    }

    @Override
    public Boolean checkProductIssueQuantity(Long id) {
        List<MesWmProductIssueLineDO> lines = issueLineService.getProductIssueLineListByIssueId(id);
        for (MesWmProductIssueLineDO line : lines) {
            List<MesWmProductIssueDetailDO> details = issueDetailService.getProductIssueDetailListByLineId(line.getId());
            BigDecimal totalDetailQty = CollectionUtils.getSumValue(details,
                    MesWmProductIssueDetailDO::getQuantity, BigDecimal::add, BigDecimal.ZERO);
            if (line.getQuantity() != null && totalDetailQty.compareTo(line.getQuantity()) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public MesWmProductIssueDO validateProductIssueExists(Long id) {
        MesWmProductIssueDO issue = issueMapper.selectById(id);
        if (issue == null) {
            throw exception(WM_PRODUCT_ISSUE_NOT_EXISTS);
        }
        return issue;
    }

    /**
     * 校验领料出库单存在且为准备中状态
     */
    @Override
    public MesWmProductIssueDO validateProductIssueExistsAndPrepare(Long id) {
        MesWmProductIssueDO issue = validateProductIssueExists(id);
        if (ObjUtil.notEqual(MesWmProductIssueStatusEnum.PREPARE.getStatus(), issue.getStatus())) {
            throw exception(WM_PRODUCT_ISSUE_STATUS_INVALID);
        }
        return issue;
    }

    /**
     * 校验编码唯一性
     */
    private void validateCodeUnique(Long id, String code) {
        MesWmProductIssueDO issue = issueMapper.selectByCode(code);
        if (issue == null) {
            return;
        }
        if (ObjUtil.notEqual(id, issue.getId())) {
            throw exception(WM_PRODUCT_ISSUE_CODE_DUPLICATE);
        }
    }

    /**
     * 校验保存时的关联数据
     */
    private void validateProductIssueSaveData(MesWmProductIssueSaveReqVO reqVO) {
        validateCodeUnique(reqVO.getId(), reqVO.getCode());
        workOrderService.validateWorkOrderConfirmed(reqVO.getWorkOrderId());
        if (reqVO.getWorkstationId() != null) {
            workstationService.validateWorkstationExistsAndEnable(reqVO.getWorkstationId());
        }
    }

}
