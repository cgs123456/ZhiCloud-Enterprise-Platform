package cn.zhicloud.module.qms.dal.mysql.instrument;

import cn.hutool.core.collection.CollUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationPageReqVO;
import cn.zhicloud.module.qms.dal.dataobject.instrument.QmsInstrumentCalibrationDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

/**
 * QMS 计量器具校准记录 Mapper
 *
 * @author 智云
 */
@Mapper
public interface QmsInstrumentCalibrationMapper extends BaseMapperX<QmsInstrumentCalibrationDO> {

    default QmsInstrumentCalibrationDO selectByCalibrationNo(String calibrationNo) {
        return selectOne(QmsInstrumentCalibrationDO::getCalibrationNo, calibrationNo);
    }

    default PageResult<QmsInstrumentCalibrationDO> selectPage(QmsInstrumentCalibrationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<QmsInstrumentCalibrationDO>()
                .eqIfPresent(QmsInstrumentCalibrationDO::getInstrumentId, reqVO.getInstrumentId())
                .likeIfPresent(QmsInstrumentCalibrationDO::getCalibrationNo, reqVO.getCalibrationNo())
                .eqIfPresent(QmsInstrumentCalibrationDO::getCalibrationResult, reqVO.getCalibrationResult())
                .likeIfPresent(QmsInstrumentCalibrationDO::getCalibrationOrganization, reqVO.getCalibrationOrganization())
                .betweenIfPresent(QmsInstrumentCalibrationDO::getCalibrationDate, reqVO.getCalibrationDate())
                .orderByDesc(QmsInstrumentCalibrationDO::getCalibrationDate)
                .orderByDesc(QmsInstrumentCalibrationDO::getId));
    }

    default List<QmsInstrumentCalibrationDO> selectListByInstrumentId(Long instrumentId) {
        return selectList(QmsInstrumentCalibrationDO::getInstrumentId, instrumentId);
    }

    default Long selectCountByInstrumentId(Long instrumentId) {
        return selectCount(QmsInstrumentCalibrationDO::getInstrumentId, instrumentId);
    }

    /**
     * 查询器具「最新」一条校准记录（§12.238：器具 last/next 校准日期的派生基准）。
     *
     * <p>按校准日期降序、同日期按编号降序取第一条；校准日期为空的记录不参与派生。
     */
    default QmsInstrumentCalibrationDO selectLatestByInstrumentId(Long instrumentId) {
        List<QmsInstrumentCalibrationDO> list = selectList(new LambdaQueryWrapperX<QmsInstrumentCalibrationDO>()
                .eq(QmsInstrumentCalibrationDO::getInstrumentId, instrumentId)
                .isNotNull(QmsInstrumentCalibrationDO::getCalibrationDate)
                .orderByDesc(QmsInstrumentCalibrationDO::getCalibrationDate)
                .orderByDesc(QmsInstrumentCalibrationDO::getId));
        return CollUtil.getFirst(list);
    }

    /**
     * 查询校准日期在指定区间内的校准记录
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 校准记录列表
     */
    default List<QmsInstrumentCalibrationDO> selectListByCalibrationDateRange(LocalDate startDate, LocalDate endDate) {
        return selectList(new LambdaQueryWrapperX<QmsInstrumentCalibrationDO>()
                .geIfPresent(QmsInstrumentCalibrationDO::getCalibrationDate, startDate)
                .leIfPresent(QmsInstrumentCalibrationDO::getCalibrationDate, endDate)
                .orderByDesc(QmsInstrumentCalibrationDO::getCalibrationDate));
    }

}
