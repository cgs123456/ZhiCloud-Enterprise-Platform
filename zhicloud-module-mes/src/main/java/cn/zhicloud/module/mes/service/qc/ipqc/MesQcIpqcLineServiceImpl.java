package cn.zhicloud.module.mes.service.qc.ipqc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.qc.ipqc.vo.line.MesQcIpqcLinePageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.qc.defectrecord.MesQcDefectRecordDO;
import cn.zhicloud.module.mes.dal.dataobject.qc.indicator.MesQcIndicatorDO;
import cn.zhicloud.module.mes.dal.dataobject.qc.ipqc.MesQcIpqcLineDO;
import cn.zhicloud.module.mes.dal.dataobject.qc.template.MesQcTemplateIndicatorDO;
import cn.zhicloud.module.mes.dal.mysql.qc.ipqc.MesQcIpqcLineMapper;
import cn.zhicloud.module.mes.enums.qc.MesQcDefectLevelEnum;
import cn.zhicloud.module.mes.service.qc.indicator.MesQcIndicatorService;
import cn.zhicloud.module.mes.service.qc.template.MesQcTemplateIndicatorService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertList;
import static cn.zhicloud.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.*;

/**
 * MES 过程检验单行 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class MesQcIpqcLineServiceImpl implements MesQcIpqcLineService {

    @Resource
    private MesQcIpqcLineMapper ipqcLineMapper;

    @Resource
    private MesQcIndicatorService indicatorService;
    @Resource
    private MesQcTemplateIndicatorService templateIndicatorService;

    @Override
    public MesQcIpqcLineDO validateIpqcLineExists(Long id) {
        MesQcIpqcLineDO line = ipqcLineMapper.selectById(id);
        if (line == null) {
            throw exception(QC_IPQC_LINE_NOT_EXISTS);
        }
        return line;
    }

    @Override
    public MesQcIpqcLineDO getIpqcLine(Long id) {
        return ipqcLineMapper.selectById(id);
    }

    @Override
    public PageResult<MesQcIpqcLineDO> getIpqcLinePage(MesQcIpqcLinePageReqVO pageReqVO) {
        return ipqcLineMapper.selectPage(pageReqVO);
    }

    @Override
    public void createLinesFromTemplate(Long ipqcId, Long templateId) {
        List<MesQcTemplateIndicatorDO> templateIndicators = templateIndicatorService.getTemplateIndicatorListByTemplateId(templateId);
        if (CollUtil.isEmpty(templateIndicators)) {
            return;
        }
        Map<Long, MesQcIndicatorDO> indicatorMap = indicatorService.getIndicatorMap(
                convertSet(templateIndicators, MesQcTemplateIndicatorDO::getIndicatorId));
        List<MesQcIpqcLineDO> lines = convertList(templateIndicators, templateIndicator -> {
            MesQcIndicatorDO indicator = indicatorMap.get(templateIndicator.getIndicatorId());
            return new MesQcIpqcLineDO()
                    .setIpqcId(ipqcId).setIndicatorId(templateIndicator.getIndicatorId())
                    .setTool(indicator != null ? indicator.getTool() : null)
                    .setCheckMethod(templateIndicator.getCheckMethod())
                    .setStandardValue(templateIndicator.getStandardValue()).setUnitMeasureId(templateIndicator.getUnitMeasureId())
                    .setMaxThreshold(templateIndicator.getThresholdMax()).setMinThreshold(templateIndicator.getThresholdMin())
                    .setCriticalQuantity(0).setMajorQuantity(0).setMinorQuantity(0);
        });
        ipqcLineMapper.insertBatch(lines);
    }

    @Override
    public void recalculateLineDefectStats(Long ipqcId, List<MesQcDefectRecordDO> records) {
        List<MesQcIpqcLineDO> lines = ipqcLineMapper.selectListByIpqcId(ipqcId);
        if (CollUtil.isEmpty(lines)) {
            return;
        }
        // 按 lineId 分组统计，避免双层循环 O(lines × records)
        Map<Long, List<MesQcDefectRecordDO>> recordsByLineId = CollUtil.isEmpty(records)
                ? Collections.emptyMap()
                : records.stream()
                .filter(record -> record.getLineId() != null)
                .collect(Collectors.groupingBy(MesQcDefectRecordDO::getLineId));
        List<MesQcIpqcLineDO> updateLines = new ArrayList<>(lines.size());
        for (MesQcIpqcLineDO line : lines) {
            int critical = 0;
            int major = 0;
            int minor = 0;
            for (MesQcDefectRecordDO record : recordsByLineId.getOrDefault(line.getId(), Collections.emptyList())) {
                int quantity = ObjUtil.defaultIfNull(record.getQuantity(), 1);
                if (Objects.equals(record.getLevel(), MesQcDefectLevelEnum.CRITICAL.getType())) {
                    critical += quantity;
                } else if (Objects.equals(record.getLevel(), MesQcDefectLevelEnum.MAJOR.getType())) {
                    major += quantity;
                } else if (Objects.equals(record.getLevel(), MesQcDefectLevelEnum.MINOR.getType())) {
                    minor += quantity;
                } else {
                    throw exception(QC_DEFECT_RECORD_LEVEL_UNKNOWN);
                }
            }
            updateLines.add(new MesQcIpqcLineDO().setId(line.getId())
                    .setCriticalQuantity(critical).setMajorQuantity(major).setMinorQuantity(minor));
        }
        ipqcLineMapper.updateBatch(updateLines);
    }

    @Override
    public List<MesQcIpqcLineDO> getIpqcLineListByIpqcId(Long ipqcId) {
        return ipqcLineMapper.selectListByIpqcId(ipqcId);
    }

    @Override
    public void deleteListByIpqcId(Long ipqcId) {
        ipqcLineMapper.deleteByIpqcId(ipqcId);
    }

    @Override
    public Long getIpqcLineCountByUnitMeasureId(Long unitMeasureId) {
        return ipqcLineMapper.selectCountByUnitMeasureId(unitMeasureId);
    }

}
