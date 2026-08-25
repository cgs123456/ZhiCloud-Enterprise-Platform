package cn.zhicloud.module.mes.service.qc.indicatorresult;

import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.dal.dataobject.qc.indicatorresult.MesQcIndicatorResultDetailDO;
import cn.zhicloud.module.mes.dal.mysql.qc.indicatorresult.MesQcIndicatorResultDetailMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * MES 检验结果明细 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class MesQcIndicatorResultDetailServiceImpl implements MesQcIndicatorResultDetailService {

    @Resource
    private MesQcIndicatorResultDetailMapper detailMapper;

    @Override
    public void createDetailList(List<MesQcIndicatorResultDetailDO> details) {
        detailMapper.insertBatch(details);
    }

    @Override
    public void createOrUpdateDetailList(List<MesQcIndicatorResultDetailDO> details) {
        detailMapper.insertOrUpdate(details);
    }

    @Override
    public List<MesQcIndicatorResultDetailDO> getDetailListByResultId(Long resultId) {
        return detailMapper.selectListByResultId(resultId);
    }

    @Override
    public void deleteDetailByResultId(Long resultId) {
        detailMapper.delete(new LambdaQueryWrapperX<MesQcIndicatorResultDetailDO>()
                .eq(MesQcIndicatorResultDetailDO::getResultId, resultId));
    }

}
