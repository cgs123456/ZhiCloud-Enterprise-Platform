package cn.zhicloud.module.mes.dal.mysql.wm.productproduce;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.wm.productproduce.vo.MesWmProductProduceLinePageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.wm.productproduce.MesWmProductProduceLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 生产入库单行 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesWmProductProduceLineMapper extends BaseMapperX<MesWmProductProduceLineDO> {

    default PageResult<MesWmProductProduceLineDO> selectPage(MesWmProductProduceLinePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmProductProduceLineDO>()
                .eqIfPresent(MesWmProductProduceLineDO::getFeedbackId, reqVO.getFeedbackId())
                .orderByDesc(MesWmProductProduceLineDO::getId));
    }

    default List<MesWmProductProduceLineDO> selectListByProduceId(Long produceId) {
        return selectList(MesWmProductProduceLineDO::getProduceId, produceId);
    }

    default List<MesWmProductProduceLineDO> selectListByFeedbackId(Long feedbackId) {
        return selectList(MesWmProductProduceLineDO::getFeedbackId, feedbackId);
    }

}
