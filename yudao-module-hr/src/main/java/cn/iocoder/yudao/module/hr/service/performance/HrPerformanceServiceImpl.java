package cn.iocoder.yudao.module.hr.service.performance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.hr.controller.admin.performance.vo.HrPerformanceDeptRankingRespVO;
import cn.iocoder.yudao.module.hr.controller.admin.performance.vo.HrPerformancePageReqVO;
import cn.iocoder.yudao.module.hr.controller.admin.performance.vo.HrPerformanceSaveReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.employee.HrEmployeeDO;
import cn.iocoder.yudao.module.hr.dal.dataobject.performance.HrPerformanceDO;
import cn.iocoder.yudao.module.hr.dal.mysql.performance.HrPerformanceMapper;
import cn.iocoder.yudao.module.hr.service.employee.HrEmployeeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.hr.enums.ErrorCodeConstants.HR_PERFORMANCE_NOT_EXISTS;

/**
 * HR 绩效记录 Service 实现类
 *
 * @author yudao
 */
@Service
@Validated
public class HrPerformanceServiceImpl implements HrPerformanceService {

    @Resource
    private HrPerformanceMapper performanceMapper;
    @Resource
    private HrEmployeeService employeeService;

    @Override
    public Long createPerformance(HrPerformanceSaveReqVO createReqVO) {
        HrPerformanceDO performance = BeanUtils.toBean(createReqVO, HrPerformanceDO.class);
        performanceMapper.insert(performance);
        return performance.getId();
    }

    @Override
    public void updatePerformance(HrPerformanceSaveReqVO updateReqVO) {
        // 校验存在
        validatePerformanceExists(updateReqVO.getId());
        // 更新
        HrPerformanceDO updateObj = BeanUtils.toBean(updateReqVO, HrPerformanceDO.class);
        performanceMapper.updateById(updateObj);
    }

    @Override
    public void deletePerformance(Long id) {
        // 校验存在
        validatePerformanceExists(id);
        // 删除
        performanceMapper.deleteById(id);
    }

    private void validatePerformanceExists(Long id) {
        if (performanceMapper.selectById(id) == null) {
            throw exception(HR_PERFORMANCE_NOT_EXISTS);
        }
    }

    @Override
    public HrPerformanceDO getPerformance(Long id) {
        return performanceMapper.selectById(id);
    }

    @Override
    public PageResult<HrPerformanceDO> getPerformancePage(HrPerformancePageReqVO pageReqVO) {
        return performanceMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HrPerformanceDO> getPerformanceListByPeriod(String period) {
        return performanceMapper.selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HrPerformanceDO>()
                .eq(HrPerformanceDO::getPeriod, period));
    }

    @Override
    public List<HrPerformanceDeptRankingRespVO> getDepartmentRanking(Long deptId, String period) {
        // 1. 获取部门下所有员工
        List<HrEmployeeDO> employees = employeeService.getEmployeeListByDeptIds(Collections.singleton(deptId));
        if (employees.isEmpty()) {
            return Collections.emptyList();
        }
        // 2. 获取这些员工在该周期的绩效记录
        List<Long> employeeIds = employees.stream().map(HrEmployeeDO::getId).collect(Collectors.toList());
        List<HrPerformanceDO> performanceList = performanceMapper.selectListByEmployeeIdsAndPeriod(employeeIds, period);
        // 3. 按得分降序排序
        performanceList.sort(Comparator.comparing(HrPerformanceDO::getScore,
                Comparator.nullsLast(Comparator.reverseOrder())));
        // 4. 构建排名列表
        Map<Long, HrPerformanceDO> performanceMap = performanceList.stream()
                .collect(Collectors.toMap(HrPerformanceDO::getEmployeeId, p -> p, (a, b) -> a));
        List<HrPerformanceDeptRankingRespVO> rankingList = new java.util.ArrayList<>();
        int rank = 1;
        for (HrPerformanceDO performance : performanceList) {
            HrPerformanceDeptRankingRespVO ranking = new HrPerformanceDeptRankingRespVO();
            ranking.setRank(rank++);
            ranking.setEmployeeId(performance.getEmployeeId());
            ranking.setPeriod(period);
            ranking.setScore(performance.getScore());
            ranking.setGrade(performance.getGrade());
            rankingList.add(ranking);
        }
        return rankingList;
    }

}