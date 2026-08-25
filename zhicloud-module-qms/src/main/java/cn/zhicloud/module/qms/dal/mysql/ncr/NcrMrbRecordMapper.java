package cn.zhicloud.module.qms.dal.mysql.ncr;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.module.qms.dal.dataobject.ncr.NcrMrbRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS MRB 评审记录 Mapper
 *
 * @author 智云
 */
@Mapper
public interface NcrMrbRecordMapper extends BaseMapperX<NcrMrbRecordDO> {

    default List<NcrMrbRecordDO> selectListByNcrId(Long ncrId) {
        return selectList(NcrMrbRecordDO::getNcrId, ncrId);
    }

}
