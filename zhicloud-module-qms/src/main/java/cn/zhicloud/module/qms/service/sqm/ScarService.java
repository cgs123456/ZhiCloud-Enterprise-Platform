package cn.zhicloud.module.qms.service.sqm;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.ScarPageReqVO;
import cn.zhicloud.module.qms.controller.admin.sqm.vo.ScarSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.sqm.ScarDO;
import jakarta.validation.Valid;

/**
 * QMS SCAR（供应商纠正措施请求）Service 接口
 *
 * @author zhicloud
 */
public interface ScarService {

    Long createScar(@Valid ScarSaveReqVO createReqVO);

    void updateScar(@Valid ScarSaveReqVO updateReqVO);

    void deleteScar(Long id);

    ScarDO getScar(Long id);

    PageResult<ScarDO> getScarPage(ScarPageReqVO pageReqVO);

    /**
     * 关闭 SCAR
     *
     * @param id 编号
     */
    void closeScar(Long id);

}