package cn.iocoder.yudao.module.mes.service.dv.scada;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.scada.MesDvScadaConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.scada.MesDvScadaConfigMapper;
import cn.iocoder.yudao.module.mes.enums.dv.MesDvScadaProtocolTypeEnum;
import cn.iocoder.yudao.module.mes.service.dv.machinery.MesDvMachineryService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES SCADA 设备配置 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MesDvScadaConfigServiceImpl implements MesDvScadaConfigService {

    @Resource
    private MesDvScadaConfigMapper scadaConfigMapper;

    @Resource
    @Lazy
    private MesDvMachineryService machineryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createScadaConfig(MesDvScadaConfigSaveReqVO createReqVO) {
        // 1. 校验设备存在
        machineryService.getMachinery(createReqVO.getMachineryId());
        // 2. 校验协议类型
        validateProtocolType(createReqVO.getProtocolType());
        // 3. 校验设备唯一（一台设备只能有一份 SCADA 配置）
        if (scadaConfigMapper.selectByMachineryId(createReqVO.getMachineryId()) != null) {
            throw exception(DV_SCADA_CONFIG_MACHINERY_DUPLICATE);
        }
        // 4. 插入
        MesDvScadaConfigDO config = BeanUtils.toBean(createReqVO, MesDvScadaConfigDO.class);
        scadaConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateScadaConfig(MesDvScadaConfigSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateScadaConfigExists(updateReqVO.getId());
        // 2. 校验设备存在
        machineryService.getMachinery(updateReqVO.getMachineryId());
        // 3. 校验协议类型
        validateProtocolType(updateReqVO.getProtocolType());
        // 4. 校验设备唯一（排除自身）
        MesDvScadaConfigDO existed = scadaConfigMapper.selectByMachineryId(updateReqVO.getMachineryId());
        if (existed != null && ObjectUtil.notEqual(existed.getId(), updateReqVO.getId())) {
            throw exception(DV_SCADA_CONFIG_MACHINERY_DUPLICATE);
        }
        // 5. 更新
        MesDvScadaConfigDO updateObj = BeanUtils.toBean(updateReqVO, MesDvScadaConfigDO.class);
        scadaConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteScadaConfig(Long id) {
        validateScadaConfigExists(id);
        scadaConfigMapper.deleteById(id);
    }

    @Override
    public MesDvScadaConfigDO getScadaConfig(Long id) {
        return scadaConfigMapper.selectById(id);
    }

    @Override
    public MesDvScadaConfigDO getScadaConfigByMachineryId(Long machineryId) {
        if (machineryId == null) {
            return null;
        }
        return scadaConfigMapper.selectByMachineryId(machineryId);
    }

    @Override
    public PageResult<MesDvScadaConfigDO> getScadaConfigPage(MesDvScadaConfigPageReqVO pageReqVO) {
        return scadaConfigMapper.selectPage(pageReqVO);
    }

    private void validateScadaConfigExists(Long id) {
        if (scadaConfigMapper.selectById(id) == null) {
            throw exception(DV_SCADA_CONFIG_NOT_EXISTS);
        }
    }

    private void validateProtocolType(String protocolType) {
        if (!MesDvScadaProtocolTypeEnum.isValid(protocolType)) {
            throw exception(DV_SCADA_PROTOCOL_TYPE_INVALID);
        }
    }

}
