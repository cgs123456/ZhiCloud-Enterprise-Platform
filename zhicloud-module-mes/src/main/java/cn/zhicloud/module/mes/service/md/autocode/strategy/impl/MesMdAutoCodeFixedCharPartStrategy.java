package cn.zhicloud.module.mes.service.md.autocode.strategy.impl;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.module.mes.dal.dataobject.md.autocode.MesMdAutoCodePartDO;
import cn.zhicloud.module.mes.enums.md.autocode.MesMdAutoCodePartTypeEnum;
import cn.zhicloud.module.mes.service.md.autocode.strategy.MesMdAutoCodeContext;
import cn.zhicloud.module.mes.service.md.autocode.strategy.MesMdAutoCodePartStrategy;
import org.springframework.stereotype.Component;

/**
 * MES 编码规则 - 固定字符策略
 *
 * @author 智云
 */
@Component
public class MesMdAutoCodeFixedCharPartStrategy implements MesMdAutoCodePartStrategy {

    @Override
    public Integer getType() {
        return MesMdAutoCodePartTypeEnum.FIXED_CHAR.getType();
    }

    @Override
    public String generate(MesMdAutoCodePartDO part, MesMdAutoCodeContext context) {
        return StrUtil.emptyToDefault(part.getFixCharacter(), "");
    }

}
