package cn.zhicloud.module.qms.service.instrument;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentExpiringSoonRespVO;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentPageReqVO;
import cn.zhicloud.module.qms.controller.admin.instrument.vo.QmsInstrumentSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.instrument.QmsInstrumentDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS 计量器具台账 Service 接口
 *
 * @author 智云
 */
public interface QmsInstrumentService {

    /**
     * 创建计量器具
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createInstrument(@Valid QmsInstrumentSaveReqVO createReqVO);

    /**
     * 更新计量器具
     *
     * @param updateReqVO 更新信息
     */
    void updateInstrument(@Valid QmsInstrumentSaveReqVO updateReqVO);

    /**
     * 删除计量器具
     *
     * <p>若存在校准记录则不允许删除。
     *
     * @param id 编号
     */
    void deleteInstrument(Long id);

    /**
     * 获得计量器具
     *
     * @param id 编号
     * @return 计量器具
     */
    QmsInstrumentDO getInstrument(Long id);

    /**
     * 获得计量器具分页
     *
     * @param pageReqVO 分页查询
     * @return 计量器具分页
     */
    PageResult<QmsInstrumentDO> getInstrumentPage(QmsInstrumentPageReqVO pageReqVO);

    /**
     * 获得校准即将到期的器具列表
     *
     * <p>返回在用状态且 next_calibration_date &lt;= today + withinDays 的器具。
     *
     * @param withinDays 未来天数
     * @return 即将到期器具列表
     */
    List<QmsInstrumentExpiringSoonRespVO> getExpiringSoonInstruments(int withinDays);

    /**
     * 获得已逾期未校准的器具列表
     *
     * <p>返回在用状态且 next_calibration_date &lt; today 的器具。
     *
     * @return 已逾期器具列表
     */
    List<QmsInstrumentExpiringSoonRespVO> getOverdueInstruments();

}
