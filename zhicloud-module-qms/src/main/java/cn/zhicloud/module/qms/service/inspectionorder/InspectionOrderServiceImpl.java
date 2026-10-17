package cn.zhicloud.module.qms.service.inspectionorder;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.inspectionorder.vo.FqcInspectionOrderCreateReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionorder.vo.InspectionOrderPageReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionorder.vo.InspectionOrderSaveReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.zhicloud.module.qms.dal.mysql.inspectionorder.InspectionOrderMapper;
import cn.zhicloud.module.qms.dal.mysql.inspectionrecord.InspectionRecordMapper;
import cn.zhicloud.module.qms.enums.qms.InspectionBizTypeEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionOrderStatusEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionResultEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionSeverityEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_ORDER_NOT_EXISTS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_ORDER_NOT_SUBMIT;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_ORDER_OPTIMISTIC_LOCK_FAIL;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_ORDER_JUDGED;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_ORDER_HAS_RECORDS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_ORDER_NO_RECORDS;

/**
 * QMS 检验单 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class InspectionOrderServiceImpl implements InspectionOrderService {

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;
    @Resource
    private InspectionRecordMapper inspectionRecordMapper;

    @Override
    public Long createInspectionOrder(InspectionOrderSaveReqVO createReqVO) {
        // 插入
        InspectionOrderDO inspectionOrder = BeanUtils.toBean(createReqVO, InspectionOrderDO.class);
        // 状态无条件置为「待检验」，忽略调用方传入的任何 status。
        //
        // §12.235.6：此前这里写的是「仅在 status == null 时补默认值」，于是调用方可以
        // 在**建单**这一步直接传入 PASSED(30)，造出一张从未检验过的「已通过」检验单。
        // 该状态正是 WMS 入库质检卡点的唯一判据 —— 一次 POST 即可放行本应被拦截的收货
        // （WmsReceiptOrderServiceImpl#checkQualityGate -> InspectionOrderApi#isQualified，
        // 只读 order.status == PASSED）。真机已实证。
        //
        // 这里与 updateInspectionOrder 的 setStatus(null) 是对称的两半：
        // 状态只能由 submitInspection 依据 AQL 与缺陷严重度判定产生，
        // 通用新增/修改都不接受外部状态值。
        inspectionOrder.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
        inspectionOrderMapper.insert(inspectionOrder);
        // 返回
        return inspectionOrder.getId();
    }

    @Override
    public Long createFqcInspectionOrder(FqcInspectionOrderCreateReqVO createReqVO) {
        // 1. 构建 FQC 成品检验单：type 固定为 FQC，关联成品工单 ID 与产品 ID
        InspectionOrderDO inspectionOrder = BeanUtils.toBean(createReqVO, InspectionOrderDO.class);
        inspectionOrder.setType(InspectionTypeEnum.FQC.getType());
        // 2. 默认状态为待检验
        if (inspectionOrder.getStatus() == null) {
            inspectionOrder.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
        }
        // 3. 业务关联：默认绑定成品工单，支撑入库前质检卡点 isQualified 查询
        if (inspectionOrder.getBizType() == null) {
            inspectionOrder.setBizType(InspectionBizTypeEnum.PRODUCTION_OUT.getBizType());
        }
        if (inspectionOrder.getBizId() == null) {
            inspectionOrder.setBizId(createReqVO.getWorkOrderId());
        }
        // 4. 幂等（P2 幂等键）：同一业务单据（bizType+bizId）已存在检验单时直接返回已有单据，
        //    避免上游事件（如工单完工）重复触发创建重复检验单。
        //    注：不采用全局 (biz_type, biz_id) 唯一索引，以保留合法的「复检/重新检验」能力。
        InspectionOrderDO existing = inspectionOrderMapper.selectLatestByBiz(inspectionOrder.getBizType(), inspectionOrder.getBizId());
        if (existing != null) {
            return existing.getId();
        }
        // 5. 插入
        inspectionOrderMapper.insert(inspectionOrder);
        // 返回
        return inspectionOrder.getId();
    }

    @Override
    public void updateInspectionOrder(InspectionOrderSaveReqVO updateReqVO) {
        // 校验存在
        InspectionOrderDO oldOrder = validateInspectionOrderExists(updateReqVO.getId());
        // §12.252：终态冻结——已判定（PASSED/FAILED）的检验单不可再变更。status 由下方
        // setStatus(null) 挡住，但 bizId 等判定依据字段若可改，等于把 A 单的判定挪给
        // B 单（isQualified(bizType, bizId) 按键放行），必须拦截。与记录侧 §12.238
        // 「终态明细冻结」构成同一原则的两半。
        validateOrderNotJudged(oldOrder);
        // 更新
        InspectionOrderDO updateObj = BeanUtils.toBean(updateReqVO, InspectionOrderDO.class);
        // 禁止通过通用更新修改状态，状态变更必须走 submitInspection 等状态流转方法
        updateObj.setStatus(null);
        // P2 @Version 乐观锁：携带已有版本号，避免并发覆盖
        updateObj.setVersion(oldOrder.getVersion());
        // §12.228：检查影响行数，版本冲突时抛错而非静默成功
        if (inspectionOrderMapper.updateById(updateObj) == 0) {
            throw exception(INSPECTION_ORDER_OPTIMISTIC_LOCK_FAIL);
        }
    }

    @Override
    public void deleteInspectionOrder(Long id) {
        // 校验存在
        InspectionOrderDO order = validateInspectionOrderExists(id);
        // §12.252：终态冻结——终态单是 WMS 入库卡点的历史判据与判定证据链，删单即毁据，拒绝。
        validateOrderNotJudged(order);
        // §12.252：防孤儿——明细只能挂在可写窗口单上（§12.238 记录侧护栏只防「孤儿创建」，
        // 防不了「删父单留下孤儿」）。循计量器具 INSTRUMENT_HAS_CALIBRATIONS 先例：
        // 有明细拒删，不做级联清理（宁拒不蚀，避免静默丢弃质量证据）。
        if (CollUtil.isNotEmpty(inspectionRecordMapper.selectListByOrderId(id))) {
            throw exception(INSPECTION_ORDER_HAS_RECORDS);
        }
        // 删除
        inspectionOrderMapper.deleteById(id);
    }

    @Override
    public InspectionOrderDO getInspectionOrder(Long id) {
        return inspectionOrderMapper.selectById(id);
    }

    @Override
    public PageResult<InspectionOrderDO> getInspectionOrderPage(InspectionOrderPageReqVO pageReqVO) {
        return inspectionOrderMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitInspection(Long orderId, List<InspectionRecordSaveReqVO> records) {
        // 1. 校验检验单存在
        InspectionOrderDO order = validateInspectionOrderExists(orderId);
        // 2. 校验检验单状态：只有待检验或检验中状态才能提交检验结果
        if (!InspectionOrderStatusEnum.PENDING.getStatus().equals(order.getStatus())
                && !InspectionOrderStatusEnum.INSPECTING.getStatus().equals(order.getStatus())) {
            throw exception(INSPECTION_ORDER_NOT_SUBMIT);
        }
        // 2.5 §12.252：空明细拒判（fail-closed）。原实现 records 为空时默认判「检验通过」，
        //     等价于「零证据放行」；且下方第 3 步会先删旧明细——「建单→录明细→submit([])」
        //     即明细被清 + 判通过。判定链其余入口全部 fail-closed（isQualified 无单即 false、
        //     AQL 未配置即拒收、CRITICAL 一票否决），这是唯一的 fail-open 点，不得保留。
        //     拒判点必须在旧明细删除之前：拒绝路径不得产生任何副作用。
        //     §12.252 行为变更（对外契约收紧）：submit([]) 由「通过」改为拒绝。
        if (CollUtil.isEmpty(records)) {
            throw exception(INSPECTION_ORDER_NO_RECORDS);
        }
        // 3. 删除该检验单的原有检验记录
        List<InspectionRecordDO> oldRecords = inspectionRecordMapper.selectListByOrderId(orderId);
        if (CollUtil.isNotEmpty(oldRecords)) {
            inspectionRecordMapper.deleteByIds(oldRecords.stream().map(InspectionRecordDO::getId).toList());
        }
        // 4. 插入新的检验记录
        if (CollUtil.isNotEmpty(records)) {
            LocalDateTime now = LocalDateTime.now();
            List<InspectionRecordDO> recordDOs = BeanUtils.toBean(records, InspectionRecordDO.class);
            recordDOs.forEach(record -> {
                record.setId(null); // 清空 id，确保新增
                record.setOrderId(orderId);
                if (record.getInspectTime() == null) {
                    record.setInspectTime(now);
                }
            });
            inspectionRecordMapper.insertBatch(recordDOs);
        }
        // 5. 基于 AQL（Ac/Re）与缺陷严重度的抽样判定
        //    - 致命缺陷（CRITICAL）且 FAIL → 整单 FAIL（一票否决）
        //    - 统计 MAJOR/MINOR 不合格数：未配置 Ac/Re 时 fail-closed（任何非致命缺陷即不合格）；
        //      配置后按 Ac/Re 判定：缺陷数 > Ac 判不合格（即 >= Re）
        //    说明：判定采用整数计数，避免原 double 比例判定引入的浮点误差与「半数不良仍合格」错误
        Integer orderStatus = InspectionOrderStatusEnum.PASSED.getStatus();
        if (CollUtil.isNotEmpty(records)) {
            long criticalFailCount = records.stream()
                    .filter(record -> InspectionResultEnum.FAIL.getResult().equals(record.getResult())
                            && InspectionSeverityEnum.CRITICAL.getSeverity().equals(record.getSeverity()))
                    .count();
            long majorMinorFailCount = records.stream()
                    .filter(record -> InspectionResultEnum.FAIL.getResult().equals(record.getResult())
                            && !InspectionSeverityEnum.CRITICAL.getSeverity().equals(record.getSeverity()))
                    .count();
            if (criticalFailCount > 0) {
                // 致命缺陷一票否决
                orderStatus = InspectionOrderStatusEnum.FAILED.getStatus();
            } else if (majorMinorFailCount > 0) {
                // 非致命缺陷按 AQL Ac/Re 判定；未配置则 fail-closed（拒收）
                int acceptanceQuantity = order.getAcceptanceQuantity() == null ? 0 : order.getAcceptanceQuantity();
                if (majorMinorFailCount > acceptanceQuantity) {
                    orderStatus = InspectionOrderStatusEnum.FAILED.getStatus();
                }
            }
        }
        // 6. 更新检验单状态与检验时间
        InspectionOrderDO updateObj = new InspectionOrderDO();
        updateObj.setId(orderId);
        updateObj.setStatus(orderStatus);
        updateObj.setVersion(order.getVersion());
        updateObj.setInspectTime(LocalDateTime.now());
        // §12.228：检查影响行数，版本冲突时抛错而非静默成功
        if (inspectionOrderMapper.updateById(updateObj) == 0) {
            throw exception(INSPECTION_ORDER_OPTIMISTIC_LOCK_FAIL);
        }
    }

    private InspectionOrderDO validateInspectionOrderExists(Long id) {
        InspectionOrderDO order = inspectionOrderMapper.selectById(id);
        if (order == null) {
            throw exception(INSPECTION_ORDER_NOT_EXISTS);
        }
        return order;
    }

    /**
     * §12.252：终态冻结判据——PASSED(30)/FAILED(40) 为「已判定」，不可变更或删除。
     *
     * @param order 已校验存在的检验单
     */
    private void validateOrderNotJudged(InspectionOrderDO order) {
        if (InspectionOrderStatusEnum.PASSED.getStatus().equals(order.getStatus())
                || InspectionOrderStatusEnum.FAILED.getStatus().equals(order.getStatus())) {
            throw exception(INSPECTION_ORDER_JUDGED);
        }
    }

}