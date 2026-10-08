package cn.zhicloud.framework.common.util.number;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link NumberUtils} 的单元测试
 */
public class NumberUtilsTest {

    @Test
    public void testParseLong_numeric() {
        assertEquals(225L, NumberUtils.parseLong("225"));
        assertEquals(-225L, NumberUtils.parseLong("-225"));
        assertEquals(225L, NumberUtils.parseLong("+225")); // 与旧行为一致：hutool isLong 认带符号串
    }

    @Test
    public void testParseLong_nonNumericReturnsNull_notThrow() {
        // 核心回归：审计列 creator 可能不是用户编号（'demo-seed'、'admin'），
        // 旧实现 Long.valueOf 直接抛 NumberFormatException 把整个列表接口打成 500
        assertNull(NumberUtils.parseLong("demo-seed"));
        assertNull(NumberUtils.parseLong("admin"));
        assertNull(NumberUtils.parseLong(" 225"));   // 带空白的数字串：解析不出 = 无对应用户
        assertNull(NumberUtils.parseLong("1e3"));    // 科学计数法不是合法 Long 字面量
        assertNull(NumberUtils.parseLong("9999999999999999999999")); // 溢出 = 不是 Long
    }

    @Test
    public void testParseLong_blankReturnsNull() {
        assertNull(NumberUtils.parseLong(null));
        assertNull(NumberUtils.parseLong(""));
    }

    @Test
    public void testParseInt() {
        assertEquals(225, NumberUtils.parseInt("225"));
        assertNull(NumberUtils.parseInt("demo-seed"));
        assertNull(NumberUtils.parseInt(""));
    }

}
