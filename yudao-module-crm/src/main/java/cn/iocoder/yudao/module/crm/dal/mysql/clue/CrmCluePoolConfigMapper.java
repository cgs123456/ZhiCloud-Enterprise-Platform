package cn.iocoder.yudao.module.crm.dal.mysql.clue;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.crm.dal.dataobject.clue.CrmCluePoolConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 线索公海配置 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface CrmCluePoolConfigMapper extends BaseMapperX<CrmCluePoolConfigDO> {

    default CrmCluePoolConfigDO selectOne() {
        return selectOne(new LambdaQueryWrapperX<CrmCluePoolConfigDO>().last("LIMIT 1"));
    }

}
