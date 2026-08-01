package cn.iocoder.yudao.module.qms.service.instrument;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.instrument.QmsInstrumentCalibrationDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.instrument.QmsInstrumentDO;
import cn.iocoder.yudao.module.qms.dal.mysql.instrument.QmsInstrumentCalibrationMapper;
import cn.iocoder.yudao.module.qms.dal.mysql.instrument.QmsInstrumentMapper;
import cn.iocoder.yudao.module.qms.enums.instrument.QmsInstrumentStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.CALIBRATION_NO_DUPLICATE;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.CALIBRATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.INSTRUMENT_NOT_CALIBRATABLE;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.INSTRUMENT_NOT_EXISTS;

/**
 * QMS 计量器具校准记录 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class QmsInstrumentCalibrationServiceImpl implements QmsInstrumentCalibrationService {

    @Resource
    private QmsInstrumentCalibrationMapper calibrationMapper;

    @Resource
    private QmsInstrumentMapper instrumentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordCalibration(QmsInstrumentCalibrationSaveReqVO createReqVO) {
        // 1. 校验器具存在
        QmsInstrumentDO instrument = instrumentMapper.selectById(createReqVO.getInstrumentId());
        if (instrument == null) {
            throw exception(INSTRUMENT_NOT_EXISTS);
        }
        // 2. 校验器具状态：报废/封存不允许校准
        validateCalibratable(instrument);
        // 3. 校准证书编号唯一
        validateCalibrationNoUnique(null, createReqVO.getCalibrationNo());
        // 4. 插入校准记录
        QmsInstrumentCalibrationDO calibration = BeanUtils.toBean(createReqVO, QmsInstrumentCalibrationDO.class);
        calibrationMapper.insert(calibration);
        // 5. 自动更新器具的 last/next_calibration_date
        updateInstrumentCalibrationDates(instrument, calibration);
        return calibration.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCalibration(QmsInstrumentCalibrationSaveReqVO updateReqVO) {
        // 1. 校验存在
        validateCalibrationExists(updateReqVO.getId());
        // 2. 校准证书编号唯一
        validateCalibrationNoUnique(updateReqVO.getId(), updateReqVO.getCalibrationNo());
        // 3. 更新
        QmsInstrumentCalibrationDO updateObj = BeanUtils.toBean(updateReqVO, QmsInstrumentCalibrationDO.class);
        calibrationMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCalibration(Long id) {
        // 校验存在
        validateCalibrationExists(id);
        // 删除
        calibrationMapper.deleteById(id);
    }

    private void validateCalibrationExists(Long id) {
        if (calibrationMapper.selectById(id) == null) {
            throw exception(CALIBRATION_NOT_EXISTS);
        }
    }

    private void validateCalibrationNoUnique(Long id, String calibrationNo) {
        QmsInstrumentCalibrationDO existing = calibrationMapper.selectByCalibrationNo(calibrationNo);
        if (existing == null) {
            return;
        }
        if (id == null || !id.equals(existing.getId())) {
            throw exception(CALIBRATION_NO_DUPLICATE);
        }
    }

    /**
     * 校验器具是否允许记录校准（报废/封存状态不允许）
     */
    private void validateCalibratable(QmsInstrumentDO instrument) {
        Integer status = instrument.getStatus();
        if (QmsInstrumentStatusEnum.SCRAPPED.getStatus().equals(status)
                || QmsInstrumentStatusEnum.SEALED.getStatus().equals(status)) {
            throw exception(INSTRUMENT_NOT_CALIBRATABLE);
        }
    }

    /**
     * 根据校准记录自动更新器具的 last_calibration_date 与 next_calibration_date。
     *
     * <p>next_calibration_date 优先取校准记录中的值；若为空则按器具校准周期计算。
     */
    private void updateInstrumentCalibrationDates(QmsInstrumentDO instrument, QmsInstrumentCalibrationDO calibration) {
        QmsInstrumentDO updateObj = new QmsInstrumentDO();
        updateObj.setId(instrument.getId());
        updateObj.setLastCalibrationDate(calibration.getCalibrationDate());
        // 下次校准日期：优先使用校准记录中的 next_calibration_date
        LocalDate nextDate = calibration.getNextCalibrationDate();
        if (nextDate == null && instrument.getCalibrationCycleDays() != null
                && calibration.getCalibrationDate() != null) {
            nextDate = calibration.getCalibrationDate().plusDays(instrument.getCalibrationCycleDays());
        }
        updateObj.setNextCalibrationDate(nextDate);
        instrumentMapper.updateById(updateObj);
    }

    @Override
    public QmsInstrumentCalibrationDO getCalibration(Long id) {
        return calibrationMapper.selectById(id);
    }

    @Override
    public PageResult<QmsInstrumentCalibrationDO> getCalibrationPage(QmsInstrumentCalibrationPageReqVO pageReqVO) {
        return calibrationMapper.selectPage(pageReqVO);
    }

    @Override
    public List<QmsInstrumentCalibrationDO> getCalibrationListByInstrumentId(Long instrumentId) {
        return calibrationMapper.selectListByInstrumentId(instrumentId);
    }

}