/*
 * Created on 12 Nov 2007
 */
package org.sakaiproject.component.api.spring;

import org.sakaiproject.component.api.ComponentManager;
import org.springframework.context.ApplicationContext;

public interface SpringComponentManager extends ComponentManager {
  public void init();
  public ApplicationContext getRootContext();
  public void addChildAc();
  public void removeChildAc();
}
