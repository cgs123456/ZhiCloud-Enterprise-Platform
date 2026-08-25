package cn.zhicloud.module.qms.service.training;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.training.vo.QualificationPageReqVO;
import cn.zhicloud.module.qms.controller.admin.training.vo.QualificationSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.training.QualificationDO;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

/**
 * QMS 岗位资格 Service 接口
 *
 * @author zhicloud
 */
public interface QualificationService {

    Long createQualification(@Valid QualificationSaveReqVO createReqVO);

    void updateQualification(@Valid QualificationSaveReqVO updateReqVO);

    void deleteQualification(Long id);

    QualificationDO getQualification(Long id);

    PageResult<QualificationDO> getQualificationPage(QualificationPageReqVO pageReqVO);

    /**
     * 查询即将到期的资格列表（到期日 <= 指定日期）
     *
     * @param expireDate 到期日阈值
     * @return 即将到期资格列表
     */
    List<QualificationDO> getExpiringQualificationList(LocalDate expireDate);

}