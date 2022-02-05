package com.example.util;

import cn.hutool.core.io.resource.ClassPathResource;
import com.alibaba.excel.EasyExcel;
import org.apache.poi.ss.usermodel.Workbook;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * <p>
 * 下载工具
 * </p>
 *
 * @author MrWen
 **/
public class DownloadUtil {

    /**
     * @param filePath   要下载的文件路径
     * @param returnName 返回的文件名
     * @param response   HttpServletResponse
     * @param delFlag    是否删除文件
     */
    protected static void download(String filePath, String returnName, HttpServletResponse response, boolean delFlag) {
        prototypeDownload(new File(filePath), returnName, response, delFlag);
    }


    /**
     * @param file       要下载的文件
     * @param returnName 返回的文件名
     * @param response   HttpServletResponse
     * @param delFlag    是否删除文件
     */
    protected static void download(File file, String returnName, HttpServletResponse response, boolean delFlag) {
        prototypeDownload(file, returnName, response, delFlag);
    }

    /**
     * @param file       要下载的文件
     * @param returnName 返回的文件名
     * @param response   HttpServletResponse
     * @param delFlag    是否删除文件
     */
    public static void prototypeDownload(File file, String returnName, HttpServletResponse response, boolean delFlag) {
        // 下载文件
        FileInputStream inputStream = null;
        ServletOutputStream outputStream = null;
        try {
            if (!file.exists()) {
                return;
            }
            response.reset();
            //设置响应类型	PDF文件为"application/pdf"，WORD文件为："application/msword"， EXCEL文件为："application/vnd.ms-excel"。
            response.setContentType("application/octet-stream;charset=utf-8");

            //设置响应的文件名称,并转换成中文编码
            //returnName = URLEncoder.encode(returnName,"UTF-8");
            //保存的文件名,必须和页面编码一致,否则乱码
            returnName = response.encodeURL(new String(returnName.getBytes(), "iso8859-1"));

            //attachment作为附件下载；inline客户端机器有安装匹配程序，则直接打开；注意改变配置，清除缓存，否则可能不能看到效果
            response.addHeader("Content-Disposition", "attachment;filename=" + returnName);

            //将文件读入响应流
            inputStream = new FileInputStream(file);
            outputStream = response.getOutputStream();
            int length = 1024;
            int readLength = 0;
            byte[] buf = new byte[1024];
            readLength = inputStream.read(buf, 0, length);
            while (readLength != -1) {
                outputStream.write(buf, 0, readLength);
                readLength = inputStream.read(buf, 0, length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert outputStream != null;
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //删除原文件
            if (delFlag) {
                file.delete();
            }
        }
    }

    /**
     * by tony 2013-10-17
     *
     * @param byteArrayOutputStream 将文件内容写入ByteArrayOutputStream
     * @param response              HttpServletResponse	写入response
     * @param returnName            返回的文件名
     */
    public static void download(ByteArrayOutputStream byteArrayOutputStream, HttpServletResponse response, String returnName) {
        try {
            response.setContentType("application/octet-stream;charset=utf-8");
            //保存的文件名,必须和页面编码一致,否则乱码
            returnName = response.encodeURL(new String(returnName.getBytes(), "iso8859-1"));
            response.addHeader("Content-Disposition", "attachment;filename=" + returnName);
            response.setContentLength(byteArrayOutputStream.size());

            //取得输出流
            ServletOutputStream outputstream = response.getOutputStream();
            //写到输出流
            byteArrayOutputStream.writeTo(outputstream);
            //关闭
            byteArrayOutputStream.close();
            //刷数据
            outputstream.flush();
        } catch (Exception e) {
            throw new RuntimeException("下载文件失败啦！原因:" + e.getMessage());
        }
    }


    /**
     * excel导出
     *
     * @param wb       工作簿
     * @param fileName excel名称  机器人.xls  机器人.xlsx
     */
    public static void download(Workbook wb, String fileName) {
        try {
            HttpServletResponse response = CommonUtil.getResponse();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            wb.write(byteArrayOutputStream);
            download(byteArrayOutputStream, response, fileName);
        } catch (Exception e) {
            throw new RuntimeException("下载文件失败啦！原因:" + e.getMessage());
        }
    }


    /**
     * todo 推荐
     * 文件下载并且失败的时候返回json（默认失败了会返回一个有部分数据的Excel）
     * 直接写，这里注意，finish的时候会自动关闭OutputStream,当然你外面再关闭流问题不大
     *
     * @param head      创建excel对应的实体对象 参照{@link } todo
     * @param data      设置返回的 参数
     * @param fileName  文件名   机器人 机器人.xlsx  缺失文件类型时,会根据数据量自动适配xlsx或csv格式
     * @param sheetName sheetName
     */
    public static void downloadExcel(Class<?> head, Collection<?> data, String fileName, String sheetName) {
        //自动补齐文件类型
        fileName = fillFileType(fileName, data);

        HttpServletResponse response = CommonUtil.getResponse();
        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
            // 这里需要设置不关闭流
            EasyExcel.write(response.getOutputStream(), head).autoCloseStream(Boolean.FALSE).sheet(sheetName)
                    .doWrite(data);
        } catch (Exception e) {
            // 重置response
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            try {
                //todo 自定义统一异常返回
//                response.getWriter().println(JSONUtil.toJsonStr(AjaxResult.failed()));
                response.getWriter().println("文件下载失败,原因：" + e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
    }


    /**
     * 文件下载（失败了会返回一个有部分数据的Excel）
     * 直接写，这里注意，finish的时候会自动关闭OutputStream,当然你外面再关闭流问题不大
     *
     * @param head      创建excel对应的实体对象 参照{@link } todo
     * @param data      设置返回的 参数
     * @param fileName  文件名   机器人 机器人.xlsx  缺失文件类型时,会根据数据量自动适配xlsx或csv格式
     * @param sheetName sheetName
     */
    public static void downloadExcel2(Class<?> head, Collection<?> data, String fileName, String sheetName) {
        try {
            //自动补齐文件类型
            fileName = fillFileType(fileName, data);

            HttpServletResponse response = CommonUtil.getResponse();
            // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
            EasyExcel.write(response.getOutputStream(), head).sheet(sheetName).doWrite(data);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("文件excel下载失败啦，原因:{}" + e.getMessage());
        }
    }

    /**
     * 按照模板导出
     * 文件下载并且失败的时候返回json（默认失败了会返回一个有部分数据的Excel）
     * 直接写，这里注意，finish的时候会自动关闭OutputStream,当然你外面再关闭流问题不大
     *
     * @param data     设置返回的 参数
     * @param fileName 文件名   机器人 机器人.xlsx  缺失文件类型时,会根据数据量自动适配xlsx或csv格式
     * @param template 模板路径，相对于resources
     */
    public static void downloadExcelByTemplate(Collection<?> data, String fileName, String template) {
        HttpServletResponse response = CommonUtil.getResponse();
        //自动补齐文件类型
        fileName = fillFileType(fileName, data);

        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系
            fileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName);
            // 这里需要设置不关闭流

//            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream())
//                    .withTemplate(new ClassPathResource(template).getStream()).build();
//            WriteSheet writeSheet = EasyExcel.writerSheet().build();
//            data.forEach(x -> {
//                excelWriter.fill(x, writeSheet);
//            });

            EasyExcel.write(response.getOutputStream())
                    .withTemplate(new ClassPathResource(template).getStream())
                    .sheet().doFill(data);

        } catch (Exception e) {
            // 重置response
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            try {
                //todo 自定义统一异常返回
//                response.getWriter().println(JSONUtil.toJsonStr(AjaxResult.failed()));
                response.getWriter().println("文件下载失败,原因：" + e.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new RuntimeException(ex);
            }
        }
    }


    /**
     * 自动补齐文件类型,这里只选择xlsx和csv
     *
     * @param fileName 机器人 机器人.xlsx  缺失文件类型时,会根据数据量自动适配xlsx或csv格式
     * @param data     数据量
     * @return 机器人.xlsx
     */
    private static String fillFileType(String fileName, Collection<?> data) {
        //自动补齐文件类型,xlsx最多只能 1048576
        if (!fileName.contains(".")) {
            if (data.size() <= 1048500) {
                fileName = fileName + ".xlsx";
            } else {
                fileName = fileName + ".csv";
            }
        }
        return fileName;
    }
}