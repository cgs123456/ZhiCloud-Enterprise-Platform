package cn.iocoder.yudao.module.mes.service.pro.piecework;

import java.util.List;

/**
 * MES 计件工资汇总 Service 接口
 *
 * @author 芋道源码
 */
public interface MesProPieceworkSummaryService {

    /**
     * 按员工 + 月份汇总计件工资
     *
     * @param periodMonth 月份（yyyyMM）
     * @return 汇总列表（按员工编号升序）
     */
    List<MesProPieceworkSummaryDTO> summaryByPeriod(String periodMonth);

    /**
     * 按指定员工 + 月份汇总计件工资
     *
     * @param feedbackUserId 报工用户编号
     * @param periodMonth 月份（yyyyMM）
     * @return 汇总结果；无数据返回 null
     */
    MesProPieceworkSummaryDTO summaryByUserAndPeriod(Long feedbackUserId, String periodMonth);

}
