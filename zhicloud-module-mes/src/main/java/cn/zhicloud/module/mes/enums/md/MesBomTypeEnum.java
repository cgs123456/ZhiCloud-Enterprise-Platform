package cn.zhicloud.module.mes.enums.md;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES BOM 类型枚举
 *
 * <p>区分工程 BOM / 制造 BOM / 虚拟件，支持 BOM 多版本管理。
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum MesBomTypeEnum implements ArrayValuable<String> {

    ENGINEERING("ENGINEERING", "工程BOM"),
    MANUFACTURING("MANUFACTURING", "制造BOM"),
    PHANTOM("PHANTOM", "虚拟件");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(MesBomTypeEnum::getType).toArray(String[]::new);

    /**
     * 类型
     */
    private final String type;
    /**
     * 名称
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
