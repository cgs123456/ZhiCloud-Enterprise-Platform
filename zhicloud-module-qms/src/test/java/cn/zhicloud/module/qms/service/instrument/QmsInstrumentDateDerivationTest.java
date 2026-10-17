
package cn.zhicloud.module.qms.service.instrument;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.instrument.QmsInstrumentDO;
import cn.zhicloud.module.qms.dal.mysql.instrument.QmsInstrumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import jakarta.annotation.Resource;

import java.time.LocalDate;

import static cn.zhicloud.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * §12.256：器具校准日期「严格派生」写窗口收口测试（承接 §12.238.5.1 登记）。
 *
 * <p>last/next_calibration_date 是「校准记录集」的派生值（§12.238 D2），
 * save 路径两入口（create/update）均不采纳外部写入。
 *
 * <p><b>断言语义循 QmsInstrumentUpdateStatusGuardTest</b>：update 侧置 null 的实际效果
 * 是「该列不进 UPDATE 语句」→ 库中值<b>保持不变</b>，而不是「被置空」；
 * 显式清空只能发生在 refresh 无有效记录时（QmsInstrumentCalibrationRefreshTest 守护）。
 *
 * @author 智云
 */
@Import(QmsInstrumentServiceImpl.class)
public class QmsInstrumentDateDerivationTest extends BaseDbUnitTest {

    @Resource
    private QmsInstrumentServiceImpl service;

    @Resource
    private QmsInstrumentMapper mapper;

    private QmsInstrumentSaveReqVO buildSaveReqVO() {
        QmsInstrumentSaveReqVO req = new QmsInstrumentSaveReqVO();
        req.setCode("DATE-TEST-" + System.nanoTime());
        req.setName("派生校准日期测试器具");
        req.setCategory(10);
        return req;
    }

    @Test
    public void test_createInstrument_ignoresSuppliedDates() {
        QmsInstrumentSaveReqVO req = buildSaveReqVO();
        req.setLastCalibrationDate(LocalDate.of(2026, 1, 5));
        req.setNextCalibrationDate(LocalDate.of(2027, 1, 5));

        Long id = service.createInstrument(req);

        QmsInstrumentDO after = mapper.selectById(id);
        assertNull(after.getLastCalibrationDate(),
                "create 不播种日期：日期只能由校准记录 refresh 产生（§12.256）");
        assertNull(after.getNextCalibrationDate(),
                "create 不播种日期（§12.256）");
    }

    @Test
    public void test_updateInstrument_ignoresSuppliedDates_andStillUpdatesOtherFields() {
        QmsInstrumentDO existing = randomPojo(QmsInstrumentDO.class, o -> {
            o.setLastCalibrationDate(LocalDate.of(2020, 7, 1));
            o.setNextCalibrationDate(LocalDate.of(2021, 7, 1));
        });
        mapper.insert(existing);

        QmsInstrumentSaveReqVO req = buildSaveReqVO();
        req.setId(existing.getId());
        req.setName("改名后的器具");                       // 其它字段仍应被更新
        req.setLastCalibrationDate(LocalDate.of(2030, 6, 1));
        req.setNextCalibrationDate(LocalDate.of(2031, 6, 1));

        service.updateInstrument(req);

        QmsInstrumentDO after = mapper.selectById(existing.getId());
        assertEquals(LocalDate.of(2020, 7, 1), after.getLastCalibrationDate(),
                "update 不改写日期：值只能由校准记录 refresh 产生（§12.256）");
        assertEquals(LocalDate.of(2021, 7, 1), after.getNextCalibrationDate(),
                "update 不改写日期（§12.256）");
        assertEquals("改名后的器具", after.getName(),
                "null-skip 只排除日期列，其余字段照常更新（护栏不得殃及正常修改）");
    }

    @Test
    public void test_updateInstrument_withoutDates_keepsOriginalDates() {
        QmsInstrumentDO existing = randomPojo(QmsInstrumentDO.class, o -> {
            o.setLastCalibrationDate(LocalDate.of(2024, 2, 29));
            o.setNextCalibrationDate(LocalDate.of(2025, 2, 28));
        });
        mapper.insert(existing);

        QmsInstrumentSaveReqVO req = buildSaveReqVO();
        req.setId(existing.getId());

        service.updateInstrument(req);

        QmsInstrumentDO after = mapper.selectById(existing.getId());
        assertEquals(LocalDate.of(2024, 2, 29), after.getLastCalibrationDate(),
                "VO 未传日期时原值保持（null-skip）；不得被误清空——清空只能由 refresh 无有效记录时发生");
        assertEquals(LocalDate.of(2025, 2, 28), after.getNextCalibrationDate(),
                "VO 未传日期时原值保持（null-skip）");
    }
}
