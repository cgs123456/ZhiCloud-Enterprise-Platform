package cn.zhicloud.module.crm.service.visit;

import cn.hutool.core.lang.Assert;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.crm.controller.admin.visit.vo.CrmVisitRecordPageReqVO;
import cn.zhicloud.module.crm.controller.admin.visit.vo.CrmVisitRecordSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.visit.CrmVisitRecordDO;
import cn.zhicloud.module.crm.dal.mysql.visit.CrmVisitRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.VISIT_RECORD_NOT_EXISTS;

/**
 * CRM 拜访签到记录 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class CrmVisitRecordServiceImpl implements CrmVisitRecordService {

    @Resource
    private CrmVisitRecordMapper visitRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createVisitRecord(CrmVisitRecordSaveReqVO createReqVO) {
        CrmVisitRecordDO visitRecord = BeanUtils.toBean(createReqVO, CrmVisitRecordDO.class);
        visitRecordMapper.insert(visitRecord);
        return visitRecord.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVisitRecord(CrmVisitRecordSaveReqVO updateReqVO) {
        Assert.notNull(updateReqVO.getId(), "拜访签到记录编号不能为空");
        // 1. 校验存在
        validateVisitRecordExists(updateReqVO.getId());
        // 2. 更新
        CrmVisitRecordDO updateObj = BeanUtils.toBean(updateReqVO, CrmVisitRecordDO.class);
        visitRecordMapper.updateById(updateObj);
    }

    @Override
    public void deleteVisitRecord(Long id) {
        // 1. 校验存在
        validateVisitRecordExists(id);
        // 2. 删除
        visitRecordMapper.deleteById(id);
    }

    private void validateVisitRecordExists(Long id) {
        if (visitRecordMapper.selectById(id) == null) {
            throw exception(VISIT_RECORD_NOT_EXISTS);
        }
    }

    @Override
    public CrmVisitRecordDO getVisitRecord(Long id) {
        return visitRecordMapper.selectById(id);
    }

    @Override
    public PageResult<CrmVisitRecordDO> getVisitRecordPage(CrmVisitRecordPageReqVO pageReqVO) {
        return visitRecordMapper.selectPage(pageReqVO);
    }

}
