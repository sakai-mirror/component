/*
 * Created on 12 Nov 2007
 */
package org.sakaiproject.component.impl.spring;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ComponentRecords implements Iterable<ComponentRecord> {
    public static final String STRAY_SINGLETON = "** Stray singleton **";
    
    private Map<String, ComponentRecord> records = new ConcurrentHashMap<String, ComponentRecord>();
    
    private Map<String, String> beanToComponent = new ConcurrentHashMap<String, String>();
    
    public ComponentRecord getRecord(String componentName) {
        return records.get(componentName);
    }
    
    public void registerComponent(ComponentRecord record) {
        records.put(record.componentName, record);
    }
    
    public void unregisterComponent(String componentName) {
        records.remove(componentName);
    }
    
    public void registerComponentForBean(String beanName, String componentName) {
        beanToComponent.put(beanName, componentName);
    }
    
    public String getComponentForBean(String beanName) {
        return beanToComponent.get(beanName);
    }

    public Iterator<ComponentRecord> iterator() {
       return records.values().iterator();
    }
    
}
