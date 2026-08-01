package cn.iocoder.yudao.module.qms.service.fmea;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaItemPageReqVO;
import cn.iocoder.yudao.module.qms.controller.admin.fmea.vo.FmeaItemSaveReqVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.fmea.FmeaItemDO;
import cn.iocoder.yudao.module.qms.dal.mysql.fmea.FmeaItemMapper;
import cn.iocoder.yudao.module.qms.enums.qms.FmeaRiskLevelEnum;
import cn.iocoder.yudao.module.qms.enums.qms.FmeaActionPriorityEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.*;

/**
 * QMS FMEA 条目 Service 实现类
 *
 * <p>RPN 自动计算与风险等级判定规则：
 * <ul>
 *   <li>RPN = Severity * Occurrence * Detection（1-1000）</li>
 *   <li>RPN >= 200 高风险（红色）</li>
 *   <li>100 <= RPN < 200 中风险（黄色）</li>
 *   <li>RPN < 100 低风险（绿色）</li>
 *   <li>S/O/D 任一为 10 时，无论 RPN 多少均标记为高风险</li>
 * </ul>
 *
 * @author 芋道源码
 */
@Service
@Validated
public class FmeaItemServiceImpl implements FmeaItemService {

    @Resource
    private FmeaItemMapper fmeaItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFmeaItem(FmeaItemSaveReqVO createReqVO) {
        // 校验 S/O/D 范围
        validateSod(createReqVO.getSeverity(), createReqVO.getOccurrence(), createReqVO.getDetection());
        // 插入
        FmeaItemDO fmeaItem = BeanUtils.toBean(createReqVO, FmeaItemDO.class);
        // 自动计算 RPN
        fmeaItem.setRpn(calculateRpn(createReqVO.getSeverity(), createReqVO.getOccurrence(), createReqVO.getDetection()));
        // P0-10：自动计算 AIAG-VDA 行动优先级
        fmeaItem.setActionPriority(calculateActionPriority(createReqVO.getSeverity(), createReqVO.getOccurrence(), createReqVO.getDetection()));
        fmeaItemMapper.insert(fmeaItem);
        // 返回
        return fmeaItem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFmeaItem(FmeaItemSaveReqVO updateReqVO) {
        // 校验存在
        validateFmeaItemExists(updateReqVO.getId());
        // 校验 S/O/D 范围
        validateSod(updateReqVO.getSeverity(), updateReqVO.getOccurrence(), updateReqVO.getDetection());
        // 更新
        FmeaItemDO updateObj = BeanUtils.toBean(updateReqVO, FmeaItemDO.class);
        // 自动重算 RPN
        updateObj.setRpn(calculateRpn(updateReqVO.getSeverity(), updateReqVO.getOccurrence(), updateReqVO.getDetection()));
        // P0-10：自动重算 AIAG-VDA 行动优先级
        updateObj.setActionPriority(calculateActionPriority(updateReqVO.getSeverity(), updateReqVO.getOccurrence(), updateReqVO.getDetection()));
        fmeaItemMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFmeaItem(Long id) {
        // 校验存在
        validateFmeaItemExists(id);
        // 删除
        fmeaItemMapper.deleteById(id);
    }

    private void validateFmeaItemExists(Long id) {
        if (fmeaItemMapper.selectById(id) == null) {
            throw exception(FMEA_ITEM_NOT_EXISTS);
        }
    }

    /**
     * 校验严重度/频度/探测度范围（1-10）
     */
    private void validateSod(Integer severity, Integer occurrence, Integer detection) {
        if (!isValidSod(severity) || !isValidSod(occurrence) || !isValidSod(detection)) {
            throw exception(FMEA_ITEM_SOD_INVALID);
        }
    }

    private boolean isValidSod(Integer value) {
        return value != null && value >= 1 && value <= 10;
    }

    @Override
    public FmeaItemDO getFmeaItem(Long id) {
        return fmeaItemMapper.selectById(id);
    }

    @Override
    public PageResult<FmeaItemDO> getFmeaItemPage(FmeaItemPageReqVO pageReqVO) {
        return fmeaItemMapper.selectPage(pageReqVO);
    }

    @Override
    public List<FmeaItemDO> getFmeaItemListByFmeaId(Long fmeaId) {
        return fmeaItemMapper.selectListByFmeaId(fmeaId);
    }

    /**
     * 计算 RPN = Severity * Occurrence * Detection
     *
     * @param severity 严重度 S
     * @param occurrence 频度 O
     * @param detection 探测度 D
     * @return RPN（1-1000）
     */
    private int calculateRpn(Integer severity, Integer occurrence, Integer detection) {
        return severity * occurrence * detection;
    }

    /**
     * P0-10：计算 AIAG-VDA 2019 行动优先级
     *
     * @param severity  严重度 S
     * @param occurrence 频度 O
     * @param detection  探测度 D
     * @return 行动优先级代码（HIGH/MEDIUM/LOW）
     */
    private String calculateActionPriority(Integer severity, Integer occurrence, Integer detection) {
        FmeaActionPriorityEnum ap = FmeaActionPriorityCalculator.calculate(
                severity == null ? 1 : severity,
                occurrence == null ? 1 : occurrence,
                detection == null ? 10 : detection);
        return ap.getCode();
    }

    /**
     * 判定风险等级
     *
     * <p>S/O/D 任一为 10 时，无论 RPN 多少均标记为高风险。
     *
     * @param rpn RPN
     * @param severity 严重度
     * @param occurrence 频度
     * @param detection 探测度
     * @return 风险等级
     */
    public static FmeaRiskLevelEnum determineRiskLevel(int rpn, Integer severity, Integer occurrence, Integer detection) {
        // S/O/D 任一为 10 时自动标记为高风险
        if (severity != null && severity == 10
                || occurrence != null && occurrence == 10
                || detection != null && detection == 10) {
            return FmeaRiskLevelEnum.HIGH;
        }
        return FmeaRiskLevelEnum.of(rpn);
    }

}
