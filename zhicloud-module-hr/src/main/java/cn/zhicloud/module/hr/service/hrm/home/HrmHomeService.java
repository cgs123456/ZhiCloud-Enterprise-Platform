package cn.zhicloud.module.hr.service.hrm.home;

import cn.zhicloud.module.hr.controller.admin.hrm.home.vo.*;

import java.util.List;

/**
 * HRM 工作台（home）Service 接口（HR 首页统计 + 团队工作台 + 日历）
 *
 * @author zhicloud
 */
public interface HrmHomeService {

    /**
     * 获得 HR 首页统计汇总（员工/招聘/薪资/待办四块概览）
     */
    HrmHrHomeStatisticsRespVO getHrStatisticsSummary();

    /**
     * 获得 HR 工作台日历（个人备忘 + 入职/转正/离职/面试事项）
     */
    List<HrmHomeCalendarItemVO> getHrCalendar(String startDate, String endDate);

    /**
     * 获得团队工作台统计汇总（以当前登录员工为 leader 的团队）
     */
    HrmTeamHomeStatisticsRespVO getTeamStatisticsSummary();

    /**
     * 获得团队工作台日历（团队成员的入职/转正/离职事项）
     */
    List<HrmHomeCalendarItemVO> getTeamCalendar(String startDate, String endDate);

}
