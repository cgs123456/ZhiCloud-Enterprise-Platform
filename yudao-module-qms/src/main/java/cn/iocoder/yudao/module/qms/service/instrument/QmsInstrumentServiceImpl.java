package cn.iocoder.yudao.module.qms.service.instrument;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentExpiringSoonRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentSaveReqVO;
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
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.INSTRUMENT_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.INSTRUMENT_HAS_CALIBRATIONS;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.INSTRUMENT_NOT_EXISTS;

/**
 * QMS 计量器具台账 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class QmsInstrumentServiceImpl implements QmsInstrumentService {

    @Resource
    private QmsInstrumentMapper instrumentMapper;

    @Resource
    private QmsInstrumentCalibrationMapper calibrationMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInstrument(QmsInstrumentSaveReqVO createReqVO) {
        // 校验器具编号唯一
        validateCodeUnique(null, createReqVO.getCode());
        // 插入
        QmsInstrumentDO instrument = BeanUtils.toBean(createReqVO, QmsInstrumentDO.class);
        // 默认状态为在用
        if (instrument.getStatus() == null) {
            instrument.setStatus(QmsInstrumentStatusEnum.IN_USE.getStatus());
        }
        instrumentMapper.insert(instrument);
        // 返回
        return instrument.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInstrument(QmsInstrumentSaveReqVO updateReqVO) {
        // 校验存在
        validateInstrumentExists(updateReqVO.getId());
        // 校验器具编号唯一
        validateCodeUnique(updateReqVO.getId(), updateReqVO.getCode());
        // 更新
        QmsInstrumentDO updateObj = BeanUtils.toBean(updateReqVO, QmsInstrumentDO.class);
        instrumentMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInstrument(Long id) {
        // 校验存在
        validateInstrumentExists(id);
        // 校验是否存在校准记录
        if (calibrationMapper.selectCountByInstrumentId(id) > 0) {
            throw exception(INSTRUMENT_HAS_CALIBRATIONS);
        }
        // 删除
        instrumentMapper.deleteById(id);
    }

    private void validateInstrumentExists(Long id) {
        if (instrumentMapper.selectById(id) == null) {
            throw exception(INSTRUMENT_NOT_EXISTS);
        }
    }

    private void validateCodeUnique(Long id, String code) {
        QmsInstrumentDO existing = instrumentMapper.selectByCode(code);
        if (existing == null) {
            return;
        }
        if (id == null || !id.equals(existing.getId())) {
            throw exception(INSTRUMENT_CODE_DUPLICATE);
        }
    }

    @Override
    public QmsInstrumentDO getInstrument(Long id) {
        return instrumentMapper.selectById(id);
    }

    @Override
    public PageResult<QmsInstrumentDO> getInstrumentPage(QmsInstrumentPageReqVO pageReqVO) {
        return instrumentMapper.selectPage(pageReqVO);
    }

    @Override
    public List<QmsInstrumentExpiringSoonRespVO> getExpiringSoonInstruments(int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate deadline = today.plusDays(withinDays);
        List<QmsInstrumentDO> list = instrumentMapper.selectListByExpiringSoon(deadline);
        List<QmsInstrumentExpiringSoonRespVO> result = BeanUtils.toBean(list, QmsInstrumentExpiringSoonRespVO.class);
        result.forEach(vo -> vo.computeRemaining(today));
        return result;
    }

    @Override
    public List<QmsInstrumentExpiringSoonRespVO> getOverdueInstruments() {
        LocalDate today = LocalDate.now();
        List<QmsInstrumentDO> list = instrumentMapper.selectListOverdue(today);
        List<QmsInstrumentExpiringSoonRespVO> result = BeanUtils.toBean(list, QmsInstrumentExpiringSoonRespVO.class);
        result.forEach(vo -> vo.computeRemaining(today));
        return result;
    }

}