package cn.zhicloud.module.erp.service.collaboration.cpfr;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrExceptionHandleReqVO;
import cn.zhicloud.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrExceptionPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.collaboration.cpfr.ErpCpfrExceptionDO;
import jakarta.validation.Valid;

/**
 * ERP CPFR 协同异常 Service 接口
 *
 * @author 智云
 */
public interface ErpCpfrExceptionService {

    /**
     * 删除异常
     *
     * @param id 编号
     */
    void deleteException(Long id);

    /**
     * 获得异常
     *
     * @param id 编号
     * @return 异常
     */
    ErpCpfrExceptionDO getException(Long id);

    /**
     * 获得异常分页
     *
     * @param pageReqVO 分页查询
     * @return 异常分页
     */
    PageResult<ErpCpfrExceptionDO> getExceptionPage(ErpCpfrExceptionPageReqVO pageReqVO);

    /**
     * 处理异常：更新处理状态、处理人、处理时间
     *
     * @param handleReqVO 处理信息
     */
    void handleException(@Valid ErpCpfrExceptionHandleReqVO handleReqVO);

}
