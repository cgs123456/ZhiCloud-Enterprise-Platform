package cn.zhicloud.module.erp.service.finance.cost.bom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * BOM 数据提供者默认空实现
 *
 * <p>当容器中不存在其他 {@link ErpBomProvider} 实现时启用，返回空 BOM 列表。
 * 此时成本卷积退化为"仅汇总本层标准成本"，与改造前行为保持一致。
 *
 * <p>后续接入 MES BOM 或独立 ERP BOM 模块时，只需提供 {@link ErpBomProvider} 的
 * 实现 Bean，本默认实现会自动退出（{@link ConditionalOnMissingBean}）。
 *
 * @author 智云
 */
@Component
@ConditionalOnMissingBean(ErpBomProvider.class)
public class NoOpErpBomProvider implements ErpBomProvider {

    @Override
    public List<ErpBomComponent> getBomComponents(Long productId) {
        return Collections.emptyList();
    }

}
