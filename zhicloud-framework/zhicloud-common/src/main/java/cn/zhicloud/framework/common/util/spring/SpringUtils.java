package cn.zhicloud.framework.common.util.spring;

import cn.hutool.extra.spring.SpringUtil;

import java.util.Objects;

/**
 * Spring 工具类
 *
 * @author 智云
 */
public class SpringUtils extends SpringUtil {

    /**
     * 是否为生产环境
     *
     * @return 是否生产环境
     */
    public static boolean isProd() {
        String activeProfile = getActiveProfile();
        return Objects.equals("prod", activeProfile);
    }

    /**
     * 获取当前 Bean 的代理实例，用于方法内调用需要事务切面的方法。
     * <p>
     * 场景：当 Service 内部方法 A 调用方法 B（B 有 @Transactional），
     * 直接调用 B 不会触发事务，需通过代理调用。
     *
     * @param beanClass Bean 类型
     * @param <T>       Bean 类型
     * @return Bean 代理实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSelf(Class<T> beanClass) {
        return (T) getBean(beanClass);
    }

}
