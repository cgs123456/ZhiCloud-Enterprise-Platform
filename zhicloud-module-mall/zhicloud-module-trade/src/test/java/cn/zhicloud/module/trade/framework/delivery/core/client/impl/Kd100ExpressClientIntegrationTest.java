package cn.zhicloud.module.trade.framework.delivery.core.client.impl;

import cn.zhicloud.framework.common.util.json.JsonUtils;
import cn.zhicloud.module.trade.framework.delivery.config.TradeExpressProperties;
import cn.zhicloud.module.trade.framework.delivery.core.client.dto.ExpressTrackQueryReqDTO;
import cn.zhicloud.module.trade.framework.delivery.core.client.dto.ExpressTrackRespDTO;
import cn.zhicloud.module.trade.framework.delivery.core.client.impl.kd100.Kd100ExpressClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import org.junit.jupiter.api.Disabled;

/**
 * {@link Kd100ExpressClient} 的集成测试
 *
 * @author jason
 */
@Slf4j
@Disabled // 真实第三方快递 API 探针，不作为本地业务流程用例运行
public class Kd100ExpressClientIntegrationTest {

    private Kd100ExpressClient client;

    @BeforeEach
    public void init() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        TradeExpressProperties.Kd100Config config = new TradeExpressProperties.Kd100Config()
                .setKey("pLXUGAwK5305")
                .setCustomer("E77DF18BE109F454A5CD319E44BF5177");
        client = new Kd100ExpressClient(restTemplate, config);
    }

    @Test
    public void testGetExpressTrackList() {
        ExpressTrackQueryReqDTO reqDTO = new ExpressTrackQueryReqDTO();
        reqDTO.setExpressCode("STO");
        reqDTO.setLogisticsNo("773220402764314");
        List<ExpressTrackRespDTO> tracks = client.getExpressTrackList(reqDTO);
        System.out.println(JsonUtils.toJsonPrettyString(tracks));
    }

}
