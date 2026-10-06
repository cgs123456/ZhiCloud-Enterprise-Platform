package cn.zhicloud.server.controller.admin.mdm;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.mdm.api.MdmCrossRefPageReqVO;
import cn.zhicloud.framework.mdm.api.MdmCrossRefUpsertReq;
import cn.zhicloud.framework.mdm.dal.dataobject.MdmCrossRefDO;
import cn.zhicloud.framework.mdm.service.MdmCrossRefService;
import cn.zhicloud.server.controller.admin.mdm.vo.MdmCrossRefRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 主数据跨域对照（R-014）
 *
 * <p>V101 的对照表此前只有服务层（{@link MdmCrossRefService}），集成链路靠它解析 ERP↔WMS 等主数据，
 * 但**没有任何维护入口**：运营要登记/改/废一条映射只能手写 SQL（真机踩过：§12.139 补映射只能 INSERT）。
 * 本 Controller 补齐分页查询 + upsert 登记 + 失效三个运维端点，把「缺哪条映射」从查库变成可自查。
 *
 * <p>权限点是本次新建的（system:mdm-cross-ref:*），必须在 V142 种对应 system_menu 行——
 * 凭空造 permission 字符串会因无菜单行而恒 403（§12.95.3 的教训），种子缺失即等于接口交上去用不了。
 *
 * @author 智云集成治理
 */
@Tag(name = "管理后台 - 主数据跨域对照")
@RestController
@RequestMapping("/system/mdm-cross-ref")
@Validated
public class MdmCrossRefController {

    @Resource
    private MdmCrossRefService mdmCrossRefService;

    @GetMapping("/page")
    @Operation(summary = "获得主数据对照分页（按当前租户）")
    @PreAuthorize("@ss.hasPermission('system:mdm-cross-ref:query')")
    public CommonResult<PageResult<MdmCrossRefRespVO>> getCrossRefPage(@Valid MdmCrossRefPageReqVO pageReqVO) {
        PageResult<MdmCrossRefDO> pageResult = mdmCrossRefService.page(pageReqVO);
        return success(BeanUtils.toBean(pageResult, MdmCrossRefRespVO.class));
    }

    @PostMapping("/save")
    @Operation(summary = "登记/更新一条对照（幂等 upsert：同源键覆盖目标编号）")
    @PreAuthorize("@ss.hasPermission('system:mdm-cross-ref:create')")
    public CommonResult<Long> saveCrossRef(@Valid @RequestBody MdmCrossRefUpsertReq reqVO) {
        // upsert 语义：对照表表达「当前映射」，重复登记覆盖 targetId——所以 create 与 update 合为一个入口
        return success(mdmCrossRefService.upsert(reqVO));
    }

    @DeleteMapping("/invalidate")
    @Operation(summary = "失效一条对照（软删除，保留历史不再参与解析）")
    @Parameters({
            @Parameter(name = "sourceSystem", description = "源系统", required = true, example = "ERP"),
            @Parameter(name = "sourceEntity", description = "源实体类型", required = true, example = "PRODUCT"),
            @Parameter(name = "sourceId", description = "源编号", required = true, example = "100"),
            @Parameter(name = "targetSystem", description = "目标系统", required = true, example = "WMS"),
            @Parameter(name = "targetEntity", description = "目标实体类型", required = true, example = "SKU")
    })
    @PreAuthorize("@ss.hasPermission('system:mdm-cross-ref:delete')")
    public CommonResult<Boolean> invalidateCrossRef(@RequestParam("sourceSystem") String sourceSystem,
                                                    @RequestParam("sourceEntity") String sourceEntity,
                                                    @RequestParam("sourceId") String sourceId,
                                                    @RequestParam("targetSystem") String targetSystem,
                                                    @RequestParam("targetEntity") String targetEntity) {
        // 失效按完整源键 + 目标实体定位，绑定当前租户上下文（tenantId 传 null 由服务取上下文）
        return success(mdmCrossRefService.invalidate(null, sourceSystem, sourceEntity, sourceId,
                targetSystem, targetEntity));
    }

}
