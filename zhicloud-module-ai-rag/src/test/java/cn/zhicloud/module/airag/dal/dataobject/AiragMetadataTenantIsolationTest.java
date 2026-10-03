package cn.zhicloud.module.airag.dal.dataobject;

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
 * AI-RAG 元数据表租户隔离回归测试。
 */
class AiragMetadataTenantIsolationTest {

    @Test
    void knowledgeAndDocumentMustUseTenantBaseDO() {
        assertTrue(TenantBaseDO.class.isAssignableFrom(AiragKnowledgeDO.class));
        assertTrue(TenantBaseDO.class.isAssignableFrom(AiragDocumentDO.class));
    }

    @Test
    void knowledgeAndDocumentMustNotBeIgnoredByTenantInterceptor() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "tenant-isolation-test");
        TableInfoHelper.initTableInfo(assistant, AiragKnowledgeDO.class);
        TableInfoHelper.initTableInfo(assistant, AiragDocumentDO.class);

        TenantDatabaseInterceptor interceptor = new TenantDatabaseInterceptor(new TenantProperties());
        assertFalse(interceptor.ignoreTable("airag_knowledge"));
        assertFalse(interceptor.ignoreTable("airag_document"));
    }
}