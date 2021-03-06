package cn.koala.system;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author Houtaroy
 */
@Schema(description = "用户")
public interface User extends Idable<String>, Sortable, Stateable, UserDetails {

  /**
   * 获取用户名
   *
   * @return 用户名
   */
  @Override
  @Schema(description = "用户名")
  String getUsername();

  /**
   * 获取密码
   *
   * @return 密码
   */
  @Override
  @JsonIgnore
  @Schema(description = "密码")
  String getPassword();

  /**
   * 设置密码
   *
   * @param password 密码
   */
  void setPassword(String password);

  /**
   * 获取名称
   *
   * @return 名称
   */
  @Schema(description = "名称")
  String getName();

  /**
   * 获取头像
   *
   * @return 头像
   */
  @Schema(description = "头像")
  String getAvatar();

  /**
   * 获取邮箱
   *
   * @return 邮箱
   */
  @Schema(description = "邮箱")
  String getEmail();

  /**
   * 获取手机号
   *
   * @return 手机号
   */
  @Schema(description = "手机号")
  String getPhone();

  /**
   * 获取角色列表
   *
   * @return 角色列表
   */
  @Schema(description = "角色列表")
  List<? extends Role> getRoles();

  /**
   * 是否已启动
   *
   * @return true 是 false 否
   */
  @JsonIgnore
  @Override
  default boolean isEnabled() {
    return getIsEnable() == YesNo.YES;
  }


  /**
   * 账号是否过期
   *
   * @return 结果
   */
  @JsonIgnore
  @Override
  default boolean isAccountNonExpired() {
    return true;
  }

  /**
   * 账号是否锁定
   *
   * @return 结果
   */
  @JsonIgnore
  @Override
  default boolean isAccountNonLocked() {
    return true;
  }

  /**
   * 证书是否过期
   *
   * @return 结果
   */
  @JsonIgnore
  @Override
  default boolean isCredentialsNonExpired() {
    return true;
  }

  /**
   * 获取授权列表
   *
   * @return 授权列表
   */
  @JsonIgnore
  @Override
  default Collection<? extends GrantedAuthority> getAuthorities() {
    List<GrantedAuthority> result = new ArrayList<>();
    Optional.ofNullable(getRoles()).ifPresent(roles -> roles.forEach(role ->
      Optional.ofNullable(role.getPermissions()).ifPresent(permissions ->
        result.addAll(permissions.stream().map(Codeable::getCode).map(SimpleGrantedAuthority::new).toList())
      )
    ));
    return result;
  }
}
