package cn.zhicloud.module.erp.service.collaboration.cpfr;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrExceptionHandleReqVO;
import cn.zhicloud.module.erp.controller.admin.collaboration.cpfr.vo.ErpCpfrExceptionPageReqVO;
import cn.zhicloud.module.erp.dal.dataobject.collaboration.cpfr.ErpCpfrExceptionDO;
import cn.zhicloud.module.erp.dal.mysql.collaboration.cpfr.ErpCpfrExceptionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.CPFR_EXCEPTION_NOT_EXISTS;

/**
 * ERP CPFR 协同异常 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class ErpCpfrExceptionServiceImpl implements ErpCpfrExceptionService {

    @Resource
    private ErpCpfrExceptionMapper cpfrExceptionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteException(Long id) {
        validateExceptionExists(id);
        cpfrExceptionMapper.deleteById(id);
    }

    private void validateExceptionExists(Long id) {
        if (cpfrExceptionMapper.selectById(id) == null) {
            throw exception(CPFR_EXCEPTION_NOT_EXISTS);
        }
    }

    @Override
    public ErpCpfrExceptionDO getException(Long id) {
        return cpfrExceptionMapper.selectById(id);
    }

    @Override
    public PageResult<ErpCpfrExceptionDO> getExceptionPage(ErpCpfrExceptionPageReqVO pageReqVO) {
        return cpfrExceptionMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleException(ErpCpfrExceptionHandleReqVO handleReqVO) {
        validateExceptionExists(handleReqVO.getId());
        ErpCpfrExceptionDO updateObj = new ErpCpfrExceptionDO();
        updateObj.setId(handleReqVO.getId());
        updateObj.setHandlingStatus(handleReqVO.getHandlingStatus());
        updateObj.setHandlerUserId(handleReqVO.getHandlerUserId());
        updateObj.setHandlingTime(LocalDateTime.now());
        updateObj.setRemark(handleReqVO.getRemark());
        cpfrExceptionMapper.updateById(updateObj);
    }

}
