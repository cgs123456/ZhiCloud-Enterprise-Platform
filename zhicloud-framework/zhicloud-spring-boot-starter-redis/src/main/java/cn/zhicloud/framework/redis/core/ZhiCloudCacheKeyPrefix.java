package cn.zhicloud.framework.redis.core;

import cn.hutool.core.util.StrUtil;
import org.springframework.data.redis.cache.CacheKeyPrefix;

/**
 * 统一的缓存 Key 前缀实现类：使用 : 单冒号分隔，支持全局 key-prefix 配置。
 *
 * 详细可见 https://blog.csdn.net/chuixue24/article/details/103928965 博客
 *
 * @author 智云
 */
public class ZhiCloudCacheKeyPrefix implements CacheKeyPrefix {

    /**
     * 全局的 key-prefix 配置，可为空
     */
    private final String keyPrefix;

    private ZhiCloudCacheKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    /**
     * 创建 ZhiCloudCacheKeyPrefix 对象
     *
     * @param keyPrefix 全局的 key-prefix 配置，可为空
     * @return ZhiCloudCacheKeyPrefix 对象
     */
    public static ZhiCloudCacheKeyPrefix of(String keyPrefix) {
        return new ZhiCloudCacheKeyPrefix(keyPrefix);
    }

    @Override
    public String compute(String cacheName) {
        if (!StrUtil.isNotBlank(keyPrefix)) {
            return cacheName + StrUtil.COLON;
        }
        String normalizedKeyPrefix = keyPrefix.lastIndexOf(StrUtil.COLON) == -1 ? keyPrefix + StrUtil.COLON : keyPrefix;
        return normalizedKeyPrefix + cacheName + StrUtil.COLON;
    }

}
