package cn.koala.office;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 抽象Excel写入器
 *
 * @author Houtaroy
 */
public abstract class AbstractExcelWebWriter implements ExcelWebWriter {
  public static final String DOWNLOAD_EXTENSION = ".xlsx";

  @Override
  public <T> void write(HttpServletResponse response, String filename, List<T> data, Class<T> tClass)
    throws IOException {
    write(prepareResponse(response, filename).getOutputStream(), data, tClass);
  }

  @Override
  public void write(
    HttpServletResponse response, String filename, List<List<String>> headers, List<LinkedHashMap<String, Object>> data
  ) throws IOException {
    write(prepareResponse(response, filename).getOutputStream(), headers, data);
  }

  @Override
  public <T> void template(
    String templateFilePathName, HttpServletResponse response, String filename, List<T> data, Class<T> tClass
  ) throws IOException {
    template(templateFilePathName, prepareResponse(response, filename).getOutputStream(), data, tClass);
  }

  /**
   * 准备HttpServletResponse
   *
   * @param response HttpServletResponse
   * @param filename 文件名
   * @return 准备好的HttpServletResponse
   */
  protected HttpServletResponse prepareResponse(HttpServletResponse response, String filename) {
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    if (!filename.endsWith(DOWNLOAD_EXTENSION)) {
      filename += DOWNLOAD_EXTENSION;
    }
    response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + filename);
    return response;
  }
}
