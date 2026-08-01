package cn.iocoder.yudao.module.aimultiagent.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologyPageReqVO;
import cn.iocoder.yudao.module.aimultiagent.dal.dataobject.MultiAgentTopologyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 多 Agent 拓扑配置 Mapper
 *
 * @author yudao
 */
@Mapper
public interface MultiAgentTopologyMapper extends BaseMapperX<MultiAgentTopologyDO> {

    default PageResult<MultiAgentTopologyDO> selectPage(MultiAgentTopologyPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<MultiAgentTopologyDO>()
                .likeIfPresent(MultiAgentTopologyDO::getName, pageReqVO.getName())
                .eqIfPresent(MultiAgentTopologyDO::getStatus, pageReqVO.getStatus())
                .betweenIfPresent(MultiAgentTopologyDO::getCreateTime, pageReqVO.getCreateTime()));
    }

}
