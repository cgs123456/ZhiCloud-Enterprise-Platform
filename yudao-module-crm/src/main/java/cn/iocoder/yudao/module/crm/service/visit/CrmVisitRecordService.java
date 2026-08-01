package cn.iocoder.yudao.module.crm.service.visit;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.crm.controller.admin.visit.vo.CrmVisitRecordPageReqVO;
import cn.iocoder.yudao.module.crm.controller.admin.visit.vo.CrmVisitRecordSaveReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.visit.CrmVisitRecordDO;
import jakarta.validation.Valid;

/**
 * CRM 拜访签到记录 Service 接口
 *
 * @author 芋道源码
 */
public interface CrmVisitRecordService {

    /**
     * 创建拜访签到记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createVisitRecord(@Valid CrmVisitRecordSaveReqVO createReqVO);

    /**
     * 更新拜访签到记录
     *
     * @param updateReqVO 更新信息
     */
    void updateVisitRecord(@Valid CrmVisitRecordSaveReqVO updateReqVO);

    /**
     * 删除拜访签到记录
     *
     * @param id 编号
     */
    void deleteVisitRecord(Long id);

    /**
     * 获得拜访签到记录
     *
     * @param id 编号
     * @return 拜访签到记录
     */
    CrmVisitRecordDO getVisitRecord(Long id);

    /**
     * 获得拜访签到记录分页
     *
     * @param pageReqVO 分页查询
     * @return 拜访签到记录分页
     */
    PageResult<CrmVisitRecordDO> getVisitRecordPage(CrmVisitRecordPageReqVO pageReqVO);

}
