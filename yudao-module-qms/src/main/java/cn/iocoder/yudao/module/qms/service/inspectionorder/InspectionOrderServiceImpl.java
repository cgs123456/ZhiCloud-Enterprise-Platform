package cn.iocoder.yudao.module.qms.service.inspectionorder;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.FqcInspectionOrderCreateReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo.InspectionOrderSaveReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionorder.InspectionOrderMapper;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionrecord.InspectionRecordMapper;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionBizTypeEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionOrderStatusEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionResultEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionSeverityEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.INSPECTION_ORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.INSPECTION_ORDER_NOT_SUBMIT;

/**
 * QMS 检验单 Service 实现类
 *
 * @author 芋道源码
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
        // 默认状态为待检验
        if (inspectionOrder.getStatus() == null) {
            inspectionOrder.setStatus(InspectionOrderStatusEnum.PENDING.getStatus());
        }
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
        // 4. 插入
        inspectionOrderMapper.insert(inspectionOrder);
        // 返回
        return inspectionOrder.getId();
    }

    @Override
    public void updateInspectionOrder(InspectionOrderSaveReqVO updateReqVO) {
        // 校验存在
        validateInspectionOrderExists(updateReqVO.getId());
        // 更新
        InspectionOrderDO updateObj = BeanUtils.toBean(updateReqVO, InspectionOrderDO.class);
        // 禁止通过通用更新修改状态，状态变更必须走 submitInspection 等状态流转方法
        updateObj.setStatus(null);
        inspectionOrderMapper.updateById(updateObj);
    }

    @Override
    public void deleteInspectionOrder(Long id) {
        // 校验存在
        validateInspectionOrderExists(id);
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
        updateObj.setInspectTime(LocalDateTime.now());
        inspectionOrderMapper.updateById(updateObj);
    }

    private InspectionOrderDO validateInspectionOrderExists(Long id) {
        InspectionOrderDO order = inspectionOrderMapper.selectById(id);
        if (order == null) {
            throw exception(INSPECTION_ORDER_NOT_EXISTS);
        }
        return order;
    }

}