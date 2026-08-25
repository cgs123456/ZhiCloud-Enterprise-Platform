package cn.zhicloud.framework.tenant.core.redis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.redis.core.TimeoutRedisCacheManager;
import cn.zhicloud.framework.redis.core.ZhiCloudCacheKeyPrefix;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.util.Set;

/**
 * 多租户的 {@link RedisCacheManager} 实现类
 *
 * 通过自定义 {@link CacheKeyPrefix}，在计算缓存 Key 时动态读取 {@link TenantContextHolder} 的租户编号，
 * 拼接租户后缀，格式为 name + ":" + tenantId + ":"。从而无需按租户分裂 Cache 实例，避免内存中 Cache 对象无限增长
 *
 * @author airhead
 */
@Slf4j
public class TenantRedisCacheManager extends TimeoutRedisCacheManager {

    private static final String SPLIT = "#";

    public TenantRedisCacheManager(RedisCacheWriter cacheWriter,
                                   RedisCacheConfiguration defaultCacheConfiguration) {
        super(cacheWriter, defaultCacheConfiguration);
    }

    /**
     * 构建「基础 Key 前缀 + 租户后缀」的 CacheKeyPrefix 对象，供装配处传入 RedisCacheConfiguration 使用
     *
     * @param redisKeyPrefix 全局的 key-prefix 配置（spring.cache.redis.key-prefix），可为空
     * @param ignoreCaches   不追加租户后缀的缓存名列表
     * @return CacheKeyPrefix 对象
     */
    public static CacheKeyPrefix buildTenantCacheKeyPrefix(String redisKeyPrefix, Set<String> ignoreCaches) {
        return new TenantCacheKeyPrefix(ZhiCloudCacheKeyPrefix.of(redisKeyPrefix), ignoreCaches);
    }

    /**
     * 多租户的 {@link CacheKeyPrefix} 实现类。
     *
     * 注意：compute 在每次缓存 Key 计算时都会执行，动态读取当前线程的租户上下文，
     * 相比原先按租户分裂 Cache 实例（实例创建后固定）更加准确
     */
    @RequiredArgsConstructor
    public static class TenantCacheKeyPrefix implements CacheKeyPrefix {

        /**
         * 基础前缀：处理全局 key-prefix 与 : 单冒号分隔符
         */
        private final CacheKeyPrefix delegate;

        /**
         * 不追加租户后缀的缓存名列表
         */
        private final Set<String> ignoreCaches;

        @Override
        public String compute(String cacheName) {
            // 移除可能的 #ttl 后缀，仅使用真实缓存名做忽略判断与基础前缀计算
            String bareName = StrUtil.contains(cacheName, SPLIT) ? StrUtil.subBefore(cacheName, SPLIT, false) : cacheName;
            // 基础前缀
            String basePrefix = delegate.compute(bareName);
            // 如果开启多租户，则 Key 拼接租户后缀；ignoreCaches 中的缓存不拼接
            if (!TenantContextHolder.isIgnore()
                    && TenantContextHolder.getTenantId() != null
                    && !CollUtil.contains(ignoreCaches, bareName)) {
                return basePrefix + TenantContextHolder.getTenantId() + StrUtil.COLON;
            }
            return basePrefix;
        }

    }

}
