package cn.iocoder.yudao.module.qms.service.spc;

import cn.iocoder.yudao.module.qms.controller.admin.spc.vo.SamplingPlanRespVO;
import cn.iocoder.yudao.module.qms.controller.admin.spc.vo.SpcAnalysisRespVO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionitem.InspectionItemDO;
import cn.iocoder.yudao.module.qms.dal.dataobject.inspectionrecord.InspectionRecordDO;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionitem.InspectionItemMapper;
import cn.iocoder.yudao.module.qms.dal.mysql.inspectionrecord.InspectionRecordMapper;
import cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.qms.enums.qms.SpcChartTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * SPC 统计过程控制 Service 实现类
 *
 * <p>实现：
 * <ul>
 *   <li>{@link #analyze(Long)}：从 qms_inspection_record 按 itemId 聚合，计算 Cp/Cpk/控制限</li>
 *   <li>{@link #getSamplingPlan(Long, String, BigDecimal)}：MIL-STD-105E 字码表 + 主表查询</li>
 * </ul>
 *
 * @author yudao
 */
@Service
@Validated
@Slf4j
public class SpcServiceImpl implements SpcService {

    private static final int SCALE = 4;
    private static final BigDecimal THREE = new BigDecimal("3");

    @Resource
    private InspectionItemMapper inspectionItemMapper;

    @Resource
    private InspectionRecordMapper inspectionRecordMapper;

    @Override
    public SpcAnalysisRespVO analyze(Long itemId) {
        // 1. 校验检验项目存在
        InspectionItemDO item = inspectionItemMapper.selectById(itemId);
        if (item == null) {
            throw exception(ErrorCodeConstants.INSPECTION_ITEM_NOT_EXISTS);
        }

        // 2. 拉取该项目下所有检验记录的 measuredValue（按 create_time 倒序）
        List<InspectionRecordDO> records = inspectionRecordMapper.selectListByItemId(itemId);
        List<BigDecimal> samples = new ArrayList<>();
        if (records != null) {
            for (InspectionRecordDO r : records) {
                if (r.getItemId() != null && r.getItemId().equals(itemId) && r.getMeasuredValue() != null) {
                    try {
                        BigDecimal v = new BigDecimal(r.getMeasuredValue().trim());
                        samples.add(v);
                    } catch (NumberFormatException ex) {
                        // 非数值型实测值跳过（如合格/不合格文本结果）
                    }
                }
            }
        }

        SpcAnalysisRespVO resp = new SpcAnalysisRespVO();
        resp.setItemId(itemId);
        resp.setSamples(samples);
        resp.setSampleCount(samples.size());

        // 3. 样本不足，无法计算
        if (samples.size() < 2) {
            resp.setMean(BigDecimal.ZERO);
            resp.setStdDev(BigDecimal.ZERO);
            return resp;
        }

        // 4. 计算均值
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal v : samples) {
            sum = sum.add(v);
        }
        BigDecimal mean = sum.divide(BigDecimal.valueOf(samples.size()), SCALE, RoundingMode.HALF_UP);
        resp.setMean(mean);

        // 5. 计算样本标准差（Bessel 校正，n-1）
        BigDecimal sqSum = BigDecimal.ZERO;
        for (BigDecimal v : samples) {
            BigDecimal diff = v.subtract(mean);
            sqSum = sqSum.add(diff.multiply(diff));
        }
        BigDecimal variance = sqSum.divide(BigDecimal.valueOf(samples.size() - 1L), SCALE, RoundingMode.HALF_UP);
        double stdDouble = Math.sqrt(variance.doubleValue());
        BigDecimal stdDev = BigDecimal.valueOf(stdDouble).setScale(SCALE, RoundingMode.HALF_UP);
        resp.setStdDev(stdDev);

        // 6. 控制限 UCL/LCL = mean ± 3σ
        BigDecimal threeSigma = stdDev.multiply(THREE);
        resp.setUpperControlLimit(mean.add(threeSigma).setScale(SCALE, RoundingMode.HALF_UP));
        resp.setLowerControlLimit(mean.subtract(threeSigma).setScale(SCALE, RoundingMode.HALF_UP));

        // 7. 统计超限样本数
        int outOfControl = 0;
        for (BigDecimal v : samples) {
            if (v.compareTo(resp.getUpperControlLimit()) > 0 || v.compareTo(resp.getLowerControlLimit()) < 0) {
                outOfControl++;
            }
        }
        resp.setOutOfControlCount(outOfControl);

        // P0-9：Western Electric 8 规则检测
        resp.setRuleViolations(WesternElectricRuleDetector.detect(samples, mean, stdDev));

        // 8. 规格限与目标值（从检验项目读取）
        resp.setUpperSpecLimit(item.getUpperLimit());
        resp.setLowerSpecLimit(item.getLowerLimit());
        BigDecimal target = null;
        if (item.getTarget() != null) {
            try {
                target = new BigDecimal(item.getTarget().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        resp.setTarget(target);

        // 9. Cp / Cpk（需要同时有 USL / LSL 且 stdDev > 0）
        if (item.getUpperLimit() != null && item.getLowerLimit() != null && stdDouble > 0) {
            BigDecimal usl = item.getUpperLimit();
            BigDecimal lsl = item.getLowerLimit();
            BigDecimal t = usl.subtract(lsl);
            BigDecimal sixSigma = stdDev.multiply(new BigDecimal("6"));
            BigDecimal cp = t.divide(sixSigma, SCALE, RoundingMode.HALF_UP);
            resp.setCp(cp);

            BigDecimal cpu = usl.subtract(mean).divide(stdDev.multiply(THREE), SCALE, RoundingMode.HALF_UP);
            BigDecimal cpl = mean.subtract(lsl).divide(stdDev.multiply(THREE), SCALE, RoundingMode.HALF_UP);
            BigDecimal cpk = cpu.min(cpl);
            resp.setCpk(cpk);

            // 10. 工序能力评价
            resp.setCapabilityLevel(evaluateCapability(cpk));
        } else {
            resp.setCapabilityLevel("规格限不完整，无法计算");
        }

        // P0-9：默认按单值-移动极差图（I-MR）处理，因 qms_inspection_record 每条记录即为一次测量值
        resp.setChartType(SpcChartTypeEnum.I_MR.getCode());

        return resp;
    }

    @Override
    public SamplingPlanRespVO getSamplingPlan(Long lotSize, String inspectionLevel, BigDecimal aql) {
        if (lotSize == null || lotSize <= 0) {
            throw exception(ErrorCodeConstants.SPC_LOT_SIZE_INVALID);
        }
        String level = (inspectionLevel == null || inspectionLevel.isEmpty()) ? "II" : inspectionLevel;
        BigDecimal aqlValue = (aql == null) ? BigDecimal.ONE : aql;

        // 1. 查字码
        String codeLetter = lookupCodeLetter(lotSize, level);
        // 2. 查样本量
        int sampleSize = lookupSampleSize(codeLetter);
        // 3. 查 Ac/Re
        int[] normal = lookupAcRe(codeLetter, aqlValue, false);
        int[] tightened = lookupAcRe(codeLetter, aqlValue, true);
        int[] reduced = lookupAcRe(codeLetter, aqlValue.multiply(new BigDecimal("1.5")), false);

        SamplingPlanRespVO resp = new SamplingPlanRespVO();
        resp.setLotSizeFrom(lotSize);
        resp.setLotSizeTo(lotSize);
        resp.setCodeLetter(codeLetter);
        resp.setInspectionLevel(level);
        resp.setSampleSize(sampleSize);
        resp.setAql(aqlValue);
        resp.setNormalAccept(normal[0]);
        resp.setNormalReject(normal[1]);
        resp.setTightenedAccept(tightened[0]);
        resp.setTightenedReject(tightened[1]);
        resp.setReducedAccept(reduced[0]);
        resp.setReducedReject(reduced[1]);
        return resp;
    }

    /**
     * 工序能力评价
     */
    private String evaluateCapability(BigDecimal cpk) {
        if (cpk == null) {
            return "无法计算";
        }
        double v = cpk.doubleValue();
        if (v >= 1.67) {
            return "优秀";
        }
        if (v >= 1.33) {
            return "充足";
        }
        if (v >= 1.0) {
            return "勉强";
        }
        return "不足";
    }

    /**
     * MIL-STD-105E 字码表（一般检验水平 II 简化版，覆盖常用批量范围）
     */
    private String lookupCodeLetter(Long lotSize, String level) {
        // 简化：基于一般检验水平 II，覆盖 2~50000+ 批量
        long n = lotSize;
        if ("I".equalsIgnoreCase(level)) {
            if (n <= 50) return "A";
            if (n <= 500) return "E";
            if (n <= 35000) return "K";
            return "M";
        }
        if ("III".equalsIgnoreCase(level)) {
            if (n <= 50) return "C";
            if (n <= 500) return "H";
            if (n <= 35000) return "N";
            return "P";
        }
        // II 默认
        if (n <= 8) return "A";
        if (n <= 15) return "B";
        if (n <= 25) return "C";
        if (n <= 50) return "D";
        if (n <= 90) return "E";
        if (n <= 150) return "F";
        if (n <= 280) return "G";
        if (n <= 500) return "H";
        if (n <= 1200) return "J";
        if (n <= 3200) return "K";
        if (n <= 10000) return "L";
        if (n <= 35000) return "M";
        if (n <= 150000) return "N";
        if (n <= 500000) return "P";
        return "Q";
    }

    /**
     * 字码对应的样本量
     */
    private int lookupSampleSize(String codeLetter) {
        switch (codeLetter) {
            case "A": return 2;
            case "B": return 3;
            case "C": return 5;
            case "D": return 8;
            case "E": return 13;
            case "F": return 20;
            case "G": return 32;
            case "H": return 50;
            case "J": return 80;
            case "K": return 125;
            case "L": return 200;
            case "M": return 315;
            case "N": return 500;
            case "P": return 800;
            case "Q": return 1250;
            case "R": return 2000;
            default: return 0;
        }
    }

    /**
     * 简化的 Ac/Re 查询（AQL=1.0 单点抽样方案）
     *
     * <p>真实 MIL-STD-105E 主表为二维表，此处按 AQL=1.0 的简化值返回，
     * 业务方可在此基础上扩展为完整 AQL 表。返回 [Ac, Re]。
     */
    private int[] lookupAcRe(String codeLetter, BigDecimal aql, boolean tightened) {
        // 简化：以 AQL=1.0 为基准
        int ac;
        int re;
        switch (codeLetter) {
            case "A":
            case "B":
                ac = 0; re = 1; break;
            case "C":
                ac = tightened ? 0 : 0; re = 1; break;
            case "D":
                ac = tightened ? 0 : 1; re = tightened ? 1 : 2; break;
            case "E":
            case "F":
                ac = tightened ? 1 : 2; re = tightened ? 2 : 3; break;
            case "G":
            case "H":
                ac = tightened ? 2 : 3; re = tightened ? 3 : 4; break;
            case "J":
                ac = tightened ? 3 : 5; re = tightened ? 4 : 6; break;
            case "K":
                ac = tightened ? 5 : 7; re = tightened ? 6 : 8; break;
            case "L":
                ac = tightened ? 7 : 10; re = tightened ? 8 : 11; break;
            case "M":
                ac = tightened ? 10 : 14; re = tightened ? 11 : 15; break;
            case "N":
                ac = tightened ? 14 : 21; re = tightened ? 15 : 22; break;
            case "P":
                ac = tightened ? 21 : 30; re = tightened ? 22 : 31; break;
            default:
                ac = 0; re = 1;
        }
        return new int[]{ac, re};
    }

    // 静态占位（保留扩展用，未来可填充完整 MIL-STD-105E 主表）
    @SuppressWarnings("unused")
    private static final java.util.Map<String, String> MIL_STD_105E_NOTE =
            java.util.Map.of("note", "MIL-STD-105E simplified table, AQL=1.0 baseline");

}
