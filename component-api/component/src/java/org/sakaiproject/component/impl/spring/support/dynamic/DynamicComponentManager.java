/*
 * Created on 1 Dec 2007
 */
package org.sakaiproject.component.impl.spring.support.dynamic;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.sakaiproject.component.impl.support.DynamicComponentRecord;

public class DynamicComponentManager {
  private Map<String, DynamicComponentRecord> dynamicRecords = new HashMap<String, DynamicComponentRecord>();
  
  
  private File componentBase;
  
  public void setComponentBase(String componentBase) {
    this.componentBase = new File(componentBase);
  }
  
  public void init() {
    File[] dirs = componentBase.listFiles();
  }
  
}
