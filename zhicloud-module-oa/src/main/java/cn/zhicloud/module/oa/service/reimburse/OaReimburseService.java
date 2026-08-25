package cn.zhicloud.module.oa.service.reimburse;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimbursePageReqVO;
import cn.zhicloud.module.oa.controller.admin.reimburse.vo.OaReimburseSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.reimburse.OaReimburseDO;
import jakarta.validation.Valid;

/**
 * OA 报销单 Service 接口
 *
 * @author zhicloud
 */
public interface OaReimburseService {

    /**
     * 创建报销单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createReimburse(@Valid OaReimburseSaveReqVO createReqVO);

    /**
     * 更新报销单
     *
     * @param updateReqVO 更新信息
     */
    void updateReimburse(@Valid OaReimburseSaveReqVO updateReqVO);

    /**
     * 删除报销单
     *
     * @param id 编号
     */
    void deleteReimburse(Long id);

    /**
     * 获得报销单
     *
     * @param id 编号
     * @return 报销单
     */
    OaReimburseDO getReimburse(Long id);

    /**
     * 获得报销单分页
     *
     * @param pageReqVO 分页查询
     * @return 报销单分页
     */
    PageResult<OaReimburseDO> getReimbursePage(OaReimbursePageReqVO pageReqVO);

    /**
     * 发起报销审批：草稿 -> 审批中
     *
     * @param id 编号
     */
    void submitReimburse(Long id);

    /**
     * 更新报销单状态（审批回调）
     *
     * @param id                编号
     * @param status            状态
     * @param processInstanceId 工作流编号
     */
    void updateReimburseStatus(Long id, Integer status, String processInstanceId);

}
