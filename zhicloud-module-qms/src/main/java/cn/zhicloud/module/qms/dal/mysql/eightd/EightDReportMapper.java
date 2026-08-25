package cn.zhicloud.module.qms.dal.mysql.eightd;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.eightd.vo.EightDReportPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.eightd.EightDReportDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * QMS 8D 报告 Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface EightDReportMapper extends BaseMapperX<EightDReportDO> {

    default PageResult<EightDReportDO> selectPage(EightDReportPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EightDReportDO>()
                .likeIfPresent(EightDReportDO::getReportNo, reqVO.getReportNo())
                .likeIfPresent(EightDReportDO::getTitle, reqVO.getTitle())
                .eqIfPresent(EightDReportDO::getStatus, reqVO.getStatus())
                .eqIfPresent(EightDReportDO::getNcrId, reqVO.getNcrId())
                .eqIfPresent(EightDReportDO::getCapaId, reqVO.getCapaId())
                .orderByDesc(EightDReportDO::getId));
    }

    default EightDReportDO selectByReportNo(String reportNo) {
        return selectOne(EightDReportDO::getReportNo, reportNo);
    }

}