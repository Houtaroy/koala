package cn.koala.utils;

import lombok.Data;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * jdbc列
 *
 * @author Houtaroy
 */
@Data
public class JdbcColumn {
  protected String name;
  protected String type;
  protected Integer length;
  protected boolean nullable;
  protected String comment;

  /**
   * 构造函数
   *
   * @param rs ResultSet
   * @throws SQLException SQL异常
   */
  public JdbcColumn(ResultSet rs) throws SQLException {
    name = rs.getString(JdbcLabels.COLUMN_NAME);
    type = JDBCType.valueOf(rs.getInt(JdbcLabels.COLUMN_TYPE)).getName();
    length = rs.getInt(JdbcLabels.COLUMN_SIZE);
    nullable = rs.getBoolean(JdbcLabels.IS_NULLABLE);
    comment = rs.getString(JdbcLabels.COMMENT);
  }
}
