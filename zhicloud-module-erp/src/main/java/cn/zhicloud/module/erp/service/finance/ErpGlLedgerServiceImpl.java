package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerDetailExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerDetailRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerGeneralExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerGeneralRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerMultiColumnExcelDataVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerMultiColumnRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectBalanceExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectBalanceRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

/**
 * ERP 账表 Service 实现（账套口径，金额式四族，ADR 266）
 *
 * <p>门面职责：编排「聚合引擎（{@link ErpGlLedgerQueryEngine}）→ 行装配（{@link ErpGlLedgerRowAssembler}）」，
 * 余额口径与行模型的口径说明分别落在上下文类与装配器 javadoc。
 *
 * <p>一期金额式：数量与辅助核算四族不建（分录层无该两类事实，ADR 266 §2.2）。
 *
 * @author 智云
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Validated
public class ErpGlLedgerServiceImpl implements ErpGlLedgerService {

    private final ErpGlLedgerQueryEngine engine;
    private final ErpGlLedgerRowAssembler assembler;

    @Override
    public List<ErpGlLedgerDetailRespVO> getDetailList(ErpGlLedgerReqVO reqVO) {
        ErpGlLedgerQueryContext ctx = engine.buildContext(reqVO);
        ErpGlAccountDO target = ctx == null ? null : ctx.accountMap.get(reqVO.getSubjectId());
        return target == null ? Collections.emptyList() : assembler.detailRows(ctx, target);
    }

    @Override
    public List<ErpGlLedgerGeneralRespVO> getGeneralList(ErpGlLedgerReqVO reqVO) {
        ErpGlLedgerQueryContext ctx = engine.buildContext(reqVO);
        return ctx == null ? Collections.emptyList() : assembler.generalRows(ctx, reqVO);
    }

    @Override
    public List<ErpGlLedgerSubjectBalanceRespVO> getSubjectBalanceList(ErpGlLedgerReqVO reqVO) {
        ErpGlLedgerQueryContext ctx = engine.buildContext(reqVO);
        return ctx == null ? Collections.emptyList() : assembler.subjectBalanceTree(ctx, reqVO);
    }

    @Override
    public ErpGlLedgerMultiColumnRespVO getMultiColumn(ErpGlLedgerReqVO reqVO) {
        ErpGlLedgerMultiColumnRespVO result = new ErpGlLedgerMultiColumnRespVO();
        ErpGlLedgerQueryContext ctx = engine.buildContext(reqVO);
        ErpGlAccountDO target = ctx == null ? null : ctx.accountMap.get(reqVO.getSubjectId());
        if (target != null) {
            result = assembler.multiColumn(ctx, target);
        }
        return result;
    }

    @Override
    public List<ErpGlLedgerSubjectRespVO> getDetailSubjectList(ErpGlLedgerReqVO reqVO) {
        ErpGlLedgerQueryContext ctx = engine.buildContext(reqVO);
        return ctx == null ? Collections.emptyList() : assembler.detailSubjects(ctx);
    }

    @Override
    public List<ErpGlLedgerDetailExcelVO> getDetailExcelList(ErpGlLedgerReqVO reqVO) {
        ErpGlLedgerQueryContext ctx = engine.buildContext(reqVO);
        ErpGlAccountDO target = ctx == null ? null : ctx.accountMap.get(reqVO.getSubjectId());
        return target == null ? Collections.emptyList() : assembler.detailExcelList(ctx, target);
    }

    @Override
    public List<ErpGlLedgerGeneralExcelVO> getGeneralExcelList(ErpGlLedgerReqVO reqVO) {
        ErpGlLedgerQueryContext ctx = engine.buildContext(reqVO);
        return ctx == null ? Collections.emptyList() : assembler.generalExcelList(ctx, reqVO);
    }

    @Override
    public List<ErpGlLedgerSubjectBalanceExcelVO> getSubjectBalanceExcelList(ErpGlLedgerReqVO reqVO) {
        return assembler.subjectBalanceExcelList(getSubjectBalanceList(reqVO));
    }

    @Override
    public ErpGlLedgerMultiColumnExcelDataVO getMultiColumnExcelData(ErpGlLedgerReqVO reqVO) {
        return assembler.multiColumnExcelData(getMultiColumn(reqVO));
    }

}
