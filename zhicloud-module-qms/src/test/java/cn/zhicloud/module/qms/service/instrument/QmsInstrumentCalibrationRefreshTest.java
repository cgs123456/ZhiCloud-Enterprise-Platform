package cn.zhicloud.module.qms.service.instrument;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.instrument.QmsInstrumentDO;
import cn.zhicloud.module.qms.dal.mysql.instrument.QmsInstrumentMapper;
import cn.zhicloud.module.qms.enums.instrument.QmsCalibrationResultEnum;
import cn.zhicloud.module.qms.enums.instrument.QmsInstrumentStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import java.time.LocalDate;
import java.util.UUID;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * §12.238：校准记录 → 器具 last/next_calibration_date 派生刷新测试。
 *
 * <p>缺陷形态：旧实现把「本条记录」的日期无条件回写器具 —— 补录一条历史校准会把
 * 器具日期改旧（校准到期预警口径失真）；更新/删除记录又完全不回写（器具日期滞留）。
 *
 * @author 智云
 */
@Import(QmsInstrumentCalibrationServiceImpl.class)
public class QmsInstrumentCalibrationRefreshTest extends BaseDbUnitTest {

    @Resource
    private QmsInstrumentCalibrationServiceImpl calibrationService;

    @Resource
    private QmsInstrumentMapper instrumentMapper;

    private Long createInstrument(Integer cycleDays) {
        QmsInstrumentDO instrument = randomPojo(QmsInstrumentDO.class, o -> {
            o.setCode("INS-REFRESH-" + UUID.randomUUID());
            o.setStatus(QmsInstrumentStatusEnum.IN_USE.getStatus());
            o.setCalibrationCycleDays(cycleDays);
            o.setLastCalibrationDate(null);
            o.setNextCalibrationDate(null);
        });
        instrumentMapper.insert(instrument);
        return instrument.getId();
    }

    private Long record(Long instrumentId, String calibrationNo, String date, String next) {
        QmsInstrumentCalibrationSaveReqVO req = new QmsInstrumentCalibrationSaveReqVO();
        req.setInstrumentId(instrumentId);
        req.setCalibrationNo(calibrationNo);
        req.setCalibrationDate(LocalDate.parse(date));
        req.setCalibrationResult(QmsCalibrationResultEnum.QUALIFIED.getResult());
        if (next != null) {
            req.setNextCalibrationDate(LocalDate.parse(next));
        }
        return calibrationService.recordCalibration(req);
    }

    private void assertDates(Long instrumentId, LocalDate expectedLast, LocalDate expectedNext) {
        QmsInstrumentDO instrument = instrumentMapper.selectById(instrumentId);
        assertEquals(expectedLast, instrument.getLastCalibrationDate());
        assertEquals(expectedNext, instrument.getNextCalibrationDate());
    }

    @Test
    public void test_record_writesDatesFromLatestRecord() {
        Long instrumentId = createInstrument(180);

        record(instrumentId, "CAL-R1-001", "2025-01-10", null);

        // next 为空 → 按周期 180 天推算：2025-01-10 + 180 = 2025-07-09
        assertDates(instrumentId, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 7, 9));
    }

    @Test
    public void test_record_backfill_doesNotRegress() {
        Long instrumentId = createInstrument(180);
        record(instrumentId, "CAL-R2-001", "2025-01-10", null);

        // 补录一条更早的历史校准：器具日期必须保持最新那条，不得被改旧
        record(instrumentId, "CAL-R2-002", "2024-06-01", "2024-12-01");

        assertDates(instrumentId, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 7, 9));
    }

    @Test
    public void test_record_newer_advances() {
        Long instrumentId = createInstrument(180);
        record(instrumentId, "CAL-R3-001", "2025-01-10", null);

        record(instrumentId, "CAL-R3-002", "2025-03-01", "2026-03-01");

        assertDates(instrumentId, LocalDate.of(2025, 3, 1), LocalDate.of(2026, 3, 1));
    }

    @Test
    public void test_update_recomputesFromLatest() {
        Long instrumentId = createInstrument(180);
        record(instrumentId, "CAL-R4-001", "2025-01-10", null);
        Long latestId = record(instrumentId, "CAL-R4-002", "2025-03-01", "2026-03-01");

        // 把最新记录改早（数据订正）：器具日期应回退到真实的最新一条
        QmsInstrumentCalibrationSaveReqVO update = new QmsInstrumentCalibrationSaveReqVO();
        update.setId(latestId);
        update.setInstrumentId(instrumentId);
        update.setCalibrationNo("CAL-R4-002");
        update.setCalibrationDate(LocalDate.of(2024, 1, 1));
        update.setCalibrationResult(QmsCalibrationResultEnum.QUALIFIED.getResult());
        update.setNextCalibrationDate(LocalDate.of(2024, 7, 1));
        calibrationService.updateCalibration(update);

        assertDates(instrumentId, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 7, 9));
    }

    @Test
    public void test_delete_recomputesFromLatest() {
        Long instrumentId = createInstrument(180);
        Long latestId = record(instrumentId, "CAL-R5-001", "2025-01-10", null);
        record(instrumentId, "CAL-R5-002", "2024-06-01", "2024-12-01");

        // 删除最新记录：器具日期回退到次新记录
        calibrationService.deleteCalibration(latestId);

        assertDates(instrumentId, LocalDate.of(2024, 6, 1), LocalDate.of(2024, 12, 1));
    }

    @Test
    public void test_deleteAll_clearsDates() {
        Long instrumentId = createInstrument(180);
        Long r1 = record(instrumentId, "CAL-R6-001", "2025-01-10", null);
        Long r2 = record(instrumentId, "CAL-R6-002", "2024-06-01", "2024-12-01");

        calibrationService.deleteCalibration(r1);
        calibrationService.deleteCalibration(r2);

        // 无有效记录：显式清空（不得滞留指向已删除记录的日期）
        assertDates(instrumentId, null, null);
    }

    @Test
    public void test_update_moveInstrument_recomputesBothSides() {
        Long fromInstrumentId = createInstrument(180);
        Long toInstrumentId = createInstrument(365);
        Long calibrationId = record(fromInstrumentId, "CAL-R7-001", "2025-01-10", null);
        assertDates(fromInstrumentId, LocalDate.of(2025, 1, 10), LocalDate.of(2025, 7, 9));

        QmsInstrumentCalibrationSaveReqVO update = new QmsInstrumentCalibrationSaveReqVO();
        update.setId(calibrationId);
        update.setInstrumentId(toInstrumentId);
        update.setCalibrationNo("CAL-R7-001");
        update.setCalibrationDate(LocalDate.of(2025, 1, 10));
        update.setCalibrationResult(QmsCalibrationResultEnum.QUALIFIED.getResult());
        calibrationService.updateCalibration(update);

        assertDates(fromInstrumentId, null, null); // 移出后：无记录 → 清空
        assertDates(toInstrumentId, LocalDate.of(2025, 1, 10), LocalDate.of(2026, 1, 10)); // 周期 365 天
    }
}
