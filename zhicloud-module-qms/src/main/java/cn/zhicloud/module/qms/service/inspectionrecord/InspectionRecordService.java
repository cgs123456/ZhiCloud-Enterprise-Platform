package cn.zhicloud.module.qms.service.inspectionrecord;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordPageReqVO;
import cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo.InspectionRecordSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS 检验记录 Service 接口
 *
 * @author 智云
 */
public interface InspectionRecordService {

    /**
     * 创建检验记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createInspectionRecord(@Valid InspectionRecordSaveReqVO createReqVO);

    /**
     * 更新检验记录
     *
     * @param updateReqVO 更新信息
     */
    void updateInspectionRecord(@Valid InspectionRecordSaveReqVO updateReqVO);

    /**
     * 删除检验记录
     *
     * @param id 编号
     */
    void deleteInspectionRecord(Long id);

    /**
     * 获得检验记录
     *
     * @param id 编号
     * @return 检验记录
     */
    InspectionRecordDO getInspectionRecord(Long id);

    /**
     * 获得检验记录分页
     *
     * @param pageReqVO 分页查询
     * @return 检验记录分页
     */
    PageResult<InspectionRecordDO> getInspectionRecordPage(InspectionRecordPageReqVO pageReqVO);

    /**
     * 获得指定检验单的检验记录列表
     *
     * @param orderId 检验单 ID
     * @return 检验记录列表
     */
    List<InspectionRecordDO> getInspectionRecordListByOrderId(Long orderId);

}
