package cn.zhicloud.module.mes.dal.mysql.wm.miscissue;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.wm.miscissue.vo.MesWmMiscIssuePageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.wm.miscissue.MesWmMiscIssueDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 杂项出库单 Mapper
 */
@Mapper
public interface MesWmMiscIssueMapper extends BaseMapperX<MesWmMiscIssueDO> {

    default PageResult<MesWmMiscIssueDO> selectPage(MesWmMiscIssuePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmMiscIssueDO>()
                .likeIfPresent(MesWmMiscIssueDO::getCode, reqVO.getCode())
                .likeIfPresent(MesWmMiscIssueDO::getName, reqVO.getName())
                .eqIfPresent(MesWmMiscIssueDO::getType, reqVO.getType())
                .likeIfPresent(MesWmMiscIssueDO::getSourceDocCode, reqVO.getSourceDocCode())
                .eqIfPresent(MesWmMiscIssueDO::getSourceDocType, reqVO.getSourceDocType())
                .eqIfPresent(MesWmMiscIssueDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(MesWmMiscIssueDO::getIssueDate, reqVO.getIssueDate())
                .orderByDesc(MesWmMiscIssueDO::getId));
    }

    default MesWmMiscIssueDO selectByCode(String code) {
        return selectOne(MesWmMiscIssueDO::getCode, code);
    }

    default List<MesWmMiscIssueDO> selectListByStatus(Integer status) {
        return selectList(MesWmMiscIssueDO::getStatus, status);
    }

}
