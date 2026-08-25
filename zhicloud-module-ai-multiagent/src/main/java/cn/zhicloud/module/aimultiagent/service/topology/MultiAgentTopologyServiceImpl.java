package cn.zhicloud.module.aimultiagent.service.topology;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologyPageReqVO;
import cn.zhicloud.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologySaveReqVO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentTopologyDO;
import cn.zhicloud.module.aimultiagent.dal.mysql.MultiAgentTopologyMapper;
import cn.zhicloud.module.aimultiagent.model.AgentTopology;
import cn.zhicloud.module.aimultiagent.service.agent.WorkerAgentRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.aimultiagent.enums.ErrorCodeConstants.TOPOLOGY_NOT_EXISTS;
import static cn.zhicloud.module.aimultiagent.enums.ErrorCodeConstants.TOPOLOGY_WORKER_CONFIG_INVALID;
import static cn.zhicloud.module.aimultiagent.enums.ErrorCodeConstants.TOPOLOGY_WORKER_NOT_REGISTERED;

/**
 * 多 Agent 拓扑配置 Service 实现类
 *
 * @author zhicloud
 */
@Service
@Slf4j
public class MultiAgentTopologyServiceImpl implements MultiAgentTopologyService {

    @Resource
    private MultiAgentTopologyMapper topologyMapper;

    @Resource
    private WorkerAgentRegistry workerAgentRegistry;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public Long createTopology(MultiAgentTopologySaveReqVO createReqVO) {
        // 0. 校验 Worker 配置（含引用注册中心的存在性），失败即报错，避免运行时才暴露
        validateWorkerConfig(createReqVO.getWorkerConfig());
        MultiAgentTopologyDO topology = BeanUtils.toBean(createReqVO, MultiAgentTopologyDO.class);
        // 配置 schema 版本默认 v1
        topology.setVersion(StrUtil.blankToDefault(topology.getVersion(), "v1"));
        topologyMapper.insert(topology);
        return topology.getId();
    }

    @Override
    public void updateTopology(MultiAgentTopologySaveReqVO updateReqVO) {
        // 1. 校验存在
        validateTopologyExists(updateReqVO.getId());
        // 2. 校验 Worker 配置（含引用注册中心的存在性）
        validateWorkerConfig(updateReqVO.getWorkerConfig());
        // 3. 更新
        MultiAgentTopologyDO topology = BeanUtils.toBean(updateReqVO, MultiAgentTopologyDO.class);
        topology.setVersion(StrUtil.blankToDefault(topology.getVersion(), "v1"));
        topologyMapper.updateById(topology);
    }

    @Override
    public void deleteTopology(Long id) {
        // 1. 校验存在
        validateTopologyExists(id);
        // 2. 删除
        topologyMapper.deleteById(id);
    }

    @Override
    public MultiAgentTopologyDO getTopology(Long id) {
        return topologyMapper.selectById(id);
    }

    @Override
    public PageResult<MultiAgentTopologyDO> getTopologyPage(MultiAgentTopologyPageReqVO pageReqVO) {
        return topologyMapper.selectPage(pageReqVO);
    }

    private MultiAgentTopologyDO validateTopologyExists(Long id) {
        if (ObjUtil.isNull(id)) {
            throw exception(TOPOLOGY_NOT_EXISTS);
        }
        MultiAgentTopologyDO topology = topologyMapper.selectById(id);
        if (ObjUtil.isNull(topology)) {
            throw exception(TOPOLOGY_NOT_EXISTS);
        }
        return topology;
    }

    /**
     * 校验 Worker 配置 JSON：解析结构、每项 name 非空、且引用的 Worker 在注册中心存在。
     * 允许为空（表示空 workers），仅做非空时的结构与引用校验，使配置错误在保存时即暴露，而非运行时。
     */
    private void validateWorkerConfig(String workerConfigJson) {
        if (StrUtil.isBlank(workerConfigJson)) {
            return;
        }
        List<AgentTopology.WorkerConfig> workers;
        try {
            workers = OBJECT_MAPPER.readValue(workerConfigJson,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, AgentTopology.WorkerConfig.class));
        } catch (Exception e) {
            throw exception(TOPOLOGY_WORKER_CONFIG_INVALID);
        }
        if (CollUtil.isEmpty(workers)) {
            return;
        }
        for (AgentTopology.WorkerConfig worker : workers) {
            if (StrUtil.isBlank(worker.getName())) {
                throw exception(TOPOLOGY_WORKER_CONFIG_INVALID);
            }
            if (workerAgentRegistry.getWorker(worker.getName()) == null) {
                throw exception(TOPOLOGY_WORKER_NOT_REGISTERED, worker.getName());
            }
        }
    }

}
