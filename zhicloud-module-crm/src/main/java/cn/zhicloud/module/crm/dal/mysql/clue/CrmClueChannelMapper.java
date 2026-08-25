package cn.zhicloud.module.crm.dal.mysql.clue;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.crm.controller.admin.clue.vo.channel.CrmClueChannelPageReqVO;
import cn.zhicloud.module.crm.service.clue.channel.CrmClueChannelDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CRM 线索渠道 Mapper
 *
 * @author dhb52
 */
@Mapper
public interface CrmClueChannelMapper extends BaseMapperX<CrmClueChannelDO> {

    default CrmClueChannelDO selectByChannelCode(String channelCode) {
        return selectOne(CrmClueChannelDO::getChannelCode, channelCode);
    }

    default PageResult<CrmClueChannelDO> selectPage(CrmClueChannelPageReqVO pageReqVO) {
        return selectPage(pageReqVO, new LambdaQueryWrapperX<CrmClueChannelDO>()
                .likeIfPresent(CrmClueChannelDO::getChannelName, pageReqVO.getChannelName())
                .eqIfPresent(CrmClueChannelDO::getChannelType, pageReqVO.getChannelType())
                .likeIfPresent(CrmClueChannelDO::getChannelCode, pageReqVO.getChannelCode())
                .eqIfPresent(CrmClueChannelDO::getStatus, pageReqVO.getStatus())
                .orderByDesc(CrmClueChannelDO::getId));
    }

}
