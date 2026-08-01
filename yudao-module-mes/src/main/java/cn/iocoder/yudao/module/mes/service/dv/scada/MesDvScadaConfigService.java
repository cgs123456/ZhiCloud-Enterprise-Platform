package cn.iocoder.yudao.module.mes.service.dv.scada;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.dv.scada.vo.MesDvScadaConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.scada.MesDvScadaConfigDO;
import jakarta.validation.Valid;

/**
 * MES SCADA 设备配置 Service 接口
 *
 * @author 芋道源码
 */
public interface MesDvScadaConfigService {

    /**
     * 创建 SCADA 设备配置
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createScadaConfig(@Valid MesDvScadaConfigSaveReqVO createReqVO);

    /**
     * 更新 SCADA 设备配置
     *
     * @param updateReqVO 更新信息
     */
    void updateScadaConfig(@Valid MesDvScadaConfigSaveReqVO updateReqVO);

    /**
     * 删除 SCADA 设备配置
     *
     * @param id 编号
     */
    void deleteScadaConfig(Long id);

    /**
     * 获得 SCADA 设备配置
     *
     * @param id 编号
     * @return SCADA 设备配置
     */
    MesDvScadaConfigDO getScadaConfig(Long id);

    /**
     * 根据设备编号获取 SCADA 配置
     *
     * @param machineryId MES 设备编号
     * @return SCADA 设备配置；不存在返回 null
     */
    MesDvScadaConfigDO getScadaConfigByMachineryId(Long machineryId);

    /**
     * 获得 SCADA 设备配置分页
     *
     * @param pageReqVO 分页查询
     * @return SCADA 设备配置分页
     */
    PageResult<MesDvScadaConfigDO> getScadaConfigPage(MesDvScadaConfigPageReqVO pageReqVO);

}
