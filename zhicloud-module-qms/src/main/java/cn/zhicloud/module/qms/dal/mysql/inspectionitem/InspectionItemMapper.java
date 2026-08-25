package cn.zhicloud.module.qms.dal.mysql.inspectionitem;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.inspectionitem.vo.InspectionItemPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.inspectionitem.InspectionItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 检验项目 Mapper
 *
 * @author 智云
 */
@Mapper
public interface InspectionItemMapper extends BaseMapperX<InspectionItemDO> {

    default PageResult<InspectionItemDO> selectPage(InspectionItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<InspectionItemDO>()
                .likeIfPresent(InspectionItemDO::getCode, reqVO.getCode())
                .likeIfPresent(InspectionItemDO::getName, reqVO.getName())
                .eqIfPresent(InspectionItemDO::getType, reqVO.getType())
                .eqIfPresent(InspectionItemDO::getMethod, reqVO.getMethod())
                .eqIfPresent(InspectionItemDO::getStatus, reqVO.getStatus())
                .orderByDesc(InspectionItemDO::getId));
    }

    default InspectionItemDO selectByCode(String code) {
        return selectOne(InspectionItemDO::getCode, code);
    }

}
