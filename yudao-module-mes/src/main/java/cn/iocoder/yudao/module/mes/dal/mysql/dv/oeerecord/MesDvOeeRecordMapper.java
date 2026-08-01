package cn.iocoder.yudao.module.mes.dal.mysql.dv.oeerecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.dv.oeerecord.vo.MesDvOeeRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.oeerecord.MesDvOeeRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MES OEE 记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesDvOeeRecordMapper extends BaseMapperX<MesDvOeeRecordDO> {

    default PageResult<MesDvOeeRecordDO> selectPage(MesDvOeeRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesDvOeeRecordDO>()
                .eqIfPresent(MesDvOeeRecordDO::getMachineryId, reqVO.getMachineryId())
                .betweenIfPresent(MesDvOeeRecordDO::getRecordDate, reqVO.getRecordDate())
                .orderByDesc(MesDvOeeRecordDO::getRecordDate));
    }

    default List<MesDvOeeRecordDO> selectListByMachineryAndDateRange(Long machineryId,
                                                                      LocalDateTime startDate,
                                                                      LocalDateTime endDate) {
        return selectList(new LambdaQueryWrapperX<MesDvOeeRecordDO>()
                .eqIfPresent(MesDvOeeRecordDO::getMachineryId, machineryId)
                .ge(MesDvOeeRecordDO::getRecordDate, startDate)
                .le(MesDvOeeRecordDO::getRecordDate, endDate)
                .orderByAsc(MesDvOeeRecordDO::getRecordDate));
    }

}
