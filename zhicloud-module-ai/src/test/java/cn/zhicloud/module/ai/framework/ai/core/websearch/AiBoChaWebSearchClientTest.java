package cn.zhicloud.module.ai.framework.ai.core.websearch;

import cn.zhicloud.framework.common.util.json.JsonUtils;
import cn.zhicloud.module.ai.framework.ai.core.webserch.AiWebSearchRequest;
import cn.zhicloud.module.ai.framework.ai.core.webserch.AiWebSearchResponse;
import cn.zhicloud.module.ai.framework.ai.core.webserch.bocha.AiBoChaWebSearchClient;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * {@link AiBoChaWebSearchClient} 集成测试类
 *
 * @author 智云
 */
public class AiBoChaWebSearchClientTest {

    private final String apiKey = System.getenv("BOCHA_API_KEY");

    private final AiBoChaWebSearchClient webSearchClient = new AiBoChaWebSearchClient(apiKey);

    @Test
    @Disabled
    public void testSearch() {
        Assumptions.assumeTrue(apiKey != null, "跳过：未配置 BOCHA_API_KEY 环境变量");
        AiWebSearchRequest request = new AiWebSearchRequest()
                .setQuery("阿里巴巴")
                .setCount(3);
        AiWebSearchResponse response = webSearchClient.search(request);
        System.out.println(JsonUtils.toJsonPrettyString(response));
    }

}