/*
 * Created on 13 Nov 2007
 */
package org.sakaiproject.component.impl.spring;

public interface ContextProcessor {
    public void registerComponentContext(ComponentRecord record);
    public void unregisterComponentContext(String componentName);
}
