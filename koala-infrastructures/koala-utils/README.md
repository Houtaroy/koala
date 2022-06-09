# koala-utils

常用工具类

## 引入依赖

```xml

<dependency>
  <groupId>cn.koala</groupId>
  <artifactId>koala-utils</artifactId>
  <version>2022.0.0-SNAPSHOT</version>
</dependency>
```

## ImageUtil

### 压缩图片

自动推断压缩比例:

```java
public class Test {
  public static void main(String[] args) {
    try {
      compress("D:\\test\\1.png", "D:\\test\\1.jpg");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
```

注意: **因png图片压缩率较低, 所有图片压缩输出格式默认为`jpg`**

手动设置压缩比例/质量/输出格式:

```java
public class Test {
  public static void main(String[] args) {
    try {
      compress("D:\\test\\1.png", "D:\\test\\1.jpg", 0.5, 0.5, "jpg");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
```

### 压缩文件夹下所有图片

```java
import net.coobird.thumbnailator.name.Rename;

public class Test {
  public static void main(String[] args) {
    try {
      compress("D:\\Temp\\Images", "D:\\Temp\\Images", Rename.PREFIX_DOT_THUMBNAIL, 0.5, 0.5, "jpg");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
```