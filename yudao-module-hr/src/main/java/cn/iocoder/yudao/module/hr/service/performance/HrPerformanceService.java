package cn.iocoder.yudao.module.hr.service.performance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.hr.controller.admin.performance.vo.HrPerformanceDeptRankingRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.performance.vo.HrPerformancePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.performance.vo.HrPerformanceSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.performance.HrPerformanceDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * HR 绩效记录 Service 接口
 *
 * @author yudao
 */
public interface HrPerformanceService {

    /**
     * 创建绩效记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPerformance(@Valid HrPerformanceSaveReqVO createReqVO);

    /**
     * 更新绩效记录
     *
     * @param updateReqVO 更新信息
     */
    void updatePerformance(@Valid HrPerformanceSaveReqVO updateReqVO);

    /**
     * 删除绩效记录
     *
     * @param id 编号
     */
    void deletePerformance(Long id);

    /**
     * 获得绩效记录
     *
     * @param id 编号
     * @return 绩效记录
     */
    HrPerformanceDO getPerformance(Long id);

    /**
     * 获得绩效记录分页
     *
     * @param pageReqVO 分页查询
     * @return 绩效记录分页
     */
    PageResult<HrPerformanceDO> getPerformancePage(HrPerformancePageReqVO pageReqVO);

    /**
     * 按周期查询绩效记录
     *
     * @param period 考核周期
     * @return 绩效记录列表
     */
    List<HrPerformanceDO> getPerformanceListByPeriod(String period);

    /**
     * 获得部门绩效排名
     *
     * @param deptId 部门编号
     * @param period 考核周期
     * @return 部门绩效排名列表
     */
    List<HrPerformanceDeptRankingRespVO> getDepartmentRanking(Long deptId, String period);

}