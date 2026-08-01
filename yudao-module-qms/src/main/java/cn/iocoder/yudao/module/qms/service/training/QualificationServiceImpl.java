package cn.iocoder.yudao.module.qms.service.training;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.QualificationPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.training.vo.QualificationSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.training.QualificationDO;
import cn.iocoder.yudao.module.qms.dal.mysql.training.QualificationMapper;
import cn.iocoder.yudao.module.qms.enums.qms.QualificationStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.QUALIFICATION_NOT_EXISTS;

/**
 * QMS 岗位资格 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class QualificationServiceImpl implements QualificationService {

    @Resource
    private QualificationMapper qualificationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createQualification(QualificationSaveReqVO createReqVO) {
        QualificationDO qualification = BeanUtils.toBean(createReqVO, QualificationDO.class);
        if (qualification.getStatus() == null) {
            qualification.setStatus(QualificationStatusEnum.VALID.getStatus());
        }
        qualificationMapper.insert(qualification);
        return qualification.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQualification(QualificationSaveReqVO updateReqVO) {
        validateQualificationExists(updateReqVO.getId());
        QualificationDO updateObj = BeanUtils.toBean(updateReqVO, QualificationDO.class);
        qualificationMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQualification(Long id) {
        validateQualificationExists(id);
        qualificationMapper.deleteById(id);
    }

    private void validateQualificationExists(Long id) {
        if (qualificationMapper.selectById(id) == null) {
            throw exception(QUALIFICATION_NOT_EXISTS);
        }
    }

    @Override
    public QualificationDO getQualification(Long id) {
        return qualificationMapper.selectById(id);
    }

    @Override
    public PageResult<QualificationDO> getQualificationPage(QualificationPageReqVO pageReqVO) {
        return qualificationMapper.selectPage(pageReqVO);
    }

    @Override
    public List<QualificationDO> getExpiringQualificationList(LocalDate expireDate) {
        return qualificationMapper.selectExpiringList(expireDate);
    }

}