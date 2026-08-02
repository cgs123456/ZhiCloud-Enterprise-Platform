package cn.iocoder.yudao.framework.mybatis.core.intercept;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Map;

/**
 * 分页「不分页」({@link PageParam#PAGE_SIZE_NONE} = -1) 安全护栏
 *
 * <p>业务导出接口常使用 {@code pageSize = -1} 一次性查出全部数据。若数据量过大，
 * 会同时造成：① JDBC 结果集在应用内存中堆积导致 OOM；② 下游 Excel 写出内存峰值过高。
 * 本拦截器在查询执行前，将 {@code size <= 0} 的 {@link IPage} 改写为安全上限，
 * 既保留「导出全部」语义，又避免无界查询拖垮 JVM。</p>
 *
 * <p>仅对「不分页」参数生效，正常分页（size 1~200，受 {@link PageParam} 的 {@code @Max(200)} 约束）
 * 不受影响，因此不会破坏既有分页行为。</p>
 *
 * @author ZhiCloud 平台加固
 */
public class PageSizeNoneLimitInnerInterceptor implements InnerInterceptor {

    /**
     * 「不分页」请求被改写后的安全上限（行）。超过此量的导出应改用异步导出任务中心。
     */
    public static final long MAX_PAGE_SIZE_NONE = 100_000L;

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler,
                            org.apache.ibatis.mapping.BoundSql boundSql) {
        IPage<?> page = findPage(parameter);
        if (page == null) {
            return;
        }
        Long size = page.getSize();
        // size <= 0 即 MyBatis-Plus 的「不分页」语义（导出接口常用 -1）
        if (size != null && size <= 0 && size != MAX_PAGE_SIZE_NONE) {
            page.setSize(MAX_PAGE_SIZE_NONE);
        }
    }

    /**
     * 从 MyBatis 参数对象中定位 {@link IPage}（与 PaginationInnerInterceptor 所需的定位逻辑一致）。
     * 找不到时返回 {@code null}，保持无操作（fail-safe）。
     */
    @SuppressWarnings("unchecked")
    private IPage<?> findPage(Object parameter) {
        if (parameter == null) {
            return null;
        }
        if (parameter instanceof IPage) {
            return (IPage<?>) parameter;
        }
        if (parameter instanceof Map) {
            for (Object value : ((Map<?, ?>) parameter).values()) {
                if (value instanceof IPage) {
                    return (IPage<?>) value;
                }
            }
        }
        // MapperMethod.ParamMap 同时实现了 Map，上面分支已覆盖；
        // 其余 POJO 包装场景暂不处理，由 PaginationInnerInterceptor 兜底。
        return null;
    }
}
