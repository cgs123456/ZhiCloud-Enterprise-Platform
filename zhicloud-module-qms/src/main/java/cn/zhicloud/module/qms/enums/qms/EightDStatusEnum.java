package cn.zhicloud.module.qms.enums.qms;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 8D 报告状态枚举
 *
 * <p>对应 8D（Eight Disciplines）问题解决法的八个阶段：
 * <ul>
 *   <li>D1 成立团队</li>
 *   <li>D2 描述问题</li>
 *   <li>D3 临时遏制措施</li>
 *   <li>D4 根本原因分析</li>
 *   <li>D5 制定永久纠正措施</li>
 *   <li>D6 实施并验证</li>
 *   <li>D7 预防再发生</li>
 *   <li>D8 团队表彰</li>
 * </ul>
 *
 * @author zhicloud
 */
@RequiredArgsConstructor
@Getter
public enum EightDStatusEnum implements ArrayValuable<Integer> {

    DRAFT(0, "草稿"),
    D1_TEAM(10, "D1 成立团队"),
    D2_PROBLEM(20, "D2 描述问题"),
    D3_INTERIM(30, "D3 临时遏制措施"),
    D4_ROOT_CAUSE(40, "D4 根本原因分析"),
    D5_PERMANENT(50, "D5 制定永久纠正措施"),
    D6_IMPLEMENT(60, "D6 实施并验证"),
    D7_PREVENT(70, "D7 预防再发生"),
    D8_CLOSED(80, "D8 团队表彰/关闭"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(EightDStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}