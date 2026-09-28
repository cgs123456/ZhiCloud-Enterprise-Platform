package cn.zhicloud.module.ai.dal.dataobject.prompt;

import cn.zhicloud.framework.tenant.config.TenantProperties;
import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.framework.tenant.core.db.TenantDatabaseInterceptor;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI Prompt 模板租户隔离回归测试。
 *
 * <p>Prompt 模板按租户保存，且 {@code selectByCode} 依赖 MyBatis-Plus 自动追加
 * {@code tenant_id} 条件；一旦退回 BaseDO，租户间会互相覆盖模板。
 */
class AiPromptTemplateTenantIsolationTest {

    @Test
    void promptTemplateMustUseTenantBaseDO() {
        assertTrue(TenantBaseDO.class.isAssignableFrom(AiPromptTemplateDO.class));
    }

    @Test
    void promptTemplateMustNotBeIgnoredByTenantInterceptor() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "tenant-isolation-test");
        TableInfoHelper.initTableInfo(assistant, AiPromptTemplateDO.class);

        TenantDatabaseInterceptor interceptor = new TenantDatabaseInterceptor(new TenantProperties());
        assertFalse(interceptor.ignoreTable("ai_prompt_template"));
    }
}