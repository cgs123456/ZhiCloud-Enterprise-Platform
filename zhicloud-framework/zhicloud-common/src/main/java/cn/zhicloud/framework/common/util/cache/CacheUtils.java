package cn.zhicloud.framework.common.util.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cache 工具类
 *
 * @author 智云
 */
public class CacheUtils {

    /**
     * 异步刷新的 LoadingCache 最大缓存数量
     *
     * @see <a href="">本地缓存 CacheUtils 工具类建议</a>
     */
    private static final Integer CACHE_MAX_SIZE = 10000;

    /**
     * 共享的虚拟线程执行器，用于 Guava 异步刷新。
     * 避免每次构建缓存都新建一个 CachedThreadPool 导致线程无法回收
     */
    private static final ExecutorService ASYNC_RELOADER_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 构建异步刷新的 LoadingCache 对象
     *
     * 注意：如果你的缓存和 ThreadLocal 有关系，要么自己处理 ThreadLocal 的传递，要么使用 {@link #buildCache(Duration, CacheLoader)} 方法
     *
     * 或者简单理解：
     * 1、和“人”相关的，使用 {@link #buildCache(Duration, CacheLoader)} 方法
     * 2、和“全局”、“系统”相关的，使用当前缓存方法
     *
     * @param duration 过期时间
     * @param loader  CacheLoader 对象
     * @return LoadingCache 对象
     */
    public static <K, V> LoadingCache<K, V> buildAsyncReloadingCache(Duration duration, CacheLoader<K, V> loader) {
        return CacheBuilder.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                // 只阻塞当前数据加载线程，其他线程返回旧值
                .refreshAfterWrite(duration)
                // 通过共享的虚拟线程执行器实现全异步加载，包括 refreshAfterWrite 被阻塞的加载线程
                .build(CacheLoader.asyncReloading(loader, ASYNC_RELOADER_EXECUTOR));
    }

    /**
     * 构建同步刷新的 LoadingCache 对象
     *
     * @param duration 过期时间
     * @param loader  CacheLoader 对象
     * @return LoadingCache 对象
     */
    public static <K, V> LoadingCache<K, V> buildCache(Duration duration, CacheLoader<K, V> loader) {
        return CacheBuilder.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                // 只阻塞当前数据加载线程，其他线程返回旧值
                .refreshAfterWrite(duration)
                .build(loader);
    }

}
