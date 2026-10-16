package cn.zhicloud.module.qms.service.training;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.training.vo.QualificationPageReqVO;
import cn.zhicloud.module.qms.controller.admin.training.vo.QualificationSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.QualificationDO;
import cn.zhicloud.module.qms.dal.mysql.training.QualificationMapper;
import cn.zhicloud.module.qms.enums.qms.QualificationStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.QUALIFICATION_NOT_EXISTS;

/**
 * QMS 岗位资格 Service 实现类
 *
 * @author zhicloud
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
        // 状态无条件置为初始值，忽略调用方传入的任何 status。
        // §12.235.5：此前「仅在 null 时补默认」，调用方即可在**建单**这一步直接写入终态，
        // 跳过整个流转（该类可能连流转方法都没有），产生「未审即批/未做即完成」的记录。
        // 状态只能由本模块的流转方法产生。
        qualification.setStatus(QualificationStatusEnum.VALID.getStatus());
        qualificationMapper.insert(qualification);
        return qualification.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQualification(QualificationSaveReqVO updateReqVO) {
        validateQualificationExists(updateReqVO.getId());
        QualificationDO updateObj = BeanUtils.toBean(updateReqVO, QualificationDO.class);
        // 状态不接受外部写入（§12.236.7）。本类**没有任何流转方法**，
        // 若允许通用修改写状态，则「新增 + 修改」两步就能到达任意终态，
        // 新增侧刚加的护栏形同虚设。
        // MyBatis-Plus 的 updateById 跳过 null 字段，故置 null 的效果是
        // 该列不进 UPDATE 语句 —— 调用方的 status 无法生效。
        updateObj.setStatus(null);
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