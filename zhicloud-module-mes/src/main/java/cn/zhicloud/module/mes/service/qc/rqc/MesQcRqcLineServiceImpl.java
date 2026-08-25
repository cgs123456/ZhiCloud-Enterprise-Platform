package cn.zhicloud.module.mes.service.qc.rqc;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.qc.rqc.vo.line.MesQcRqcLinePageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.qc.defectrecord.MesQcDefectRecordDO;
import cn.zhicloud.module.mes.dal.dataobject.qc.indicator.MesQcIndicatorDO;
import cn.zhicloud.module.mes.dal.dataobject.qc.rqc.MesQcRqcLineDO;
import cn.zhicloud.module.mes.dal.dataobject.qc.template.MesQcTemplateIndicatorDO;
import cn.zhicloud.module.mes.dal.mysql.qc.rqc.MesQcRqcLineMapper;
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
 * MES 退货检验行 Service 实现类
 *
 * @author 智云
 */
@Service
@Validated
public class MesQcRqcLineServiceImpl implements MesQcRqcLineService {

    @Resource
    private MesQcRqcLineMapper rqcLineMapper;

    @Resource
    private MesQcIndicatorService indicatorService;
    @Resource
    private MesQcTemplateIndicatorService templateIndicatorService;

    @Override
    public MesQcRqcLineDO validateRqcLineExists(Long id) {
        MesQcRqcLineDO line = rqcLineMapper.selectById(id);
        if (line == null) {
            throw exception(QC_RQC_LINE_NOT_EXISTS);
        }
        return line;
    }

    @Override
    public MesQcRqcLineDO getRqcLine(Long id) {
        return rqcLineMapper.selectById(id);
    }

    @Override
    public PageResult<MesQcRqcLineDO> getRqcLinePage(MesQcRqcLinePageReqVO pageReqVO) {
        return rqcLineMapper.selectPage(pageReqVO);
    }

    @Override
    public void createLinesFromTemplate(Long rqcId, Long templateId) {
        List<MesQcTemplateIndicatorDO> templateIndicators = templateIndicatorService.getTemplateIndicatorListByTemplateId(templateId);
        if (CollUtil.isEmpty(templateIndicators)) {
            return;
        }
        Map<Long, MesQcIndicatorDO> indicatorMap = indicatorService.getIndicatorMap(
                convertSet(templateIndicators, MesQcTemplateIndicatorDO::getIndicatorId));
        List<MesQcRqcLineDO> lines = convertList(templateIndicators, templateIndicator -> {
            MesQcIndicatorDO indicator = indicatorMap.get(templateIndicator.getIndicatorId());
            return new MesQcRqcLineDO()
                    .setRqcId(rqcId).setIndicatorId(templateIndicator.getIndicatorId())
                    .setTool(indicator != null ? indicator.getTool() : null)
                    .setCheckMethod(templateIndicator.getCheckMethod())
                    .setStandardValue(templateIndicator.getStandardValue()).setUnitMeasureId(templateIndicator.getUnitMeasureId())
                    .setMaxThreshold(templateIndicator.getThresholdMax()).setMinThreshold(templateIndicator.getThresholdMin())
                    .setCriticalQuantity(0).setMajorQuantity(0).setMinorQuantity(0);
        });
        rqcLineMapper.insertBatch(lines);
    }

    @Override
    public void recalculateLineDefectStats(Long rqcId, List<MesQcDefectRecordDO> records) {
        List<MesQcRqcLineDO> lines = rqcLineMapper.selectListByRqcId(rqcId);
        if (CollUtil.isEmpty(lines)) {
            return;
        }
        // 按 lineId 分组统计，避免双层循环 O(lines × records)
        Map<Long, List<MesQcDefectRecordDO>> recordsByLineId = CollUtil.isEmpty(records)
                ? Collections.emptyMap()
                : records.stream()
                .filter(record -> record.getLineId() != null)
                .collect(Collectors.groupingBy(MesQcDefectRecordDO::getLineId));
        List<MesQcRqcLineDO> updateLines = new ArrayList<>(lines.size());
        for (MesQcRqcLineDO line : lines) {
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
            updateLines.add(new MesQcRqcLineDO().setId(line.getId())
                    .setCriticalQuantity(critical).setMajorQuantity(major).setMinorQuantity(minor));
        }
        rqcLineMapper.updateBatch(updateLines);
    }

    @Override
    public List<MesQcRqcLineDO> getRqcLineListByRqcId(Long rqcId) {
        return rqcLineMapper.selectListByRqcId(rqcId);
    }

    @Override
    public void deleteByRqcId(Long rqcId) {
        rqcLineMapper.deleteByRqcId(rqcId);
    }

    @Override
    public Long getRqcLineCountByUnitMeasureId(Long unitMeasureId) {
        return rqcLineMapper.selectCountByUnitMeasureId(unitMeasureId);
    }

}
