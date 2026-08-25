package cn.zhicloud.module.hr.service.salary;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.hr.controller.admin.salary.vo.HrSalaryApproveReqVO;
import cn.zhicloud.module.hr.controller.admin.salary.vo.HrSalaryCalculateReqVO;
import cn.zhicloud.module.hr.controller.admin.salary.vo.HrSalaryPageReqVO;
import cn.zhicloud.module.hr.controller.admin.salary.vo.HrSalarySaveReqVO;
import cn.zhicloud.module.hr.dal.dataobject.salary.HrSalaryDO;
import jakarta.validation.Valid;

/**
 * HR 薪资记录 Service 接口
 *
 * @author zhicloud
 */
public interface HrSalaryService {

    /**
     * 创建薪资记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSalary(@Valid HrSalarySaveReqVO createReqVO);

    /**
     * 更新薪资记录
     *
     * @param updateReqVO 更新信息
     */
    void updateSalary(@Valid HrSalarySaveReqVO updateReqVO);

    /**
     * 删除薪资记录
     *
     * @param id 编号
     */
    void deleteSalary(Long id);

    /**
     * 获得薪资记录
     *
     * @param id 编号
     * @return 薪资记录
     */
    HrSalaryDO getSalary(Long id);

    /**
     * 获得薪资记录分页
     *
     * @param pageReqVO 分页查询
     * @return 薪资记录分页
     */
    PageResult<HrSalaryDO> getSalaryPage(HrSalaryPageReqVO pageReqVO);

    /**
     * 月度核算：基本工资 + 加班费 + 奖金 - 扣款 - 社保 - 公积金 - 个税 = 实发工资
     *
     * @param reqVO 核算请求
     * @return 薪资记录编号
     */
    Long calculateMonthlySalary(@Valid HrSalaryCalculateReqVO reqVO);

    /**
     * 审核薪资：将草稿状态变更为已审核
     *
     * @param reqVO 审核请求
     */
    void approveSalary(@Valid HrSalaryApproveReqVO reqVO);

}