package cn.zhicloud.module.qms.service.inspectionrecord;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordPageReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.zhicloud.module.qms.dal.mysql.inspectionrecord.InspectionRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSPECTION_RECORD_NOT_EXISTS;

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

    @Override
    public Long createInspectionRecord(InspectionRecordSaveReqVO createReqVO) {
        // 插入
        InspectionRecordDO inspectionRecord = BeanUtils.toBean(createReqVO, InspectionRecordDO.class);
        inspectionRecordMapper.insert(inspectionRecord);
        // 返回
        return inspectionRecord.getId();
    }

    @Override
    public void updateInspectionRecord(InspectionRecordSaveReqVO updateReqVO) {
        // 校验存在
        validateInspectionRecordExists(updateReqVO.getId());
        // 更新
        InspectionRecordDO updateObj = BeanUtils.toBean(updateReqVO, InspectionRecordDO.class);
        inspectionRecordMapper.updateById(updateObj);
    }

    @Override
    public void deleteInspectionRecord(Long id) {
        // 校验存在
        validateInspectionRecordExists(id);
        // 删除
        inspectionRecordMapper.deleteById(id);
    }

    private void validateInspectionRecordExists(Long id) {
        if (inspectionRecordMapper.selectById(id) == null) {
            throw exception(INSPECTION_RECORD_NOT_EXISTS);
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
