package cn.zhicloud.module.ai.controller.admin.nl2sql;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.ai.controller.admin.nl2sql.vo.Nl2SqlQueryReqVO;
import cn.zhicloud.module.ai.controller.admin.nl2sql.vo.Nl2SqlQueryRespVO;
import cn.zhicloud.module.ai.service.nl2sql.Nl2SqlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * AI NL2SQL 报表分析 Controller
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - AI NL2SQL 报表分析")
@RestController
@RequestMapping("/ai/nl2sql")
public class Nl2SqlController {

    @Resource
    private Nl2SqlService nl2SqlService;

    @PostMapping("/query")
    @Operation(summary = "自然语言查询报表数据")
    @PreAuthorize("@ss.hasPermission('ai:nl2sql:query')")
    public CommonResult<Nl2SqlQueryRespVO> query(@Valid @RequestBody Nl2SqlQueryReqVO reqVO) {
        return success(nl2SqlService.queryByNaturalLanguage(reqVO.getNaturalLanguage()));
    }

}
