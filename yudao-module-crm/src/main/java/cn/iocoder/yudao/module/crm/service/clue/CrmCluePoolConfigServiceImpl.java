package cn.iocoder.yudao.module.crm.service.clue;

import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.crm.controller.admin.clue.vo.poolconfig.CrmCluePoolConfigSaveReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.clue.CrmCluePoolConfigDO;
import cn.iocoder.yudao.module.crm.dal.mysql.clue.CrmCluePoolConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Objects;

/**
 * 线索公海配置 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class CrmCluePoolConfigServiceImpl implements CrmCluePoolConfigService {

    @Resource
    private CrmCluePoolConfigMapper cluePoolConfigMapper;

    @Override
    public CrmCluePoolConfigDO getCluePoolConfig() {
        return cluePoolConfigMapper.selectOne();
    }

    @Override
    public void saveCluePoolConfig(CrmCluePoolConfigSaveReqVO saveReqVO) {
        // 1. 存在，则进行更新
        CrmCluePoolConfigDO dbConfig = getCluePoolConfig();
        CrmCluePoolConfigDO poolConfig = BeanUtils.toBean(saveReqVO, CrmCluePoolConfigDO.class);
        if (Objects.nonNull(dbConfig)) {
            cluePoolConfigMapper.updateById(poolConfig.setId(dbConfig.getId()));
            return;
        }

        // 2. 不存在，则进行插入
        cluePoolConfigMapper.insert(poolConfig);
    }

}
