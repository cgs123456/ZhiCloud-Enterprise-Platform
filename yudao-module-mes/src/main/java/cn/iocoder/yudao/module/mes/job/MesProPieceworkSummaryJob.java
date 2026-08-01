package cn.iocoder.yudao.module.mes.job;

import cn.iocoder.yudao.module.mes.service.pro.piecework.MesProPieceworkSummaryDTO;
import cn.iocoder.yudao.module.mes.service.pro.piecework.MesProPieceworkSummaryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * MES 计件工资月度汇总 Job
 *
 * <p>每月末 23:00 汇总当月计件明细，生成员工维度工资汇总。
 *
 * <p>实现说明：Spring {@code @Scheduled} 的 cron 不支持 {@code L}（月末）通配符，
 * 因此在每月 28-31 日 23:00 触发，执行时判断当天是否为当月最后一天，非月末则跳过。
 *
 * @author 芋道源码
 */
@Component
@Slf4j
public class MesProPieceworkSummaryJob {

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Resource
    private MesProPieceworkSummaryService pieceworkSummaryService;

    /**
     * 每月 28-31 日 23:00 触发，仅当当天为当月最后一天时执行汇总
     */
    @Scheduled(cron = "0 0 23 28-31 * ?")
    public void summary() {
        LocalDate today = LocalDate.now();
        if (!isLastDayOfMonth(today)) {
            return;
        }
        String periodMonth = today.format(PERIOD_FORMATTER);
        log.info("[summary][开始汇总 {} 月计件工资]", periodMonth);
        try {
            List<MesProPieceworkSummaryDTO> result = pieceworkSummaryService.summaryByPeriod(periodMonth);
            log.info("[summary][汇总完成，共 {} 名员工]", result.size());
        } catch (Exception e) {
            log.error("[summary][汇总 {} 月计件工资失败]", periodMonth, e);
        }
    }

    /**
     * 判断当天是否为当月最后一天
     */
    private boolean isLastDayOfMonth(LocalDate date) {
        return date.equals(date.withDayOfMonth(date.lengthOfMonth()));
    }

}
