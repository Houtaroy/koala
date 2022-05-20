package cn.koala.eucalyptus;

import java.sql.JDBCType;
import java.util.Map;

/**
 * @author Houtaroy
 */
public class Constant {
  public static final Map<String, String> TYPE_MAP;

  static {
    TYPE_MAP = Map.ofEntries(
      Map.entry(JDBCType.TINYINT.getName(), "Integer"),
      Map.entry(JDBCType.SMALLINT.getName(), "Integer"),
      Map.entry(JDBCType.INTEGER.getName(), "Integer"),
      Map.entry(JDBCType.BIGINT.getName(), "Long"),
      Map.entry(JDBCType.FLOAT.getName(), "Float"),
      Map.entry(JDBCType.DOUBLE.getName(), "Double"),
      Map.entry(JDBCType.DECIMAL.getName(), "Long"),
      Map.entry(JDBCType.BINARY.getName(), "Object"),
      Map.entry(JDBCType.BOOLEAN.getName(), "Boolean"),
      Map.entry(JDBCType.DATE.getName(), "LocalDate"),
      Map.entry(JDBCType.TIMESTAMP.getName(), "LocalDateTime"),
      Map.entry(JDBCType.NCLOB.getName(), "Object"),
      Map.entry(JDBCType.BLOB.getName(), "Object")
    );
  }
}