package cn.zhicloud.server.service.inventory;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.inventory.service.InventoryReconcileReport;
import cn.zhicloud.framework.inventory.service.InventoryReconciliationService;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.enums.GlobalErrorCodeConstants.ERROR_CONFIGURATION;
import static cn.zhicloud.framework.common.exception.enums.GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR;

/**
 * 库存对账的手动外部告警推送服务。
 *
 * <p>本服务只由管理后台手动按钮触发，不注册 Quartz、不注册 Windows Task Scheduler、
 * 不增加任何自动排程。webhook 地址和 token 都从环境变量读取，避免写入数据库或前端。</p>
 */
@Service
public class InventoryReconcileAlertService {

    static final String WEBHOOK_URL_ENV = "ZC_RECONCILE_WEBHOOK_URL";
    static final String WEBHOOK_TOKEN_ENV = "ZC_RECONCILE_WEBHOOK_TOKEN";
    static final String LEGACY_TOKEN_ENV = "ZC_RECONCILE_TOKEN";

    private final InventoryReconciliationService reconciliationService;
    private final RestTemplate restTemplate;
    private final Environment environment;

    public InventoryReconcileAlertService(InventoryReconciliationService reconciliationService,
                                          RestTemplate restTemplate,
                                          Environment environment) {
        this.reconciliationService = reconciliationService;
        this.restTemplate = restTemplate;
        this.environment = environment;
    }

    /**
     * 手动执行一次对账；只有发现不一致时才向配置的 webhook 推送。
     */
    public InventoryReconcileAlertResult pushAlertIfNeeded() {
        InventoryReconcileReport report = reconciliationService.reconcile();
        InventoryReconcileAlertResult result = toResult(report);
        if (report.isConsistent()) {
            result.setMessage("库存对账一致，未推送告警");
            return result;
        }

        String webhook = firstNonBlank(environment.getProperty(WEBHOOK_URL_ENV));
        if (webhook == null) {
            throw new ServiceException(ERROR_CONFIGURATION,
                    "未配置 " + WEBHOOK_URL_ENV + "，无法手动推送库存对账告警");
        }
        URI webhookUri = parseWebhookUri(webhook);
        String token = firstNonBlank(
                environment.getProperty(WEBHOOK_TOKEN_ENV),
                environment.getProperty(LEGACY_TOKEN_ENV));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msg_type", "text");
        payload.put("content", Map.of("text", summarizeMismatches(report)));
        payload.put("source", "inventory-reconcile");
        payload.put("consistent", false);
        payload.put("report", report);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    webhookUri, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ServiceException(INTERNAL_SERVER_ERROR,
                        "库存对账告警 webhook 返回非 2xx: " + response.getStatusCode());
            }
        } catch (RestClientException ex) {
            throw new ServiceException(INTERNAL_SERVER_ERROR, "库存对账告警 webhook 调用失败", ex);
        }

        result.setPushed(true);
        result.setMessage("库存对账不一致，告警已手动推送");
        return result;
    }

    private InventoryReconcileAlertResult toResult(InventoryReconcileReport report) {
        InventoryReconcileAlertResult result = new InventoryReconcileAlertResult();
        result.setConsistent(report.isConsistent());
        result.setPushed(false);
        result.setSourceCount(report.getSourceCount());
        result.setProjectionCount(report.getProjectionCount());
        result.setMismatchCount(report.getMismatches().size());
        return result;
    }

    private URI parseWebhookUri(String webhook) {
        try {
            URI uri = URI.create(webhook);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("webhook 只允许 http/https");
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(ERROR_CONFIGURATION,
                    WEBHOOK_URL_ENV + " 不是合法的 http/https URL", ex);
        }
    }

    private String summarizeMismatches(InventoryReconcileReport report) {
        StringBuilder message = new StringBuilder()
                .append("库存对账不一致：source=").append(report.getSourceCount())
                .append(" projection=").append(report.getProjectionCount())
                .append(" mismatches=").append(report.getMismatches().size());
        report.getMismatches().stream().limit(10).forEach(item -> message
                .append("\n- source=").append(item.getSource())
                .append(" item=").append(item.getItemId())
                .append(" wh=").append(item.getWarehouseId())
                .append(" truth=").append(item.getTruthQuantity())
                .append(" projection=").append(item.getProjectionQuantity()));
        if (report.getMismatches().size() > 10) {
            message.append("\n- ...另有 ").append(report.getMismatches().size() - 10).append(" 条");
        }
        return message.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

}
