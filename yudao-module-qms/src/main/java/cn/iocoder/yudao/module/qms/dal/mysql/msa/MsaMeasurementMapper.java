package cn.iocoder.yudao.module.qms.dal.mysql.msa;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.qms.dal.dataobject.msa.MsaMeasurementDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS MSA 测量数据 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MsaMeasurementMapper extends BaseMapperX<MsaMeasurementDO> {

    default List<MsaMeasurementDO> selectListByStudyId(Long studyId) {
        return selectList(MsaMeasurementDO::getStudyId, studyId);
    }

}
