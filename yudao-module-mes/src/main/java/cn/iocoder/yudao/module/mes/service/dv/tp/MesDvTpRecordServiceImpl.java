package cn.iocoder.yudao.module.mes.service.dv.tp;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo.MesDvTpRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp.MesDvTpRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.dv.tp.MesDvTpRecordMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.DV_TP_RECORD_NOT_EXISTS;

/**
 * MES TPM 执行记录 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class MesDvTpRecordServiceImpl implements MesDvTpRecordService {

    @Resource
    private MesDvTpRecordMapper tpRecordMapper;

    @Override
    public MesDvTpRecordDO getTpRecord(Long id) {
        return tpRecordMapper.selectById(id);
    }

    @Override
    public PageResult<MesDvTpRecordDO> getTpRecordPage(MesDvTpRecordPageReqVO pageReqVO) {
        return tpRecordMapper.selectPage(pageReqVO);
    }

    @Override
    public List<MesDvTpRecordDO> getTpRecordListByPlanId(Long planId) {
        return tpRecordMapper.selectListByPlanId(planId);
    }

    @Override
    public List<MesDvTpRecordDO> getTpRecordListByEquipmentAndPeriod(Long equipmentId, String periodStart, String periodEnd) {
        return tpRecordMapper.selectListByEquipmentIdAndPeriod(equipmentId, periodStart, periodEnd);
    }

    /**
     * 校验执行记录存在
     */
    public MesDvTpRecordDO validateTpRecord(Long id) {
        MesDvTpRecordDO record = tpRecordMapper.selectById(id);
        if (record == null) {
            throw exception(DV_TP_RECORD_NOT_EXISTS);
        }
        return record;
    }

}