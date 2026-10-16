package cn.zhicloud.module.qms.controller.admin.instrument.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * §12.245（A 组第 2 项）：器具域 LocalDate 序列化形状测试。
 *
 * <p>背景：§12.238.5 登记全局 {@code write-dates-as-timestamps: true} 下无
 * {@code @JsonFormat} 的 LocalDate 序列化为 {@code [y,m,d]} 数组。本测试用
 * 与全局配置同效的 ObjectMapper（JavaTimeModule + WRITE_DATES_AS_TIMESTAMPS）
 * 先证伪（无注解的 createTime 保持时间戳数字）后证真（有注解的日期输出字符串）。
 *
 * @author 智云
 */
public class QmsInstrumentDateFormatTest {

    private ObjectMapper globalLikeMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 复现 application.yaml: write-dates-as-timestamps: true
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Test
    public void test_calibrationRespVO_datesSerializedAsString() throws Exception {
        QmsInstrumentCalibrationRespVO vo = new QmsInstrumentCalibrationRespVO();
        vo.setCalibrationDate(LocalDate.of(2024, 1, 1));
        vo.setNextCalibrationDate(LocalDate.of(2025, 1, 1));

        String json = globalLikeMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"calibrationDate\":\"2024-01-01\""), json);
        assertTrue(json.contains("\"nextCalibrationDate\":\"2025-01-01\""), json);
    }

    @Test
    public void test_instrumentRespVO_datesSerializedAsString() throws Exception {
        QmsInstrumentRespVO vo = new QmsInstrumentRespVO();
        vo.setLastCalibrationDate(LocalDate.of(2024, 1, 1));
        vo.setNextCalibrationDate(LocalDate.of(2025, 1, 1));

        String json = globalLikeMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"lastCalibrationDate\":\"2024-01-01\""), json);
        assertTrue(json.contains("\"nextCalibrationDate\":\"2025-01-01\""), json);
    }

    @Test
    public void test_controlFieldWithoutJsonFormat_staysTimestamp() throws Exception {
        // 对照：无 @JsonFormat 的 createTime 在该配置下保持数组形状 [y,m,d,H,M,S]，
        // 证明本测试的 mapper 确实复现了全局行为（上面的字符串断言有意义）。
        // 注：生产环境 LocalDateTime 由框架自定义 TimestampLocalDateTimeSerializer 输出 epoch 毫秒数；
        // 此处仅复现 write-dates-as-timestamps 开关对方括号形状的决定作用。
        QmsInstrumentCalibrationRespVO vo = new QmsInstrumentCalibrationRespVO();
        vo.setCreateTime(LocalDateTime.of(2024, 1, 2, 3, 4, 5));

        String json = globalLikeMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"createTime\":[2024,1,2,3,4,5]"), json);
    }

    @Test
    public void test_calibrationSaveReqVO_dateStringDeserialized() throws Exception {
        QmsInstrumentCalibrationSaveReqVO vo = globalLikeMapper().readValue(
                "{\"instrumentId\":1,\"calibrationNo\":\"C1\","
                        + "\"calibrationDate\":\"2024-01-01\",\"calibrationResult\":10}",
                QmsInstrumentCalibrationSaveReqVO.class);

        assertEquals(LocalDate.of(2024, 1, 1), vo.getCalibrationDate());
    }
}
