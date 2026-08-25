package cn.zhicloud.module.mes.service.qc.oqc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.qc.oqc.vo.line.MesQcOqcLinePageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.qc.defectrecord.MesQcDefectRecordDO;
import cn.zhicloud.module.mes.dal.dataobject.qc.indicator.MesQcIndicatorDO;
import cn.zhicloud.module.mes.dal.dataobject.qc.oqc.MesQcOqcLineDO;
import cn.zhicloud.module.mes.dal.dataobject.qc.template.MesQcTemplateIndicatorDO;
import cn.zhicloud.module.mes.dal.mysql.qc.oqc.MesQcOqcLineMapper;
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
 * MES 出货检验单行 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class MesQcOqcLineServiceImpl implements MesQcOqcLineService {

    @Resource
    private MesQcOqcLineMapper oqcLineMapper;

    @Resource
    private MesQcIndicatorService indicatorService;
    @Resource
    private MesQcTemplateIndicatorService templateIndicatorService;

    @Override
    public MesQcOqcLineDO validateOqcLineExists(Long id) {
        MesQcOqcLineDO line = oqcLineMapper.selectById(id);
        if (line == null) {
            throw exception(QC_OQC_LINE_NOT_EXISTS);
        }
        return line;
    }

    @Override
    public MesQcOqcLineDO getOqcLine(Long id) {
        return oqcLineMapper.selectById(id);
    }

    @Override
    public PageResult<MesQcOqcLineDO> getOqcLinePage(MesQcOqcLinePageReqVO pageReqVO) {
        return oqcLineMapper.selectPage(pageReqVO);
    }

    @Override
    public void createLinesFromTemplate(Long oqcId, Long templateId) {
        List<MesQcTemplateIndicatorDO> templateIndicators = templateIndicatorService.getTemplateIndicatorListByTemplateId(templateId);
        if (CollUtil.isEmpty(templateIndicators)) {
            return;
        }
        Map<Long, MesQcIndicatorDO> indicatorMap = indicatorService.getIndicatorMap(
                convertSet(templateIndicators, MesQcTemplateIndicatorDO::getIndicatorId));
        List<MesQcOqcLineDO> lines = convertList(templateIndicators, templateIndicator -> {
            MesQcIndicatorDO indicator = indicatorMap.get(templateIndicator.getIndicatorId());
            return new MesQcOqcLineDO()
                    .setOqcId(oqcId).setIndicatorId(templateIndicator.getIndicatorId())
                    .setTool(indicator != null ? indicator.getTool() : null)
                    .setCheckMethod(templateIndicator.getCheckMethod())
                    .setStandardValue(templateIndicator.getStandardValue()).setUnitMeasureId(templateIndicator.getUnitMeasureId())
                    .setMaxThreshold(templateIndicator.getThresholdMax()).setMinThreshold(templateIndicator.getThresholdMin())
                    .setCriticalQuantity(0).setMajorQuantity(0).setMinorQuantity(0);
        });
        oqcLineMapper.insertBatch(lines);
    }

    @Override
    public void recalculateLineDefectStats(Long oqcId, List<MesQcDefectRecordDO> records) {
        List<MesQcOqcLineDO> lines = oqcLineMapper.selectListByOqcId(oqcId);
        if (CollUtil.isEmpty(lines)) {
            return;
        }
        // 按 lineId 分组统计，避免双层循环 O(lines × records)
        Map<Long, List<MesQcDefectRecordDO>> recordsByLineId = CollUtil.isEmpty(records)
                ? Collections.emptyMap()
                : records.stream()
                .filter(record -> record.getLineId() != null)
                .collect(Collectors.groupingBy(MesQcDefectRecordDO::getLineId));
        List<MesQcOqcLineDO> updateLines = new ArrayList<>(lines.size());
        for (MesQcOqcLineDO line : lines) {
            int critical = 0, major = 0, minor = 0;
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
            updateLines.add(new MesQcOqcLineDO().setId(line.getId())
                    .setCriticalQuantity(critical).setMajorQuantity(major).setMinorQuantity(minor));
        }
        oqcLineMapper.updateBatch(updateLines);
    }

    @Override
    public List<MesQcOqcLineDO> getOqcLineListByOqcId(Long oqcId) {
        return oqcLineMapper.selectListByOqcId(oqcId);
    }

    @Override
    public void deleteByOqcId(Long oqcId) {
        oqcLineMapper.deleteByOqcId(oqcId);
    }

    @Override
    public Long getOqcLineCountByUnitMeasureId(Long unitMeasureId) {
        return oqcLineMapper.selectCountByUnitMeasureId(unitMeasureId);
    }

}
