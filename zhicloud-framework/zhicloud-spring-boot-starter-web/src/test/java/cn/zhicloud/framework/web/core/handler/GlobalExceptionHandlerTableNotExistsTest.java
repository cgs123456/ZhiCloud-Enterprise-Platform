package cn.zhicloud.framework.web.core.handler;

import cn.zhicloud.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.zhicloud.framework.common.pojo.CommonResult;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link GlobalExceptionHandler} 对「表不存在」的对外文案测试。
 *
 * <p>钉住的是措辞的事实性：以前这里按表名前缀把异常断言成"某某模块 已禁用/未启用"，
 * 而模块明明在跑（真机上商城、CRM 都正常服务），于是一句假结论把人带去查"要不要开启模块"，
 * 而真正的原因是迁移没执行到位。
 */
public class GlobalExceptionHandlerTableNotExistsTest {

    private CommonResult<?> handle(String table) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler("zhicloud-server", mock(ApiErrorLogCommonApi.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/admin-api/demo/table-missing");
        when(request.getMethod()).thenReturn("GET");
        Throwable ex = new RuntimeException(new SQLException(
                "Table 'zhicloud_platform." + table + "' doesn't exist"));
        return handler.allExceptionHandler(request, ex);
    }

    @Test
    public void shouldStateMissingTableInsteadOfClaimingModuleDisabled() {
        CommonResult<?> result = handle("wms_inventory_log");
        String msg = String.valueOf(result.getMsg());
        assertTrue(msg.contains("wms_inventory_log"), "文案必须带上真实缺失的表名：" + msg);
        assertTrue(msg.contains("不存在"), "应陈述事实：" + msg);
        assertFalse(msg.contains("已禁用"), "不能再断言模块被禁用：" + msg);
        assertFalse(msg.contains("未启用"), "不能再断言模块未启用：" + msg);

        // 历史上"已禁用"这三个字正是从商城分支（product_/promotion_/trade_）吐出来的，单独钉住
        String mallMsg = String.valueOf(handle("product_spu").getMsg());
        assertTrue(mallMsg.contains("product_spu"), "商城分支也要给表名：" + mallMsg);
        assertFalse(mallMsg.contains("已禁用"), "商城明明在跑，不能被报成已禁用：" + mallMsg);
    }

    @Test
    public void shouldGiveModuleDocHintWhenPrefixIsKnown() {
        String msg = String.valueOf(handle("crm_contract").getMsg());
        assertTrue(msg.contains("zhicloud-module-crm"), "命中已知前缀时要给出该看哪份文档：" + msg);
    }

    @Test
    public void shouldNotGuessWhenPrefixIsUnknown() {
        // 认不出归属模块时不替用户下结论（以前这里是 return null 交给通用处理，保持原语义）
        String msg = String.valueOf(handle("some_unknown_table").getMsg());
        assertFalse(msg.contains("zhicloud-module-"), "未知前缀不应硬套某个模块：" + msg);
    }

}
