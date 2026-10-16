package cn.zhicloud.module.qms.service.inspectionrecord;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordPageReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionorder.InspectionOrderDO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.zhicloud.module.qms.dal.mysql.inspectionorder.InspectionOrderMapper;
import cn.zhicloud.module.qms.dal.mysql.inspectionrecord.InspectionRecordMapper;
import cn.zhicloud.module.qms.enums.qms.InspectionOrderStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_ORDER_NOT_EXISTS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_RECORD_NOT_EXISTS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_RECORD_ORDER_NOT_EDITABLE;

/**
 * QMS 检验记录 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class InspectionRecordServiceImpl implements InspectionRecordService {

    @Resource
    private InspectionRecordMapper inspectionRecordMapper;

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;

    @Override
    public Long createInspectionRecord(InspectionRecordSaveReqVO createReqVO) {
        // 校验父检验单可写（§12.238）
        validateOrderEditable(createReqVO.getOrderId());
        // 插入
        InspectionRecordDO inspectionRecord = BeanUtils.toBean(createReqVO, InspectionRecordDO.class);
        inspectionRecordMapper.insert(inspectionRecord);
        // 返回
        return inspectionRecord.getId();
    }

    @Override
    public void updateInspectionRecord(InspectionRecordSaveReqVO updateReqVO) {
        // 校验存在（并取出旧记录：父单可写校验与「是否被移动到别的检验单」都基于它）
        InspectionRecordDO oldRecord = validateInspectionRecordExists(updateReqVO.getId());
        // 校验父检验单可写（§12.238）：旧单必须可写；若把记录移动到别的检验单，目标单也必须可写
        validateOrderEditable(oldRecord.getOrderId());
        if (!Objects.equals(oldRecord.getOrderId(), updateReqVO.getOrderId())) {
            validateOrderEditable(updateReqVO.getOrderId());
        }
        // 更新
        InspectionRecordDO updateObj = BeanUtils.toBean(updateReqVO, InspectionRecordDO.class);
        inspectionRecordMapper.updateById(updateObj);
    }

    @Override
    public void deleteInspectionRecord(Long id) {
        // 校验存在（并取出记录：父单可写校验基于它）
        InspectionRecordDO record = validateInspectionRecordExists(id);
        // 校验父检验单可写（§12.238）
        validateOrderEditable(record.getOrderId());
        // 删除
        inspectionRecordMapper.deleteById(id);
    }

    private InspectionRecordDO validateInspectionRecordExists(Long id) {
        InspectionRecordDO record = inspectionRecordMapper.selectById(id);
        if (record == null) {
            throw exception(INSPECTION_RECORD_NOT_EXISTS);
        }
        return record;
    }

    /**
     * 校验父检验单可写（§12.238）：检验单必须存在，且状态为「待检验(10)/检验中(20)」。
     *
     * <p>PASSED(30)/FAILED(40) 只由 submitInspection 依据 AQL 与缺陷严重度判定产生；
     * 一次性判定后明细即冻结。此前记录 CRUD 完全不看父单状态：可给「已通过」的检验单
     * 补一条 CRITICAL 不合格记录，造出「父单 PASSED、明细 FAIL」的台账不一致
     * （WMS 入库质检卡点只读父单状态，据此放行的货实际带着不合格明细）；
     * 也可以给不存在的 orderId 直接写入孤儿明细。父单不存在时同样拒绝。
     */
    private void validateOrderEditable(Long orderId) {
        InspectionOrderDO order = inspectionOrderMapper.selectById(orderId);
        if (order == null) {
            throw exception(INSPECTION_ORDER_NOT_EXISTS);
        }
        if (!InspectionOrderStatusEnum.PENDING.getStatus().equals(order.getStatus())
                && !InspectionOrderStatusEnum.INSPECTING.getStatus().equals(order.getStatus())) {
            throw exception(INSPECTION_RECORD_ORDER_NOT_EDITABLE);
        }
    }

    @Override
    public InspectionRecordDO getInspectionRecord(Long id) {
        return inspectionRecordMapper.selectById(id);
    }

    @Override
    public PageResult<InspectionRecordDO> getInspectionRecordPage(InspectionRecordPageReqVO pageReqVO) {
        return inspectionRecordMapper.selectPage(pageReqVO);
    }

    @Override
    public List<InspectionRecordDO> getInspectionRecordListByOrderId(Long orderId) {
        return inspectionRecordMapper.selectListByOrderId(orderId);
    }

}
