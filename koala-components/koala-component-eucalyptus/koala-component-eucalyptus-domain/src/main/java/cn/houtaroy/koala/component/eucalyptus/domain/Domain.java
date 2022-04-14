package cn.houtaroy.koala.component.eucalyptus.domain;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author Houtaroy
 */
@Data
@NoArgsConstructor
public class Domain {

  protected String name;
  protected String description;
  protected List<Property> properties;
}