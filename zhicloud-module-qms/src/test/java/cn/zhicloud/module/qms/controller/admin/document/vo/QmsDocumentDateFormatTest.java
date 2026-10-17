package cn.zhicloud.module.qms.controller.admin.document.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * §12.254：文档域 LocalDate 序列化形状测试（循 §12.245 QmsInstrumentDateFormatTest 母本）。
 *
 * <p>背景：全局 {@code write-dates-as-timestamps: true} 下无 {@code @JsonFormat}
 * 的 {@code LocalDate} 序列化为 {@code [y,m,d]} 数组。本测试用与全局配置同效的
 * ObjectMapper（JavaTimeModule + WRITE_DATES_AS_TIMESTAMPS）先证伪（无注解的
 * createTime 保持数组形状）后证真（有注解的日期输出/解析字符串）。
 *
 * @author 智云
 */
public class QmsDocumentDateFormatTest {

    private ObjectMapper globalLikeMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // 复现 application.yaml: write-dates-as-timestamps: true
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Test
    public void test_documentRespVO_datesSerializedAsString() throws Exception {
        QmsDocumentRespVO vo = new QmsDocumentRespVO();
        vo.setEffectiveDate(LocalDate.of(2024, 1, 1));
        vo.setExpiryDate(LocalDate.of(2025, 12, 31));

        String json = globalLikeMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"effectiveDate\":\"2024-01-01\""), json);
        assertTrue(json.contains("\"expiryDate\":\"2025-12-31\""), json);
    }

    @Test
    public void test_distributeRespVO_datesSerializedAsString() throws Exception {
        QmsDocumentDistributeRespVO vo = new QmsDocumentDistributeRespVO();
        vo.setDistributeDate(LocalDate.of(2024, 1, 1));
        vo.setReceivedDate(LocalDate.of(2024, 1, 2));
        vo.setReturnedDate(LocalDate.of(2025, 1, 1));

        String json = globalLikeMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"distributeDate\":\"2024-01-01\""), json);
        assertTrue(json.contains("\"receivedDate\":\"2024-01-02\""), json);
        assertTrue(json.contains("\"returnedDate\":\"2025-01-01\""), json);
    }

    @Test
    public void test_changeRequestRespVO_applyDateSerializedAsString() throws Exception {
        QmsDocumentChangeRequestRespVO vo = new QmsDocumentChangeRequestRespVO();
        vo.setApplyDate(LocalDate.of(2024, 2, 3));

        String json = globalLikeMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"applyDate\":\"2024-02-03\""), json);
    }

    @Test
    public void test_controlFieldWithoutJsonFormat_staysArray() throws Exception {
        // 对照：无 @JsonFormat 的 createTime 在该配置下保持数组形状。
        // 注：生产环境 LocalDateTime 由框架自定义 TimestampLocalDateTimeSerializer 输出 epoch 毫秒数；
        // 此处仅复现 write-dates-as-timestamps 开关对方括号形状的决定作用。
        QmsDocumentRespVO vo = new QmsDocumentRespVO();
        vo.setCreateTime(LocalDateTime.of(2024, 1, 2, 3, 4, 5));

        String json = globalLikeMapper().writeValueAsString(vo);

        assertTrue(json.contains("\"createTime\":[2024,1,2,3,4,5]"), json);
    }

    @Test
    public void test_documentSaveReqVO_dateStringDeserialized() throws Exception {
        // Save 侧：前端 value-format="YYYY-MM-DD" 的字符串提交必须能反序列化
        QmsDocumentSaveReqVO vo = globalLikeMapper().readValue(
                "{\"docNo\":\"DOC-T1\",\"title\":\"T\","
                        + "\"effectiveDate\":\"2024-01-01\",\"expiryDate\":\"2025-12-31\"}",
                QmsDocumentSaveReqVO.class);

        assertEquals(LocalDate.of(2024, 1, 1), vo.getEffectiveDate());
        assertEquals(LocalDate.of(2025, 12, 31), vo.getExpiryDate());
    }
}
