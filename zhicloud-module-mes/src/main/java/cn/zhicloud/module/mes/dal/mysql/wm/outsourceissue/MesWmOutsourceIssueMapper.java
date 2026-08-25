package cn.zhicloud.module.mes.dal.mysql.wm.outsourceissue;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.mes.controller.admin.wm.outsourceissue.vo.MesWmOutsourceIssuePageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.wm.outsourceissue.MesWmOutsourceIssueDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * MES 外协发料单 Mapper
 *
 * @author 智云
 */
@Mapper
public interface MesWmOutsourceIssueMapper extends BaseMapperX<MesWmOutsourceIssueDO> {

    default PageResult<MesWmOutsourceIssueDO> selectPage(MesWmOutsourceIssuePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesWmOutsourceIssueDO>()
                .likeIfPresent(MesWmOutsourceIssueDO::getCode, reqVO.getCode())
                .likeIfPresent(MesWmOutsourceIssueDO::getName, reqVO.getName())
                .eqIfPresent(MesWmOutsourceIssueDO::getVendorId, reqVO.getVendorId())
                .eqIfPresent(MesWmOutsourceIssueDO::getWorkOrderId, reqVO.getWorkOrderId())
                .betweenIfPresent(MesWmOutsourceIssueDO::getIssueDate, reqVO.getIssueDate())
                .orderByDesc(MesWmOutsourceIssueDO::getId));
    }

    default MesWmOutsourceIssueDO selectByCode(String code) {
        return selectOne(MesWmOutsourceIssueDO::getCode, code);
    }

    default Long selectCountByVendorId(Long vendorId) {
        return selectCount(MesWmOutsourceIssueDO::getVendorId, vendorId);
    }

}
