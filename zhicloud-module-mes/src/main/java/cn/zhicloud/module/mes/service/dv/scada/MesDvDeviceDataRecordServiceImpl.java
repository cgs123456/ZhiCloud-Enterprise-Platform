package cn.zhicloud.module.mes.service.dv.scada;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.dv.scada.vo.MesDvDeviceDataRecordPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.dv.scada.MesDvDeviceDataRecordDO;
import cn.zhicloud.module.mes.dal.mysql.dv.scada.MesDvDeviceDataRecordMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.DV_DEVICE_DATA_RECORD_NOT_EXISTS;

/**
 * MES 设备数据采集记录 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class MesDvDeviceDataRecordServiceImpl implements MesDvDeviceDataRecordService {

    @Resource
    private MesDvDeviceDataRecordMapper dataRecordMapper;

    @Override
    public PageResult<MesDvDeviceDataRecordDO> getDataRecordPage(MesDvDeviceDataRecordPageReqVO pageReqVO) {
        return dataRecordMapper.selectPage(pageReqVO);
    }

    @Override
    public MesDvDeviceDataRecordDO getDataRecord(Long id) {
        return dataRecordMapper.selectById(id);
    }

    @Override
    public MesDvDeviceDataRecordDO getLatestDataRecord(Long machineryId) {
        if (machineryId == null) {
            return null;
        }
        return dataRecordMapper.selectLatestByMachineryId(machineryId);
    }

    @Override
    public List<MesDvDeviceDataRecordDO> getDataRecordListByTimeRange(Long machineryId,
                                                                       LocalDateTime start,
                                                                       LocalDateTime end) {
        if (machineryId == null || start == null || end == null) {
            return Collections.emptyList();
        }
        return dataRecordMapper.selectListByMachineryAndTimeRange(machineryId, start, end);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveRecords(List<MesDvDeviceDataRecordDO> records) {
        if (CollUtil.isEmpty(records)) {
            return 0;
        }
        dataRecordMapper.insertBatch(records);
        return records.size();
    }

    @Override
    public void deleteDataRecord(Long id) {
        if (dataRecordMapper.selectById(id) == null) {
            throw exception(DV_DEVICE_DATA_RECORD_NOT_EXISTS);
        }
        dataRecordMapper.deleteById(id);
    }

}
