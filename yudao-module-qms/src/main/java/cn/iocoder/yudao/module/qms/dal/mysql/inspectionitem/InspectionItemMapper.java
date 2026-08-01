package cn.iocoder.yudao.module.qms.dal.mysql.inspectionitem;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.inspectionitem.vo.InspectionItemPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionitem.InspectionItemDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 检验项目 Mapper
 *
 * @author 芋道源码
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
