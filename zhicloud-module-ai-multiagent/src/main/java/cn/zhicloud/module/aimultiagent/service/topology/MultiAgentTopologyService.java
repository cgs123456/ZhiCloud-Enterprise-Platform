package cn.zhicloud.module.aimultiagent.service.topology;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologyPageReqVO;
import cn.zhicloud.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologySaveReqVO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentTopologyDO;

/**
 * 多 Agent 拓扑配置 Service 接口
 *
 * @author zhicloud
 */
public interface MultiAgentTopologyService {

    /**
     * 创建拓扑配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTopology(MultiAgentTopologySaveReqVO createReqVO);

    /**
     * 更新拓扑配置
     *
     * @param updateReqVO 更新信息
     */
    void updateTopology(MultiAgentTopologySaveReqVO updateReqVO);

    /**
     * 删除拓扑配置
     *
     * @param id 编号
     */
    void deleteTopology(Long id);

    /**
     * 获取拓扑配置详情
     *
     * @param id 编号
     * @return 拓扑配置
     */
    MultiAgentTopologyDO getTopology(Long id);

    /**
     * 分页查询拓扑配置
     *
     * @param pageReqVO 分页查询
     * @return 拓扑配置分页
     */
    PageResult<MultiAgentTopologyDO> getTopologyPage(MultiAgentTopologyPageReqVO pageReqVO);

}
