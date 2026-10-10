package cn.zhicloud.module.fms.service.config;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.fms.controller.admin.config.vo.financeparameter.FmsFinanceParameterRespVO;
import cn.zhicloud.module.fms.controller.admin.config.vo.financeparameter.FmsFinanceParameterSaveReqVO;
import cn.zhicloud.module.fms.dal.dataobject.config.FmsFinanceParameterDO;
import cn.zhicloud.module.fms.dal.mysql.config.FmsFinanceParameterMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * FMS 账套参数 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class FmsFinanceParameterServiceImpl implements FmsFinanceParameterService {

    @Resource
    private FmsFinanceParameterMapper financeParameterMapper;

    @Override
    public FmsFinanceParameterRespVO getFinanceParameter(Long accountSetId) {
        FmsFinanceParameterDO parameter = financeParameterMapper.selectByAccountSet(accountSetId);
        if (parameter == null) {
            return null;
        }
        return BeanUtils.toBean(parameter, FmsFinanceParameterRespVO.class);
    }

    @Override
    public void updateFinanceParameter(FmsFinanceParameterSaveReqVO reqVO) {
        FmsFinanceParameterDO exists = financeParameterMapper.selectByAccountSet(reqVO.getAccountSetId());
        FmsFinanceParameterDO entity = BeanUtils.toBean(reqVO, FmsFinanceParameterDO.class);
        if (exists == null) {
            financeParameterMapper.insert(entity);
        } else {
            entity.setId(exists.getId());
            financeParameterMapper.updateById(entity);
        }
    }

}
