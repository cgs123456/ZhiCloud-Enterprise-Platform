package cn.iocoder.yudao.module.hr.dal.mysql.contract;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.hr.controller.admin.contract.vo.HrContractPageReqVO;
import cn.iocoder.yudao.module.hr.dal.dataobject.contract.HrContractDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface HrContractMapper extends BaseMapperX<HrContractDO> {

    default PageResult<HrContractDO> selectPage(HrContractPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<HrContractDO>()
                .eqIfPresent(HrContractDO::getEmployeeId, reqVO.getEmployeeId())
                .likeIfPresent(HrContractDO::getContractNo, reqVO.getContractNo())
                .eqIfPresent(HrContractDO::getContractType, reqVO.getContractType())
                .eqIfPresent(HrContractDO::getStatus, reqVO.getStatus())
                .orderByDesc(HrContractDO::getId));
    }

    default HrContractDO selectByContractNo(String contractNo) {
        return selectOne(HrContractDO::getContractNo, contractNo);
    }

    default List<HrContractDO> selectListByExpiring(LocalDate deadline) {
        LocalDate now = LocalDate.now();
        return selectList(new LambdaQueryWrapperX<HrContractDO>()
                .isNotNull(HrContractDO::getEndDate)
                .le(HrContractDO::getEndDate, deadline)
                .ge(HrContractDO::getEndDate, now)
                .eq(HrContractDO::getStatus, 0));
    }

}