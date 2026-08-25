package cn.zhicloud.module.crm.service.clue.channel;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.crm.controller.admin.clue.vo.channel.CrmClueChannelPageReqVO;
import cn.zhicloud.module.crm.controller.admin.clue.vo.channel.CrmClueChannelSaveReqVO;
import cn.zhicloud.module.crm.controller.admin.clue.vo.channel.CrmClueReceiveReqVO;
import jakarta.validation.Valid;

/**
 * CRM 线索渠道 Service 接口
 *
 * @author dhb52
 */
public interface CrmClueChannelService {

    /**
     * 创建线索渠道
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createClueChannel(@Valid CrmClueChannelSaveReqVO createReqVO);

    /**
     * 更新线索渠道
     *
     * @param updateReqVO 更新信息
     */
    void updateClueChannel(@Valid CrmClueChannelSaveReqVO updateReqVO);

    /**
     * 删除线索渠道
     *
     * @param id 编号
     */
    void deleteClueChannel(Long id);

    /**
     * 获得线索渠道
     *
     * @param id 编号
     * @return 线索渠道
     */
    CrmClueChannelDO getClueChannel(Long id);

    /**
     * 根据渠道编码获得线索渠道
     *
     * @param channelCode 渠道编码
     * @return 线索渠道
     */
    CrmClueChannelDO getClueChannelByCode(String channelCode);

    /**
     * 获得线索渠道分页
     *
     * @param pageReqVO 分页查询
     * @return 线索渠道分页
     */
    PageResult<CrmClueChannelDO> getClueChannelPage(CrmClueChannelPageReqVO pageReqVO);

    /**
     * 接收外部线索
     *
     * @param reqVO 外部线索数据
     * @return 线索编号
     */
    Long receiveClue(@Valid CrmClueReceiveReqVO reqVO);

}
