package cn.zhicloud.module.crm.dal.mysql.contract;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractEsignTaskDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CRM 合同电子签任务 Mapper（ADR 271 D1）
 *
 * @author dhb52
 */
@Mapper
public interface CrmContractEsignTaskMapper extends BaseMapperX<CrmContractEsignTaskDO> {

    default CrmContractEsignTaskDO selectByTaskNo(String taskNo) {
        return selectOne(CrmContractEsignTaskDO::getTaskNo, taskNo);
    }

    default CrmContractEsignTaskDO selectLatestByContractId(Long contractId) {
        return selectOne(new LambdaQueryWrapperX<CrmContractEsignTaskDO>()
                .eq(CrmContractEsignTaskDO::getContractId, contractId)
                .orderByDesc(CrmContractEsignTaskDO::getId)
                .last("LIMIT 1"));
    }

}
