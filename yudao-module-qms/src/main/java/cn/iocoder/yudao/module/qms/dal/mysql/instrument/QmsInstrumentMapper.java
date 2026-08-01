package cn.iocoder.yudao.module.qms.dal.mysql.instrument;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentPageReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.instrument.QmsInstrumentDO;
import cn.iocoder.yudao.module.qms.enums.instrument.QmsInstrumentStatusEnum;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * QMS 计量器具台账 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface QmsInstrumentMapper extends BaseMapperX<QmsInstrumentDO> {

    default QmsInstrumentDO selectByCode(String code) {
        return selectOne(QmsInstrumentDO::getCode, code);
    }

    default PageResult<QmsInstrumentDO> selectPage(QmsInstrumentPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsInstrumentDO>()
                .likeIfPresent(QmsInstrumentDO::getCode, reqVO.getCode())
                .likeIfPresent(QmsInstrumentDO::getName, reqVO.getName())
                .eqIfPresent(QmsInstrumentDO::getCategory, reqVO.getCategory())
                .eqIfPresent(QmsInstrumentDO::getStatus, reqVO.getStatus())
                .likeIfPresent(QmsInstrumentDO::getResponsiblePerson, reqVO.getResponsiblePerson())
                .likeIfPresent(QmsInstrumentDO::getLocation, reqVO.getLocation())
                .eqIfPresent(QmsInstrumentDO::getManufacturer, reqVO.getManufacturer())
                .betweenIfPresent(QmsInstrumentDO::getNextCalibrationDate, reqVO.getNextCalibrationDate())
                .orderByAsc(QmsInstrumentDO::getSort)
                .orderByDesc(QmsInstrumentDO::getId));
    }

    /**
     * 查询校准即将到期的器具列表（在用状态 + next_calibration_date <= 截止日期）
     *
     * @param deadline 截止日期（now + withinDays）
     * @return 器具列表
     */
    default List<QmsInstrumentDO> selectListByExpiringSoon(LocalDate deadline) {
        return selectList(new LambdaQueryWrapperX<QmsInstrumentDO>()
                .eq(QmsInstrumentDO::getStatus, QmsInstrumentStatusEnum.IN_USE.getStatus())
                .isNotNull(QmsInstrumentDO::getNextCalibrationDate)
                .le(QmsInstrumentDO::getNextCalibrationDate, deadline)
                .orderByAsc(QmsInstrumentDO::getNextCalibrationDate));
    }

    /**
     * 查询已过期未校准的器具列表（在用状态 + next_calibration_date < 今天）
     *
     * @param today 今天日期
     * @return 器具列表
     */
    default List<QmsInstrumentDO> selectListOverdue(LocalDate today) {
        return selectList(new LambdaQueryWrapperX<QmsInstrumentDO>()
                .eq(QmsInstrumentDO::getStatus, QmsInstrumentStatusEnum.IN_USE.getStatus())
                .isNotNull(QmsInstrumentDO::getNextCalibrationDate)
                .lt(QmsInstrumentDO::getNextCalibrationDate, today)
                .orderByAsc(QmsInstrumentDO::getNextCalibrationDate));
    }

    /**
     * 查询下次校准日期在指定区间内的器具列表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 器具列表
     */
    default List<QmsInstrumentDO> selectListByCalibrationDateRange(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<QmsInstrumentDO>()
                .geIfPresent(QmsInstrumentDO::getNextCalibrationDate, startDate)
                .leIfPresent(QmsInstrumentDO::getNextCalibrationDate, endDate)
                .isNotNull(QmsInstrumentDO::getNextCalibrationDate)
                .orderByAsc(QmsInstrumentDO::getNextCalibrationDate));
    }

}
