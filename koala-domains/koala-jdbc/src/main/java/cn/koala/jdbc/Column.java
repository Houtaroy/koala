package cn.koala.jdbc;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Houtaroy
 */
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class Column {
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
  public Column(ResultSet rs) throws SQLException {
    name = rs.getString("COLUMN_NAME");
    type = JDBCType.valueOf(rs.getInt("DATA_TYPE")).getName();
    length = rs.getInt("COLUMN_SIZE");
    nullable = rs.getBoolean("IS_NULLABLE");
    comment = rs.getString("REMARKS");
  }
}