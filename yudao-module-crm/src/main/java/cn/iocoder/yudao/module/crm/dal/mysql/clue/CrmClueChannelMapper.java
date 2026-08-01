package cn.iocoder.yudao.module.crm.dal.mysql.clue;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.crm.controller.admin.clue.vo.channel.CrmClueChannelPageReqVO;
import cn.iocoder.yudao.module.crm.service.clue.channel.CrmClueChannelDO;
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
