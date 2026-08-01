package cn.iocoder.yudao.module.oa.controller.admin.announcement;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.oa.controller.admin.announcement.vo.OaAnnouncementPageReqVO;
import cn.iocoder.yudao.module.oa.controller.admin.announcement.vo.OaAnnouncementRespVO;
import cn.iocoder.yudao.module.oa.controller.admin.announcement.vo.OaAnnouncementSaveReqVO;
import cn.iocoder.yudao.module.oa.dal.dataobject.announcement.OaAnnouncementDO;
import cn.iocoder.yudao.module.oa.service.announcement.OaAnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - OA 公告管理")
@RestController
@RequestMapping("/oa/announcement")
@Validated
public class OaAnnouncementController {

    @Resource
    private OaAnnouncementService announcementService;

    @PostMapping("/create")
    @Operation(summary = "创建公告")
    @PreAuthorize("@ss.hasPermission('oa:announcement:create')")
    public CommonResult<Long> createAnnouncement(@Valid @RequestBody OaAnnouncementSaveReqVO createReqVO) {
        return success(announcementService.createAnnouncement(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新公告")
    @PreAuthorize("@ss.hasPermission('oa:announcement:update')")
    public CommonResult<Boolean> updateAnnouncement(@Valid @RequestBody OaAnnouncementSaveReqVO updateReqVO) {
        announcementService.updateAnnouncement(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除公告")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:announcement:delete')")
    public CommonResult<Boolean> deleteAnnouncement(@RequestParam("id") Long id) {
        announcementService.deleteAnnouncement(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得公告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('oa:announcement:query')")
    public CommonResult<OaAnnouncementRespVO> getAnnouncement(@RequestParam("id") Long id) {
        OaAnnouncementDO announcement = announcementService.getAnnouncement(id);
        return success(BeanUtils.toBean(announcement, OaAnnouncementRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得公告分页")
    @PreAuthorize("@ss.hasPermission('oa:announcement:query')")
    public CommonResult<PageResult<OaAnnouncementRespVO>> getAnnouncementPage(@Valid OaAnnouncementPageReqVO pageReqVO) {
        PageResult<OaAnnouncementDO> pageResult = announcementService.getAnnouncementPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, OaAnnouncementRespVO.class));
    }

    @PutMapping("/publish")
    @Operation(summary = "发布公告")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:announcement:update')")
    public CommonResult<Boolean> publishAnnouncement(@RequestParam("id") Long id) {
        announcementService.publishAnnouncement(id);
        return success(true);
    }

    @PutMapping("/take-down")
    @Operation(summary = "下架公告")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('oa:announcement:update')")
    public CommonResult<Boolean> takeDownAnnouncement(@RequestParam("id") Long id) {
        announcementService.takeDownAnnouncement(id);
        return success(true);
    }

    @GetMapping("/published-list")
    @Operation(summary = "获取已发布公告列表")
    @PreAuthorize("@ss.hasPermission('oa:announcement:query')")
    public CommonResult<List<OaAnnouncementRespVO>> getPublishedList() {
        List<OaAnnouncementDO> list = announcementService.getPublishedList();
        return success(BeanUtils.toBean(list, OaAnnouncementRespVO.class));
    }

}
