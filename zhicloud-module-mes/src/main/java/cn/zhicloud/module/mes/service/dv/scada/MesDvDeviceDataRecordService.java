package cn.zhicloud.module.mes.service.dv.scada;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.dv.scada.vo.MesDvDeviceDataRecordPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.dv.scada.MesDvDeviceDataRecordDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MES 设备数据采集记录 Service 接口
 *
 * @author 智云
 */
public interface MesDvDeviceDataRecordService {

    /**
     * 获得设备数据采集记录分页
     *
     * @param pageReqVO 分页查询
     * @return 分页结果
     */
    PageResult<MesDvDeviceDataRecordDO> getDataRecordPage(MesDvDeviceDataRecordPageReqVO pageReqVO);

    /**
     * 获得设备数据采集记录
     *
     * @param id 编号
     * @return 记录
     */
    MesDvDeviceDataRecordDO getDataRecord(Long id);

    /**
     * 按设备编号查询最新一帧采集数据
     *
     * @param machineryId 设备编号
     * @return 最新一条记录；无数据返回 null
     */
    MesDvDeviceDataRecordDO getLatestDataRecord(Long machineryId);

    /**
     * 按设备编号与时间范围查询采集记录
     *
     * @param machineryId 设备编号
     * @param start       开始时间（含）
     * @param end         结束时间（含）
     * @return 记录列表，按采集时间升序
     */
    List<MesDvDeviceDataRecordDO> getDataRecordListByTimeRange(Long machineryId,
                                                               LocalDateTime start,
                                                               LocalDateTime end);

    /**
     * 批量保存设备数据采集记录
     *
     * @param records 记录列表
     * @return 保存数量
     */
    int saveRecords(List<MesDvDeviceDataRecordDO> records);

    /**
     * 删除设备数据采集记录
     *
     * @param id 编号
     */
    void deleteDataRecord(Long id);

}
