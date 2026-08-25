package cn.zhicloud.module.airag.dal.mysql.evaluation;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.airag.dal.dataobject.evaluation.AiRagEvaluationLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI RAG 评估日志 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface AiRagEvaluationLogMapper extends BaseMapperX<AiRagEvaluationLogDO> {
}
