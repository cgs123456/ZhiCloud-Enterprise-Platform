package cn.zhicloud.framework.integration.dal.mysql;

import cn.zhicloud.framework.integration.dal.dataobject.EventDlqReplayLogDO;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;

/**
 * 死信重放审计 Mapper（V105）
 *
 * @author 智云集成治理
 */
@Mapper
public interface EventDlqReplayLogMapper extends BaseMapperX<EventDlqReplayLogDO> {
}
