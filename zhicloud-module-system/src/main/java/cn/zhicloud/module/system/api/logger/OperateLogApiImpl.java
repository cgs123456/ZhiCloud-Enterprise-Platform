package cn.zhicloud.module.system.api.logger;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.zhicloud.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.zhicloud.module.system.api.logger.dto.OperateLogRespDTO;
import cn.zhicloud.module.system.dal.dataobject.logger.OperateLogDO;
import cn.zhicloud.module.system.service.logger.OperateLogService;
import org.dromara.core.trans.anno.TransMethodResult;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 操作日志 API 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class OperateLogApiImpl implements OperateLogApi {

    @Resource
    private OperateLogService operateLogService;

    @Override
    public void createOperateLog(OperateLogCreateReqDTO createReqDTO) {
        operateLogService.createOperateLog(createReqDTO);
    }

    @Override
    @TransMethodResult
    public PageResult<OperateLogRespDTO> getOperateLogPage(OperateLogPageReqDTO pageReqDTO) {
        PageResult<OperateLogDO> operateLogPage = operateLogService.getOperateLogPage(pageReqDTO);
        return BeanUtils.toBean(operateLogPage, OperateLogRespDTO.class);
    }

}
