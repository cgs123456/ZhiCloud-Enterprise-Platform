package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.erp.api.ErpAccountBookApi;
import cn.zhicloud.module.erp.controller.admin.finance.vo.accountbook.ErpAccountBookRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpAccountBookMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link ErpAccountBookApi} 的 ERP 侧实现
 *
 * <p>V191（台账 §12.212）：FMS/ERP 财务融合后，账簿挂到 FMS 账套下，
 * 账套详情页需要反向查询「本账套挂了哪些账簿」。
 *
 * @author 智云
 */
@Component
public class ErpAccountBookApiImpl implements ErpAccountBookApi {

    @Resource
    private ErpAccountBookMapper accountBookMapper;

    @Override
    public List<ErpAccountBookRespVO> getAccountBookListByAccountSetId(Long accountSetId) {
        if (accountSetId == null) {
            return List.of();
        }
        List<ErpAccountBookDO> list = accountBookMapper.selectListByAccountSetId(accountSetId);
        return BeanUtils.toBean(list, ErpAccountBookRespVO.class);
    }

}
