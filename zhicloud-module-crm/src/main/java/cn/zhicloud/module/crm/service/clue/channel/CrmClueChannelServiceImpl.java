package cn.zhicloud.module.crm.service.clue.channel;

import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.crm.controller.admin.clue.vo.CrmClueSaveReqVO;
import cn.zhicloud.module.crm.controller.admin.clue.vo.channel.CrmClueChannelPageReqVO;
import cn.zhicloud.module.crm.controller.admin.clue.vo.channel.CrmClueChannelSaveReqVO;
import cn.zhicloud.module.crm.controller.admin.clue.vo.channel.CrmClueReceiveReqVO;
import cn.zhicloud.module.crm.dal.mysql.clue.CrmClueChannelMapper;
import cn.zhicloud.module.crm.enums.clue.CrmClueChannelStatusEnum;
import cn.zhicloud.module.crm.service.clue.CrmClueService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CLUE_CHANNEL_CODE_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CLUE_CHANNEL_DISABLED;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CLUE_CHANNEL_NOT_EXISTS;
import static cn.zhicloud.module.crm.enums.ErrorCodeConstants.CLUE_CHANNEL_NOT_EXISTS_BY_CODE;

/**
 * CRM 线索渠道 Service 实现类
 *
 * @author dhb52
 */
@Service
@Validated
@Slf4j
public class CrmClueChannelServiceImpl implements CrmClueChannelService {

    @Resource
    private CrmClueChannelMapper clueChannelMapper;

    @Resource
    @Lazy
    private CrmClueService clueService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createClueChannel(CrmClueChannelSaveReqVO createReqVO) {
        // 1. 校验渠道编码唯一
        if (clueChannelMapper.selectByChannelCode(createReqVO.getChannelCode()) != null) {
            throw exception(CLUE_CHANNEL_CODE_EXISTS);
        }
        // 2. 插入
        CrmClueChannelDO channel = BeanUtils.toBean(createReqVO, CrmClueChannelDO.class);
        clueChannelMapper.insert(channel);
        return channel.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateClueChannel(CrmClueChannelSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateClueChannelExists(updateReqVO.getId());
        // 2. 校验渠道编码唯一
        CrmClueChannelDO existing = clueChannelMapper.selectByChannelCode(updateReqVO.getChannelCode());
        if (existing != null && ObjUtil.notEqual(existing.getId(), updateReqVO.getId())) {
            throw exception(CLUE_CHANNEL_CODE_EXISTS);
        }
        // 3. 更新
        CrmClueChannelDO updateObj = BeanUtils.toBean(updateReqVO, CrmClueChannelDO.class);
        clueChannelMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteClueChannel(Long id) {
        // 1. 校验存在
        validateClueChannelExists(id);
        // 2. 删除
        clueChannelMapper.deleteById(id);
    }

    private CrmClueChannelDO validateClueChannelExists(Long id) {
        CrmClueChannelDO channel = clueChannelMapper.selectById(id);
        if (channel == null) {
            throw exception(CLUE_CHANNEL_NOT_EXISTS);
        }
        return channel;
    }

    @Override
    public CrmClueChannelDO getClueChannel(Long id) {
        return clueChannelMapper.selectById(id);
    }

    @Override
    public CrmClueChannelDO getClueChannelByCode(String channelCode) {
        return clueChannelMapper.selectByChannelCode(channelCode);
    }

    @Override
    public PageResult<CrmClueChannelDO> getClueChannelPage(CrmClueChannelPageReqVO pageReqVO) {
        return clueChannelMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long receiveClue(CrmClueReceiveReqVO reqVO) {
        // 1. 根据渠道编码查找渠道
        CrmClueChannelDO channel = clueChannelMapper.selectByChannelCode(reqVO.getChannelCode());
        if (channel == null) {
            throw exception(CLUE_CHANNEL_NOT_EXISTS_BY_CODE, reqVO.getChannelCode());
        }
        // 2. 校验渠道状态是否启用
        if (ObjUtil.notEqual(channel.getStatus(), CrmClueChannelStatusEnum.ENABLE.getStatus())) {
            throw exception(CLUE_CHANNEL_DISABLED, channel.getChannelName());
        }
        // 3. 创建线索，自动分配给 autoAssignUserId
        CrmClueSaveReqVO clueSaveReqVO = new CrmClueSaveReqVO();
        // 线索名称：优先使用公司名称，否则使用联系人姓名
        clueSaveReqVO.setName(reqVO.getCompanyName() != null ? reqVO.getCompanyName() : reqVO.getContactName());
        clueSaveReqVO.setMobile(reqVO.getContactPhone());
        clueSaveReqVO.setRemark(buildClueRemark(reqVO, channel));
        clueSaveReqVO.setOwnerUserId(channel.getAutoAssignUserId());
        // 4. 调用线索 Service 创建线索
        return clueService.createClue(clueSaveReqVO);
    }

    /**
     * 构建线索备注：包含来源、渠道、公司等信息
     */
    private String buildClueRemark(CrmClueReceiveReqVO reqVO, CrmClueChannelDO channel) {
        StringBuilder sb = new StringBuilder();
        sb.append("渠道：").append(channel.getChannelName());
        if (reqVO.getSourceName() != null) {
            sb.append("，来源：").append(reqVO.getSourceName());
        }
        if (reqVO.getCompanyName() != null) {
            sb.append("，公司：").append(reqVO.getCompanyName());
        }
        if (reqVO.getRemark() != null) {
            sb.append("，备注：").append(reqVO.getRemark());
        }
        return sb.toString();
    }

}
