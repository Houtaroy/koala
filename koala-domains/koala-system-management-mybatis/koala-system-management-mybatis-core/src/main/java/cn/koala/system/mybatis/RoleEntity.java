package cn.koala.system.mybatis;

import cn.koala.system.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * @author Houtaroy
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class RoleEntity extends AbstractEntity implements Role {
  private String code;
  private String name;
  private String description;
  private List<PermissionEntity> permissions;
}
