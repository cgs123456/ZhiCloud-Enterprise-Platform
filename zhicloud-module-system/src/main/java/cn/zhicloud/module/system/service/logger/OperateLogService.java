package cn.zhicloud.module.system.service.logger;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.biz.system.logger.dto.OperateLogCreateReqDTO;
import cn.zhicloud.module.system.api.logger.dto.OperateLogPageReqDTO;
import cn.zhicloud.module.system.controller.admin.logger.vo.operatelog.OperateLogHashVerifyRespVO;
import cn.zhicloud.module.system.controller.admin.logger.vo.operatelog.OperateLogPageReqVO;
import cn.zhicloud.module.system.dal.dataobject.logger.OperateLogDO;

/**
 * 操作日志 Service 接口
 *
 * @author 智云
 */
public interface OperateLogService {

    /**
     * 记录操作日志
     *
     * @param createReqDTO 创建请求
     */
    void createOperateLog(OperateLogCreateReqDTO createReqDTO);

    /**
     * 获得操作日志
     *
     * @param id 编号
     * @return 操作日志
     */
    OperateLogDO getOperateLog(Long id);

    /**
     * 获得操作日志分页列表
     *
     * @param pageReqVO 分页条件
     * @return 操作日志分页列表
     */
    PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqVO pageReqVO);

    /**
     * 获得操作日志分页列表
     *
     * @param pageReqVO 分页条件
     * @return 操作日志分页列表
     */
    PageResult<OperateLogDO> getOperateLogPage(OperateLogPageReqDTO pageReqVO);

    /**
     * 验证 Hash 链完整性
     *
     * 从指定 ID 开始，按 ID 升序重新计算每条日志的 hash，并验证：
     * 1. 单条日志的 current_hash 与重新计算的 hash 一致
     * 2. 相邻日志的 prev_hash 等于前一条的 current_hash
     *
     * @param startId 起始日志 ID（包含）。若为 null，从第一条日志开始
     * @param limit   本次验证的最大日志条数（避免一次性加载过多）
     * @return 验证结果
     */
    OperateLogHashVerifyRespVO verifyHashChain(Long startId, Integer limit);

}
