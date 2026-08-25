package cn.zhicloud.module.ai.dal.mysql.nl2sql;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.ai.dal.dataobject.nl2sql.AiNl2SqlQueryHistoryDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI NL2SQL 查询历史 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface AiNl2SqlQueryHistoryMapper extends BaseMapperX<AiNl2SqlQueryHistoryDO> {
}
