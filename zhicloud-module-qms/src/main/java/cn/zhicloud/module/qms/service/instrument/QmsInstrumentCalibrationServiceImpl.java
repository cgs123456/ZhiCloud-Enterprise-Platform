package cn.zhicloud.module.qms.service.instrument;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationPageReqVO;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.instrument.QmsInstrumentCalibrationDO;
import cn.zhicloud.module.qms.dal.dataobject.instrument.QmsInstrumentDO;
import cn.zhicloud.module.qms.dal.mysql.instrument.QmsInstrumentCalibrationMapper;
import cn.zhicloud.module.qms.dal.mysql.instrument.QmsInstrumentMapper;
import cn.zhicloud.module.qms.enums.instrument.QmsInstrumentStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.CALIBRATION_NO_DUPLICATE;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.CALIBRATION_NOT_EXISTS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSTRUMENT_NOT_CALIBRATABLE;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSTRUMENT_NOT_EXISTS;

/**
 * QMS 计量器具校准记录 Service 实现类
 *
 * @author 智云
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
        // 5. 重算器具的 last/next_calibration_date（派生自「最新」一条校准记录，§12.238）
        refreshInstrumentCalibrationDates(calibration.getInstrumentId());
        return calibration.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCalibration(QmsInstrumentCalibrationSaveReqVO updateReqVO) {
        // 1. 校验存在（并取出旧记录：刷新日期需要旧器具 ID）
        QmsInstrumentCalibrationDO oldCalibration = validateCalibrationExists(updateReqVO.getId());
        // 2. 校准证书编号唯一
        validateCalibrationNoUnique(updateReqVO.getId(), updateReqVO.getCalibrationNo());
        // 3. 更新
        QmsInstrumentCalibrationDO updateObj = BeanUtils.toBean(updateReqVO, QmsInstrumentCalibrationDO.class);
        calibrationMapper.updateById(updateObj);
        // 4. 重算器具的 last/next_calibration_date（§12.238）：记录被移动时两侧器具都重算
        refreshInstrumentCalibrationDates(oldCalibration.getInstrumentId());
        if (!Objects.equals(oldCalibration.getInstrumentId(), updateReqVO.getInstrumentId())) {
            refreshInstrumentCalibrationDates(updateReqVO.getInstrumentId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCalibration(Long id) {
        // 校验存在（并取出记录：刷新日期需要器具 ID）
        QmsInstrumentCalibrationDO calibration = validateCalibrationExists(id);
        // 删除
        calibrationMapper.deleteById(id);
        // 重算器具的 last/next_calibration_date（§12.238）：删掉最新记录后日期要跟着回退/清空
        refreshInstrumentCalibrationDates(calibration.getInstrumentId());
    }

    private QmsInstrumentCalibrationDO validateCalibrationExists(Long id) {
        QmsInstrumentCalibrationDO calibration = calibrationMapper.selectById(id);
        if (calibration == null) {
            throw exception(CALIBRATION_NOT_EXISTS);
        }
        return calibration;
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
     * 重算器具的 last_calibration_date 与 next_calibration_date（§12.238）。
     *
     * <p>两个日期是「最新一条校准记录」的派生值（按校准日期降序、同日期按编号降序）。
     * 修复前此处把**本条记录**的日期无条件回写器具：补录一条历史校准就会把器具日期改旧，
     * 校准到期预警口径随之失真；而更新/删除记录又完全不回写，删掉最新记录后器具仍指向
     * 已删除的记录。现在 create/update/delete 三处统一重算；无有效记录时显式置空。
     *
     * <p>next 的口径不变：优先取记录中的 next_calibration_date；若为空则按器具校准周期推算。
     * 置空必须用 UpdateWrapper（updateById 会跳过 null 字段，无法把列改回 NULL）。
     */
    private void refreshInstrumentCalibrationDates(Long instrumentId) {
        QmsInstrumentDO instrument = instrumentMapper.selectById(instrumentId);
        if (instrument == null) {
            return; // 防御：器具已被删除（正常流程不会出现）
        }
        QmsInstrumentCalibrationDO latest = calibrationMapper.selectLatestByInstrumentId(instrumentId);
        LocalDate lastDate = latest == null ? null : latest.getCalibrationDate();
        LocalDate nextDate = latest == null ? null : latest.getNextCalibrationDate();
        if (nextDate == null && lastDate != null && instrument.getCalibrationCycleDays() != null) {
            nextDate = lastDate.plusDays(instrument.getCalibrationCycleDays());
        }
        instrumentMapper.update(null, new LambdaUpdateWrapper<QmsInstrumentDO>()
                .eq(QmsInstrumentDO::getId, instrumentId)
                .set(QmsInstrumentDO::getLastCalibrationDate, lastDate)
                .set(QmsInstrumentDO::getNextCalibrationDate, nextDate));
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