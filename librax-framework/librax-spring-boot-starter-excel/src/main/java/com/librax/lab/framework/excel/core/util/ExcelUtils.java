package com.librax.lab.framework.excel.core.util;

import cn.idev.excel.FastExcelFactory;
import cn.idev.excel.converters.longconverter.LongStringConverter;
import com.librax.lab.framework.common.util.http.HttpUtils;
import com.librax.lab.framework.excel.core.handler.ColumnWidthMatchStyleStrategy;
import com.librax.lab.framework.excel.core.handler.SelectSheetWriteHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/**
 * Excel 工具类
 *
 * @author 一南
 */
public class ExcelUtils {

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
        // 先写入内存：FastExcel 默认生成 xlsx（zip），写出时会 flush。
        // 若直接写 response 再设置 header，响应已提交，Content-Type / Content-Disposition 会被丢弃，
        // 浏览器只能按 URL（export-excel）把二进制当成 xml 打开。
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FastExcelFactory.write(outputStream, head)
                .autoCloseStream(false) // 不要自动关闭，交给 Servlet 自己处理
                .registerWriteHandler(new ColumnWidthMatchStyleStrategy()) // 基于 column 长度，自动适配。最大 255 宽度
                .registerWriteHandler(new SelectSheetWriteHandler(head)) // 基于固定 sheet 实现下拉框
                .registerConverter(new LongStringConverter()) // 避免 Long 类型丢失精度
                .sheet(sheetName).doWrite(data);
        // 生成成功后再设置响应头。失败时 contentType 仍是 JSON，全局异常处理可以正常返回。
        filename = ensureXlsxFilename(filename);
        // 只用 filename= 百分号编码。filename*=UTF-8'' 会被 Knife4j 原样拼进文件名，变成 utf-8''用户数据.xlsx
        String encodedFilename = HttpUtils.encodeUtf8(filename).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + encodedFilename);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // 全局 force encoding 会给 Content-Type 追加 charset，客户端会把 xlsx 当文本解析
        response.setCharacterEncoding(null);
        response.setContentLength(outputStream.size());
        outputStream.writeTo(response.getOutputStream());
    }

    /**
     * FastExcel 写出的是 xlsx。调用方仍传 .xls 时改成 .xlsx，避免扩展名和内容不一致。
     */
    private static String ensureXlsxFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "export.xlsx";
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx")) {
            return filename;
        }
        if (lower.endsWith(".xls")) {
            return filename.substring(0, filename.length() - 4) + ".xlsx";
        }
        return filename + ".xlsx";
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
