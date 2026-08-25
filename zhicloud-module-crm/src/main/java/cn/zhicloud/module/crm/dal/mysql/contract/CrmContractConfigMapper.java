package cn.zhicloud.module.crm.dal.mysql.contract;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.QueryWrapperX;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合同配置 Mapper
 *
 * @author Wanwan
 */
@Mapper
public interface CrmContractConfigMapper extends BaseMapperX<CrmContractConfigDO> {

    default CrmContractConfigDO selectOne() {
        return selectOne(new QueryWrapperX<CrmContractConfigDO>().limitN(1));
    }

}