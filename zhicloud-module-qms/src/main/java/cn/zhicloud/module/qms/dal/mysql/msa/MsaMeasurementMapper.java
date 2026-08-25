package cn.zhicloud.module.qms.dal.mysql.msa;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.qms.dal.dataobject.msa.MsaMeasurementDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS MSA 测量数据 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MsaMeasurementMapper extends BaseMapperX<MsaMeasurementDO> {

    default List<MsaMeasurementDO> selectListByStudyId(Long studyId) {
        return selectList(MsaMeasurementDO::getStudyId, studyId);
    }

}
