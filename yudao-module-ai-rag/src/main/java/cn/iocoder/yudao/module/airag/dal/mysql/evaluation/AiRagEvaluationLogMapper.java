package cn.iocoder.yudao.module.airag.dal.mysql.evaluation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.airag.dal.dataobject.evaluation.AiRagEvaluationLogDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI RAG 评估日志 Mapper
 *
 * @author yudao
 */
@Mapper
public interface AiRagEvaluationLogMapper extends BaseMapperX<AiRagEvaluationLogDO> {
}
