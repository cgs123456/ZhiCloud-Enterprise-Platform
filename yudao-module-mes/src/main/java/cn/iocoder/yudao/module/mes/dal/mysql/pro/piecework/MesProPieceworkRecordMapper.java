package cn.iocoder.yudao.module.mes.dal.mysql.pro.piecework;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.piecework.vo.MesProPieceworkRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MES 计件工资明细 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface MesProPieceworkRecordMapper extends BaseMapperX<MesProPieceworkRecordDO> {

    default PageResult<MesProPieceworkRecordDO> selectPage(MesProPieceworkRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProPieceworkRecordDO>()
                .eqIfPresent(MesProPieceworkRecordDO::getFeedbackId, reqVO.getFeedbackId())
                .eqIfPresent(MesProPieceworkRecordDO::getFeedbackUserId, reqVO.getFeedbackUserId())
                .eqIfPresent(MesProPieceworkRecordDO::getWorkOrderId, reqVO.getWorkOrderId())
                .eqIfPresent(MesProPieceworkRecordDO::getProcessId, reqVO.getProcessId())
                .eqIfPresent(MesProPieceworkRecordDO::getItemId, reqVO.getItemId())
                .eqIfPresent(MesProPieceworkRecordDO::getWorkstationId, reqVO.getWorkstationId())
                .eqIfPresent(MesProPieceworkRecordDO::getPeriodMonth, reqVO.getPeriodMonth())
                .eqIfPresent(MesProPieceworkRecordDO::getStatus, reqVO.getStatus())
                .orderByDesc(MesProPieceworkRecordDO::getId));
    }

    default MesProPieceworkRecordDO selectByFeedbackId(Long feedbackId) {
        return selectOne(MesProPieceworkRecordDO::getFeedbackId, feedbackId);
    }

    /**
     * 按员工 + 月份查询正常状态的计件明细（用于汇总）
     */
    default List<MesProPieceworkRecordDO> selectListByUserAndPeriod(Long feedbackUserId, String periodMonth) {
        return selectList(new LambdaQueryWrapperX<MesProPieceworkRecordDO>()
                .eqIfPresent(MesProPieceworkRecordDO::getFeedbackUserId, feedbackUserId)
                .eqIfPresent(MesProPieceworkRecordDO::getPeriodMonth, periodMonth)
                .eq(MesProPieceworkRecordDO::getStatus, 0)); // 0=正常
    }

    /**
     * 按月份查询所有正常状态的计件明细（用于月度汇总 Job）
     */
    default List<MesProPieceworkRecordDO> selectListByPeriod(String periodMonth) {
        return selectList(new LambdaQueryWrapperX<MesProPieceworkRecordDO>()
                .eq(MesProPieceworkRecordDO::getPeriodMonth, periodMonth)
                .eq(MesProPieceworkRecordDO::getStatus, 0));
    }

}
