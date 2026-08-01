package cn.iocoder.yudao.module.crm.service.clue;

import cn.iocoder.yudao.module.crm.controller.admin.clue.vo.poolconfig.CrmCluePoolConfigSaveReqVO;
import cn.iocoder.yudao.module.crm.dal.dataobject.clue.CrmCluePoolConfigDO;
import jakarta.validation.Valid;

/**
 * 线索公海配置 Service 接口
 *
 * @author 芋道源码
 */
public interface CrmCluePoolConfigService {

    /**
     * 获得线索公海配置
     *
     * @return 线索公海配置
     */
    CrmCluePoolConfigDO getCluePoolConfig();

    /**
     * 保存线索公海配置
     *
     * @param saveReqVO 更新信息
     */
    void saveCluePoolConfig(@Valid CrmCluePoolConfigSaveReqVO saveReqVO);

}
