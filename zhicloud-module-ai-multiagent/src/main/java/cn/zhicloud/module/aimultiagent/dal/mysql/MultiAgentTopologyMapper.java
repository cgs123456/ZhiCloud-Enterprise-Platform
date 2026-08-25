package cn.zhicloud.module.aimultiagent.dal.mysql;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.aimultiagent.controller.admin.topology.vo.MultiAgentTopologyPageReqVO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentTopologyDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 多 Agent 拓扑配置 Mapper
 *
 * @author zhicloud
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
