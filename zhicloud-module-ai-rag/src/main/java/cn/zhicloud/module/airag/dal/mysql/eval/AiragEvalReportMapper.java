package cn.zhicloud.module.airag.dal.mysql.eval;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.airag.dal.dataobject.eval.AiragEvalReportDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * RAG 批量评估报告 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface AiragEvalReportMapper extends BaseMapperX<AiragEvalReportDO> {

    /**
     * 按数据集查询报告历史（按创建时间倒序，趋势看板用）
     *
     * @param datasetId 数据集编号
     * @param limit     返回条数
     * @return 报告列表
     */
    default List<AiragEvalReportDO> selectListByDatasetIdDesc(Long datasetId, int limit) {
        return selectList(new LambdaQueryWrapperX<AiragEvalReportDO>()
                .eq(AiragEvalReportDO::getDatasetId, datasetId)
                .orderByDesc(AiragEvalReportDO::getCreateTime)
                .last("LIMIT " + limit));
    }

    /**
     * 查询数据集最新一次已完成的报告（退步对比基线用）
     *
     * @param datasetId 数据集编号
     * @return 最新已完成报告，无则返回 null
     */
    default AiragEvalReportDO selectLatestDoneByDatasetId(Long datasetId) {
        return selectOne(new LambdaQueryWrapperX<AiragEvalReportDO>()
                .eq(AiragEvalReportDO::getDatasetId, datasetId)
                .eq(AiragEvalReportDO::getStatus, 1)
                .orderByDesc(AiragEvalReportDO::getCreateTime)
                .last("LIMIT 1"));
    }

}
