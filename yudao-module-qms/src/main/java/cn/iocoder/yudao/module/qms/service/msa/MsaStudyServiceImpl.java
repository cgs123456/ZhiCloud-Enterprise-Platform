package cn.iocoder.yudao.module.qms.service.msa;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.qms.controller.admin.msa.vo.*;
import cn.iocoder.yudao.module.qms.dal.dataobject.msa.MsaMeasurementDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.msa.MsaStudyDO;
import cn.iocoder.yudao.module.qms.dal.mysql.msa.MsaMeasurementMapper;
import cn.iocoder.yudao.module.qms.dal.mysql.msa.MsaStudyMapper;
import cn.iocoder.yudao.module.qms.enums.qms.MsaStatusEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.*;

/**
 * QMS MSA 研究 Service 实现类
 *
 * <p>GR&R 采用均值极差法（Xbar-R）：
 * <ul>
 *   <li>EV（重复性）= R̄ × K1，K1 = 1/d2(trials)</li>
 *   <li>AV（再现性）= sqrt((X̄_diff × K2)² − EV²/(parts×trials))，K2 = 1/d2(appraisers)</li>
 *   <li>Gage R&R = sqrt(EV² + AV²)</li>
 *   <li>PV（零件变异）= Rp × K3，K3 = 1/d2(parts)</li>
 *   <li>TV（总变异）= sqrt(Gage R&R² + PV²)</li>
 *   <li>%GR&R = Gage R&R / TV × 100%</li>
 * </ul>
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class MsaStudyServiceImpl implements MsaStudyService {

    private static final int SCALE = 4;

    @Resource
    private MsaStudyMapper msaStudyMapper;

    @Resource
    private MsaMeasurementMapper msaMeasurementMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createMsaStudy(MsaStudySaveReqVO createReqVO) {
        // 插入
        MsaStudyDO msaStudy = BeanUtils.toBean(createReqVO, MsaStudyDO.class);
        // 默认状态为草稿
        if (msaStudy.getStatus() == null) {
            msaStudy.setStatus(MsaStatusEnum.DRAFT.getStatus());
        }
        msaStudyMapper.insert(msaStudy);
        // 返回
        return msaStudy.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMsaStudy(MsaStudySaveReqVO updateReqVO) {
        // 校验存在
        validateMsaStudyExists(updateReqVO.getId());
        // 更新（P0 修复：屏蔽 status 字段，状态变更必须走 calculateGageRR 等专门方法）
        MsaStudyDO updateObj = BeanUtils.toBean(updateReqVO, MsaStudyDO.class);
        updateObj.setStatus(null);
        msaStudyMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMsaStudy(Long id) {
        // 校验存在
        validateMsaStudyExists(id);
        // 删除
        msaStudyMapper.deleteById(id);
    }

    private void validateMsaStudyExists(Long id) {
        if (msaStudyMapper.selectById(id) == null) {
            throw exception(MSA_STUDY_NOT_EXISTS);
        }
    }

    @Override
    public MsaStudyDO getMsaStudy(Long id) {
        return msaStudyMapper.selectById(id);
    }

    @Override
    public PageResult<MsaStudyDO> getMsaStudyPage(MsaStudyPageReqVO pageReqVO) {
        return msaStudyMapper.selectPage(pageReqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveMeasurement(MsaMeasurementSaveReqVO reqVO) {
        // 校验研究存在
        validateMsaStudyExists(reqVO.getStudyId());
        // 插入或更新
        MsaMeasurementDO measurement = BeanUtils.toBean(reqVO, MsaMeasurementDO.class);
        if (reqVO.getId() == null) {
            msaMeasurementMapper.insert(measurement);
        } else {
            msaMeasurementMapper.updateById(measurement);
        }
        return measurement.getId();
    }

    @Override
    public List<MsaMeasurementDO> getMeasurementData(Long studyId) {
        return msaMeasurementMapper.selectListByStudyId(studyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MsaGageRRRespVO calculateGageRR(Long studyId) {
        // 1. 校验研究存在
        MsaStudyDO study = msaStudyMapper.selectById(studyId);
        if (study == null) {
            throw exception(MSA_STUDY_NOT_EXISTS);
        }
        // 2. 获取测量数据
        List<MsaMeasurementDO> measurements = msaMeasurementMapper.selectListByStudyId(studyId);
        if (measurements == null || measurements.isEmpty()) {
            throw exception(MSA_STUDY_DATA_NOT_ENOUGH);
        }

        // 3. 按评价人、零件分组计算
        int trialCount = study.getTrialCount() != null ? study.getTrialCount() : 0;
        int partCount = study.getPartCount() != null ? study.getPartCount() : 0;
        int appraiserCount = study.getAppraiserCount() != null ? study.getAppraiserCount() : 0;
        if (trialCount <= 0 || partCount <= 0 || appraiserCount <= 0) {
            throw exception(MSA_STUDY_DATA_NOT_ENOUGH);
        }

        // 4. 按 appraiserId -> partId -> [测量值] 分组
        Map<Long, Map<Long, List<BigDecimal>>> appraiserPartMap = new TreeMap<>();
        for (MsaMeasurementDO m : measurements) {
            if (m.getMeasurementValue() == null) {
                continue;
            }
            appraiserPartMap
                    .computeIfAbsent(m.getAppraiserId(), k -> new TreeMap<>())
                    .computeIfAbsent(m.getPartId(), k -> new ArrayList<>())
                    .add(m.getMeasurementValue());
        }

        // 5. 计算每个评价人每个零件的极差，并求 R̄（平均极差）
        List<BigDecimal> allRanges = new ArrayList<>();
        Map<Long, BigDecimal> appraiserAverage = new TreeMap<>();
        Map<Long, BigDecimal> partAverage = new TreeMap<>();
        int totalSumCount = 0;
        BigDecimal grandSum = BigDecimal.ZERO;

        for (Map.Entry<Long, Map<Long, List<BigDecimal>>> appraiserEntry : appraiserPartMap.entrySet()) {
            BigDecimal appraiserSum = BigDecimal.ZERO;
            int appraiserSumCount = 0;
            for (Map.Entry<Long, List<BigDecimal>> partEntry : appraiserEntry.getValue().entrySet()) {
                List<BigDecimal> trials = partEntry.getValue();
                if (trials.isEmpty()) {
                    continue;
                }
                BigDecimal max = trials.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                BigDecimal min = trials.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                BigDecimal range = max.subtract(min);
                allRanges.add(range);
                // 累加零件均值
                BigDecimal partSum = BigDecimal.ZERO;
                for (BigDecimal v : trials) {
                    partSum = partSum.add(v);
                }
                BigDecimal partAvg = partSum.divide(BigDecimal.valueOf(trials.size()), SCALE, RoundingMode.HALF_UP);
                partAverage.merge(partEntry.getKey(), partAvg, BigDecimal::add);
                appraiserSum = appraiserSum.add(partAvg);
                appraiserSumCount++;
                grandSum = grandSum.add(partSum);
                totalSumCount += trials.size();
            }
            appraiserAverage.put(appraiserEntry.getKey(),
                    appraiserSum.divide(BigDecimal.valueOf(appraiserSumCount), SCALE, RoundingMode.HALF_UP));
        }

        if (allRanges.isEmpty()) {
            throw exception(MSA_STUDY_DATA_NOT_ENOUGH);
        }

        // R̄（平均极差）
        BigDecimal rBarSum = BigDecimal.ZERO;
        for (BigDecimal r : allRanges) {
            rBarSum = rBarSum.add(r);
        }
        BigDecimal rBar = rBarSum.divide(BigDecimal.valueOf(allRanges.size()), SCALE, RoundingMode.HALF_UP);

        // 总均值 X̄
        BigDecimal grandAverage = grandSum.divide(BigDecimal.valueOf(totalSumCount), SCALE, RoundingMode.HALF_UP);

        // 6. K 系数（基于 d2 因子表）
        BigDecimal k1 = lookupD2Inverse(trialCount);          // 1/d2(trials)
        BigDecimal k2 = lookupD2Inverse(appraiserCount);       // 1/d2(appraisers)
        BigDecimal k3 = lookupD2Inverse(partCount);            // 1/d2(parts)

        // 7. EV（重复性）= R̄ × K1
        BigDecimal ev = rBar.multiply(k1).setScale(SCALE, RoundingMode.HALF_UP);

        // 8. AV（再现性）= sqrt((X̄_diff × K2)² − EV²/(parts×trials))
        BigDecimal xBarDiff = computeAppraiserRange(appraiserAverage);
        BigDecimal avTerm = xBarDiff.multiply(k2).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal avSq = avTerm.multiply(avTerm)
                .subtract(ev.multiply(ev).divide(BigDecimal.valueOf((long) partCount * trialCount), SCALE, RoundingMode.HALF_UP));
        BigDecimal av = avSq.compareTo(BigDecimal.ZERO) > 0
                ? BigDecimal.valueOf(Math.sqrt(avSq.doubleValue())).setScale(SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

        // 9. Gage R&R = sqrt(EV² + AV²)
        BigDecimal gageRR = BigDecimal.valueOf(Math.sqrt(ev.multiply(ev).add(av.multiply(av)).doubleValue()))
                .setScale(SCALE, RoundingMode.HALF_UP);

        // 10. PV（零件变异）= Rp × K3
        BigDecimal rp = computePartRange(partAverage);
        BigDecimal pv = rp.multiply(k3).setScale(SCALE, RoundingMode.HALF_UP);

        // 11. TV（总变异）= sqrt(Gage R&R² + PV²)
        BigDecimal tv = BigDecimal.valueOf(Math.sqrt(gageRR.multiply(gageRR).add(pv.multiply(pv)).doubleValue()))
                .setScale(SCALE, RoundingMode.HALF_UP);

        // 12. 百分比
        BigDecimal percentEV = tv.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : ev.divide(tv, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        BigDecimal percentAV = tv.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : av.divide(tv, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        BigDecimal percentGageRR = tv.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : gageRR.divide(tv, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        BigDecimal percentPV = tv.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : pv.divide(tv, SCALE, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        // 13. 评价结论
        String conclusion = evaluateConclusion(percentGageRR);

        // 14. 更新研究状态为已完成
        MsaStudyDO updateObj = new MsaStudyDO();
        updateObj.setId(studyId);
        updateObj.setStatus(MsaStatusEnum.COMPLETED.getStatus());
        msaStudyMapper.updateById(updateObj);

        // 15. 组装结果
        MsaGageRRRespVO resp = new MsaGageRRRespVO();
        resp.setStudyId(studyId);
        resp.setEv(ev);
        resp.setAv(av);
        resp.setGageRR(gageRR);
        resp.setPv(pv);
        resp.setTv(tv);
        resp.setPercentEV(percentEV);
        resp.setPercentAV(percentAV);
        resp.setPercentGageRR(percentGageRR);
        resp.setPercentPV(percentPV);
        resp.setConclusion(conclusion);
        resp.setSampleCount(measurements.size());
        return resp;
    }

    /**
     * 计算评价人均值的极差 X̄_diff
     */
    private BigDecimal computeAppraiserRange(Map<Long, BigDecimal> appraiserAverage) {
        if (appraiserAverage.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal max = Collections.max(appraiserAverage.values());
        BigDecimal min = Collections.min(appraiserAverage.values());
        return max.subtract(min);
    }

    /**
     * 计算零件均值的极差 Rp
     */
    private BigDecimal computePartRange(Map<Long, BigDecimal> partAverage) {
        if (partAverage.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal max = Collections.max(partAverage.values());
        BigDecimal min = Collections.min(partAverage.values());
        return max.subtract(min);
    }

    /**
     * d2 因子表的倒数（K1/K2/K3 = 1/d2）
     *
     * <p>d2 取决于子组大小 n（样本量）。常见取值：
     * <ul>
     *   <li>n=2: d2=1.128, 1/d2=0.8865</li>
     *   <li>n=3: d2=1.693, 1/d2=0.5907</li>
     *   <li>n=4: d2=2.059, 1/d2=0.4857</li>
     *   <li>n=5: d2=2.326, 1/d2=0.4299</li>
     *   <li>n=6: d2=2.534, 1/d2=0.3946</li>
     *   <li>n=7: d2=2.704, 1/d2=0.3698</li>
     *   <li>n=8: d2=2.847, 1/d2=0.3512</li>
     *   <li>n=9: d2=2.970, 1/d2=0.3367</li>
     *   <li>n=10: d2=3.078, 1/d2=0.3249</li>
     * </ul>
     */
    private BigDecimal lookupD2Inverse(int n) {
        double d2 = switch (n) {
            case 2 -> 1.128;
            case 3 -> 1.693;
            case 4 -> 2.059;
            case 5 -> 2.326;
            case 6 -> 2.534;
            case 7 -> 2.704;
            case 8 -> 2.847;
            case 9 -> 2.970;
            case 10 -> 3.078;
            default -> n > 10 ? 3.078 : 1.128;
        };
        return BigDecimal.valueOf(1.0 / d2).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 评价结论
     *
     * <p>%GR&R < 10% 可接受，10-30% 有条件接受，>30% 不可接受
     */
    private String evaluateConclusion(BigDecimal percentGageRR) {
        double v = percentGageRR.doubleValue();
        if (v < 10) {
            return "可接受";
        }
        if (v <= 30) {
            return "有条件接受";
        }
        return "不可接受";
    }

}
