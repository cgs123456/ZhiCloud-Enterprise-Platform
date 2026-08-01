package cn.iocoder.yudao.module.qms.service.instrument;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.instrument.vo.QmsInstrumentCalibrationSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.instrument.QmsInstrumentCalibrationDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS 计量器具校准记录 Service 接口
 *
 * @author 芋道源码
 */
public interface QmsInstrumentCalibrationService {

    /**
     * 记录校准
     *
     * <p>新增校准记录，并自动更新关联计量器具的 last/next_calibration_date。
     * <p>仅当器具状态不是「报废」或「封存」时允许记录。
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long recordCalibration(@Valid QmsInstrumentCalibrationSaveReqVO createReqVO);

    /**
     * 更新校准记录
     *
     * @param updateReqVO 更新信息
     */
    void updateCalibration(@Valid QmsInstrumentCalibrationSaveReqVO updateReqVO);

    /**
     * 删除校准记录
     *
     * @param id 编号
     */
    void deleteCalibration(Long id);

    /**
     * 获得校准记录
     *
     * @param id 编号
     * @return 校准记录
     */
    QmsInstrumentCalibrationDO getCalibration(Long id);

    /**
     * 获得校准记录分页
     *
     * @param pageReqVO 分页查询
     * @return 校准记录分页
     */
    PageResult<QmsInstrumentCalibrationDO> getCalibrationPage(QmsInstrumentCalibrationPageReqVO pageReqVO);

    /**
     * 获得指定器具的校准记录列表
     *
     * @param instrumentId 器具 ID
     * @return 校准记录列表
     */
    List<QmsInstrumentCalibrationDO> getCalibrationListByInstrumentId(Long instrumentId);

}
