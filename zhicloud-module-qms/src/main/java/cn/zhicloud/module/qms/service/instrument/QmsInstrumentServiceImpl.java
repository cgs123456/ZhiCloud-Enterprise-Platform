package cn.zhicloud.module.qms.service.instrument;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentExpiringSoonRespVO;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentPageReqVO;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.instrument.QmsInstrumentDO;
import cn.zhicloud.module.qms.dal.mysql.instrument.QmsInstrumentCalibrationMapper;
import cn.zhicloud.module.qms.dal.mysql.instrument.QmsInstrumentMapper;
import cn.zhicloud.module.qms.enums.instrument.QmsInstrumentStatusEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSTRUMENT_CODE_DUPLICATE;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSTRUMENT_HAS_CALIBRATIONS;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.INSTRUMENT_NOT_EXISTS;

/**
 * QMS 计量器具台账 Service 实现类
 *
 * @author 智云
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
        // 状态无条件置为初始值，忽略调用方传入的任何 status。
        // §12.235.5：此前「仅在 null 时补默认」，调用方即可在**建单**这一步直接写入终态，
        // 跳过整个流转（该类可能连流转方法都没有），产生「未审即批/未做即完成」的记录。
        // 状态只能由本模块的流转方法产生。
        instrument.setStatus(QmsInstrumentStatusEnum.IN_USE.getStatus());
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
        // 状态不接受外部写入（§12.236.7）。本类**没有任何流转方法**，
        // 若允许通用修改写状态，则「新增 + 修改」两步就能到达任意终态，
        // 新增侧刚加的护栏形同虚设。
        // MyBatis-Plus 的 updateById 跳过 null 字段，故置 null 的效果是
        // 该列不进 UPDATE 语句 —— 调用方的 status 无法生效。
        updateObj.setStatus(null);
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