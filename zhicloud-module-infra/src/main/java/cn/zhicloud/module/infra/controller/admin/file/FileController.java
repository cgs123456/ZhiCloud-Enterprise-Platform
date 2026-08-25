package cn.zhicloud.module.infra.controller.admin.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.http.HttpUtils;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.tenant.core.aop.TenantIgnore;
import cn.zhicloud.module.infra.controller.admin.file.vo.file.*;
import cn.zhicloud.module.infra.dal.dataobject.file.FileDO;
import cn.zhicloud.module.infra.service.file.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.module.infra.framework.file.core.utils.FileTypeUtils.writeAttachment;

@Tag(name = "管理后台 - 文件存储")
@RestController
@RequestMapping("/infra/file")
@Validated
@Slf4j
public class FileController {

    /**
     * 允许上传的文件扩展名白名单（防止上传 webshell 等可执行文件）
     */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            // 图片
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "ico", "svg",
            // 文档
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "md",
            // 压缩包
            "zip", "rar", "7z", "gz", "tar",
            // 音视频
            "mp3", "mp4", "avi", "mov", "wmv", "flv", "wav",
            // 其他
            "json", "xml", "yaml", "yml"
    );

    @Resource
    private FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "模式一：后端上传文件")
    @Parameter(name = "file", description = "文件附件", required = true,
            schema = @Schema(type = "string", format = "binary"))
    @PreAuthorize("@ss.hasPermission('infra:file:upload')")
    public CommonResult<String> uploadFile(@Valid FileUploadReqVO uploadReqVO) throws Exception {
        MultipartFile file = uploadReqVO.getFile();
        // 校验文件扩展名白名单，防止上传 webshell 等可执行文件
        String originalFilename = file.getOriginalFilename();
        // 安全加固：原始文件名为空时直接拒绝（防止依赖客户端未提供文件名导致扩展名校验被绕过）
        if (StrUtil.isBlank(originalFilename)) {
            throw new IllegalArgumentException("原始文件名不能为空");
        }
        String extension = FileUtil.extName(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            return CommonResult.error(400, "不支持的文件类型：" + extension + "，仅允许：" + ALLOWED_EXTENSIONS);
        }
        byte[] content = IoUtil.readBytes(file.getInputStream());
        return success(fileService.createFile(content, originalFilename,
                uploadReqVO.getDirectory(), file.getContentType()));
    }

    @GetMapping("/presigned-url")
    @Operation(summary = "获取文件预签名地址（上传）", description = "模式二：前端上传文件：用于前端直接上传七牛、阿里云 OSS 等文件存储器")
    @Parameters({
            @Parameter(name = "name", description = "文件名称", required = true),
            @Parameter(name = "directory", description = "文件目录")
    })
    @PreAuthorize("@ss.hasPermission('infra:file:upload')")
    public CommonResult<FilePresignedUrlRespVO> getFilePresignedUrl(
            @RequestParam("name") String name,
            @RequestParam(value = "directory", required = false) String directory) {
        // 校验文件扩展名白名单
        String extension = FileUtil.extName(name);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            return CommonResult.error(400, "不支持的文件类型：" + extension + "，仅允许：" + ALLOWED_EXTENSIONS);
        }
        return success(fileService.presignPutUrl(name, directory));
    }

    @PostMapping("/create")
    @Operation(summary = "创建文件", description = "模式二：前端上传文件：配合 presigned-url 接口，记录上传了上传的文件")
    @PreAuthorize("@ss.hasPermission('infra:file:upload')")
    public CommonResult<Long> createFile(@Valid @RequestBody FileCreateReqVO createReqVO) {
        return success(fileService.createFile(createReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得文件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:query')")
    public CommonResult<FileRespVO> getFile(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(fileService.getFile(id), FileRespVO.class));
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除文件")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:delete')")
    public CommonResult<Boolean> deleteFile(@RequestParam("id") Long id) throws Exception {
        fileService.deleteFile(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除文件")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('infra:file:delete')")
    public CommonResult<Boolean> deleteFileList(@RequestParam("ids") List<Long> ids) throws Exception {
        fileService.deleteFileList(ids);
        return success(true);
    }

    @GetMapping("/{configId}/get/**")
    @PermitAll
    @TenantIgnore
    @Operation(summary = "下载文件")
    @Parameter(name = "configId", description = "配置编号", required = true)
    public void getFileContent(HttpServletRequest request,
                               HttpServletResponse response,
                               @PathVariable("configId") Long configId) throws Exception {
        // 获取请求的路径
        String path = StrUtil.subAfter(request.getRequestURI(), "/get/", false);
        if (StrUtil.isEmpty(path)) {
            throw new IllegalArgumentException("结尾的 path 路径必须传递");
        }
        // 解码，解决中文、%、+ 等特殊字符路径的问题
        // https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/807/
        // https://gitee.com/zhijiantianya/ruoyi-vue-pro/pulls/1432/
        path = HttpUtils.decodeUrlPath(path);

        // 读取内容
        byte[] content = fileService.getFileContent(configId, path);
        if (content == null) {
            log.warn("[getFileContent][configId({}) path({}) 文件不存在]", configId, path);
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return;
        }
        FileDO file = fileService.getFileByConfigIdAndPath(configId, path);
        String filename = file != null && StrUtil.isNotEmpty(file.getName()) ? file.getName() : FileUtil.getName(path);
        writeAttachment(response, filename, content);
    }

    @GetMapping("/page")
    @Operation(summary = "获得文件分页")
    @PreAuthorize("@ss.hasPermission('infra:file:query')")
    public CommonResult<PageResult<FileRespVO>> getFilePage(@Valid FilePageReqVO pageVO) {
        PageResult<FileDO> pageResult = fileService.getFilePage(pageVO);
        return success(BeanUtils.toBean(pageResult, FileRespVO.class));
    }

}
