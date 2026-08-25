package cn.zhicloud.module.qms.dal.mysql.fmea;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.fmea.vo.FmeaItemPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.fmea.FmeaItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * QMS FMEA 条目 Mapper
 *
 * @author 智云
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
