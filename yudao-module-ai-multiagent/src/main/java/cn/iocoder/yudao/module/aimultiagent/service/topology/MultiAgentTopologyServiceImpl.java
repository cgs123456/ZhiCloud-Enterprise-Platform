package cn.iocoder.yudao.module.aimultiagent.service.topology;

import cn.hutool.core.util.ObjUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologyPageReqVO;
import cn.iocoder.yudao.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologySaveReqVO;
import cn.iocoder.yudao.module.aimultiagent.dal.dataobject.MultiAgentTopologyDO;
import cn.iocoder.yudao.module.aimultiagent.dal.mysql.MultiAgentTopologyMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.aimultiagent.enums.ErrorCodeConstants.TOPOLOGY_NOT_EXISTS;

/**
 * 多 Agent 拓扑配置 Service 实现类
 *
 * @author yudao
 */
@Service
@Slf4j
public class MultiAgentTopologyServiceImpl implements MultiAgentTopologyService {

    @Resource
    private MultiAgentTopologyMapper topologyMapper;

    @Override
    public Long createTopology(MultiAgentTopologySaveReqVO createReqVO) {
        MultiAgentTopologyDO topology = BeanUtils.toBean(createReqVO, MultiAgentTopologyDO.class);
        topologyMapper.insert(topology);
        return topology.getId();
    }

    @Override
    public void updateTopology(MultiAgentTopologySaveReqVO updateReqVO) {
        // 1. 校验存在
        validateTopologyExists(updateReqVO.getId());
        // 2. 更新
        MultiAgentTopologyDO topology = BeanUtils.toBean(updateReqVO, MultiAgentTopologyDO.class);
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

}
