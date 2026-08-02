package cn.iocoder.yudao.framework.excel.core.util;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.common.util.http.HttpUtils;
import cn.iocoder.yudao.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import cn.iocoder.yudao.framework.excel.core.handler.SelectSheetWriteHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Excel 工具类
 *
 * @author 芋道源码
 */
public class ExcelUtils {

    /**
     * 单次导出行数硬上限（与 {@code PageSizeNoneLimitInnerInterceptor#MAX_PAGE_SIZE_NONE} 保持一致）。
     * 超过此量时直接报错，避免超大列表在应用内存堆积导致 OOM；
     * 超大数据量导出应改用异步导出任务中心，而非一次性写出。
     */
    public static final int MAX_EXPORT_ROWS = 100_000;

    /**
     * 将列表以 Excel 响应给前端
     *
     * @param response  响应
     * @param filename  文件名
     * @param sheetName Excel sheet 名
     * @param head      Excel head 头
     * @param data      数据列表哦
     * @param <T>       泛型，保证 head 和 data 类型的一致性
     * @throws IOException 写入失败的情况
     */
    public static <T> void write(HttpServletResponse response, String filename, String sheetName,
                                 Class<T> head, List<T> data) throws IOException {
        // 安全护栏：防止超大列表导出导致 OOM。超过上限时明确报错，引导调用方缩小查询范围或改用异步导出。
        if (data != null && data.size() > MAX_EXPORT_ROWS) {
            throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(),
                    "导出数据量过大（上限 " + MAX_EXPORT_ROWS + " 行），请缩小查询范围或改用异步导出任务中心");
        }
        // 输出 Excel
        FastExcelFactory.write(response.getOutputStream(), head)
                .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy()) // 基于 column 长度，自动适配。最大 255 宽度
                .registerWriteHandler(new SelectSheetWriteHandler(head)) // 基于固定 sheet 实现下拉框
                .registerConverter(new LongStringConverter()) // 避免 Long 类型丢失精度
                .sheet(sheetName).doWrite(data);
        // 设置 header 和 contentType。写在最后的原因是，避免报错时，响应 contentType 已经被修改了
        response.addHeader("Content-Disposition", "attachment;filename=" + HttpUtils.encodeUtf8(filename));
        response.setContentType("application/vnd.ms-excel;charset=UTF-8");
    }

    public static <T> List<T> read(MultipartFile file, Class<T> head) throws IOException {
        // 参考 https://t.zsxq.com/zM77F 帖子，增加 try 处理，兼容 windows 场景
        try (InputStream inputStream = file.getInputStream()) {
            return FastExcelFactory.read(inputStream, head, null)
                    .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                    .doReadAllSync();
        }
    }

}
