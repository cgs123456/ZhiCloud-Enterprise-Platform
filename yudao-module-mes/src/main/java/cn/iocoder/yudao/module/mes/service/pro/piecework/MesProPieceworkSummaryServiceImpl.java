package cn.iocoder.yudao.module.mes.service.pro.piecework;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.piecework.MesProPieceworkRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.piecework.MesProPieceworkRecordMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PIECEWORK_SUMMARY_PERIOD_INVALID;

/**
 * MES 计件工资汇总 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class MesProPieceworkSummaryServiceImpl implements MesProPieceworkSummaryService {

    @Resource
    private MesProPieceworkRecordMapper pieceworkRecordMapper;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    public List<MesProPieceworkSummaryDTO> summaryByPeriod(String periodMonth) {
        validatePeriodMonth(periodMonth);
        List<MesProPieceworkRecordDO> records = pieceworkRecordMapper.selectListByPeriod(periodMonth);
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        // 1. 按员工分组聚合
        Map<Long, MesProPieceworkSummaryDTO> grouped = new LinkedHashMap<>();
        for (MesProPieceworkRecordDO r : records) {
            if (r.getFeedbackUserId() == null) {
                continue;
            }
            MesProPieceworkSummaryDTO dto = grouped.computeIfAbsent(r.getFeedbackUserId(), uid -> {
                MesProPieceworkSummaryDTO d = new MesProPieceworkSummaryDTO();
                d.setEmployeeId(uid);
                d.setPeriodMonth(periodMonth);
                d.setTotalQualifiedQty(BigDecimal.ZERO);
                d.setTotalScrapQty(BigDecimal.ZERO);
                d.setTotalAmount(BigDecimal.ZERO);
                return d;
            });
            dto.setTotalQualifiedQty(dto.getTotalQualifiedQty().add(nullToZero(r.getQualifiedQty())));
            dto.setTotalScrapQty(dto.getTotalScrapQty().add(nullToZero(r.getScrapQty())));
            dto.setTotalAmount(dto.getTotalAmount().add(nullToZero(r.getTotalAmount())));
        }
        // 2. 批量获取员工姓名
        Set<Long> userIds = grouped.keySet();
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        List<MesProPieceworkSummaryDTO> result = new ArrayList<>(grouped.values());
        for (MesProPieceworkSummaryDTO dto : result) {
            AdminUserRespDTO user = userMap.get(dto.getEmployeeId());
            if (user != null) {
                dto.setEmployeeName(user.getNickname());
            }
        }
        // 3. 按员工编号升序
        result.sort((a, b) -> Long.compare(a.getEmployeeId(), b.getEmployeeId()));
        return result;
    }

    @Override
    public MesProPieceworkSummaryDTO summaryByUserAndPeriod(Long feedbackUserId, String periodMonth) {
        validatePeriodMonth(periodMonth);
        if (feedbackUserId == null) {
            return null;
        }
        List<MesProPieceworkRecordDO> records = pieceworkRecordMapper.selectListByUserAndPeriod(feedbackUserId, periodMonth);
        if (CollUtil.isEmpty(records)) {
            return null;
        }
        MesProPieceworkSummaryDTO dto = new MesProPieceworkSummaryDTO();
        dto.setEmployeeId(feedbackUserId);
        dto.setPeriodMonth(periodMonth);
        BigDecimal totalQualified = BigDecimal.ZERO;
        BigDecimal totalScrap = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (MesProPieceworkRecordDO r : records) {
            totalQualified = totalQualified.add(nullToZero(r.getQualifiedQty()));
            totalScrap = totalScrap.add(nullToZero(r.getScrapQty()));
            totalAmount = totalAmount.add(nullToZero(r.getTotalAmount()));
        }
        dto.setTotalQualifiedQty(totalQualified);
        dto.setTotalScrapQty(totalScrap);
        dto.setTotalAmount(totalAmount);
        // 员工姓名
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(Collections.singleton(feedbackUserId));
        AdminUserRespDTO user = userMap.get(feedbackUserId);
        if (user != null) {
            dto.setEmployeeName(user.getNickname());
        }
        return dto;
    }

    private void validatePeriodMonth(String periodMonth) {
        if (periodMonth == null || periodMonth.length() != 6) {
            throw exception(PRO_PIECEWORK_SUMMARY_PERIOD_INVALID);
        }
        try {
            Integer.parseInt(periodMonth);
        } catch (NumberFormatException e) {
            throw exception(PRO_PIECEWORK_SUMMARY_PERIOD_INVALID);
        }
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

}
