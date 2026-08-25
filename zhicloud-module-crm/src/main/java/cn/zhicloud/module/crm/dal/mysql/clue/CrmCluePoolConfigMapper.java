package cn.zhicloud.module.crm.dal.mysql.clue;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.crm.dal.dataobject.clue.CrmCluePoolConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 线索公海配置 Mapper
 *
 * @author 智云
 */
@Mapper
public interface CrmCluePoolConfigMapper extends BaseMapperX<CrmCluePoolConfigDO> {

    default CrmCluePoolConfigDO selectOne() {
        return selectOne(new LambdaQueryWrapperX<CrmCluePoolConfigDO>().last("LIMIT 1"));
    }

}
