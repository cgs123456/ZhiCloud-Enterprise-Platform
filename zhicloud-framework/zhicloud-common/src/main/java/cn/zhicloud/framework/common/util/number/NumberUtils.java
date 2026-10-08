package cn.zhicloud.framework.common.util.number;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数字的工具类，补全 {@link cn.hutool.core.util.NumberUtil} 的功能
 *
 * @author 智云
 */
public class NumberUtils {

    /**
     * 宽松解析 Long：非数字串返回 null，而不是抛 NumberFormatException。
     *
     * <p>调用方拿它解析审计列 {@code creator} 这类"通常是用户编号、但并非一定是"的字符串：
     * 平台自己就会写入非数字值（system_role.creator='admin'、demo 通道 creator='demo-seed'）。
     * 解析不出用户编号的语义就是"没有对应用户"，与空串走同一条降级路径
     * （convertSetByFlatMap / MapUtils.findAndThen 均已过滤 null）；
     * 若在这里抛异常，则一条审计列就能把整个列表接口打成 500。
     */
    public static Long parseLong(String str) {
        return StrUtil.isNotEmpty(str) && NumberUtil.isLong(str) ? Long.valueOf(str) : null;
    }

    public static Integer parseInt(String str) {
        return StrUtil.isNotEmpty(str) && NumberUtil.isInteger(str) ? Integer.valueOf(str) : null;
    }

    public static boolean isAllNumber(List<String> values) {
        if (CollUtil.isEmpty(values)) {
            return false;
        }
        for (String value : values) {
            if (!NumberUtil.isNumber(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 通过经纬度获取地球上两点之间的距离
     *
     * 参考 <<a href="https://gitee.com/dromara/hutool/blob/1caabb586b1f95aec66a21d039c5695df5e0f4c1/hutool-core/src/main/java/cn/hutool/core/util/DistanceUtil.java">DistanceUtil</a>> 实现，目前它已经被 hutool 删除
     *
     * @param lat1 经度1
     * @param lng1 纬度1
     * @param lat2 经度2
     * @param lng2 纬度2
     * @return 距离，单位：千米
     */
    public static double getDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = lat1 * Math.PI / 180.0;
        double radLat2 = lat2 * Math.PI / 180.0;
        double a = radLat1 - radLat2;
        double b = lng1 * Math.PI / 180.0 - lng2 * Math.PI / 180.0;
        double distance = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        distance = distance * 6378.137;
        distance = Math.round(distance * 10000d) / 10000d;
        return distance;
    }

    /**
     * 提供精确的乘法运算
     *
     * 和 hutool {@link NumberUtil#mul(BigDecimal...)} 的差别是，如果存在 null，则返回 null
     *
     * @param values 多个被乘值
     * @return 积
     */
    public static BigDecimal mul(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value == null) {
                return null;
            }
        }
        return NumberUtil.mul(values);
    }

}
