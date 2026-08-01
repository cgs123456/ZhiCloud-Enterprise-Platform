package cn.iocoder.yudao.module.qms.dal.mysql.fmea;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaItemPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.fmea.FmeaItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS FMEA 条目 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface FmeaItemMapper extends BaseMapperX<FmeaItemDO> {

    default PageResult<FmeaItemDO> selectPage(FmeaItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<FmeaItemDO>()
                .eqIfPresent(FmeaItemDO::getFmeaId, reqVO.getFmeaId())
                .orderByAsc(FmeaItemDO::getId));
    }

    default List<FmeaItemDO> selectListByFmeaId(Long fmeaId) {
        return selectList(FmeaItemDO::getFmeaId, fmeaId);
    }

}
