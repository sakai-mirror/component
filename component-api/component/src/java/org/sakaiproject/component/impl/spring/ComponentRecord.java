/*
 * Created on 12 Nov 2007
 */
package org.sakaiproject.component.impl.spring;

import org.aopalliance.intercept.MethodInterceptor;
import org.sakaiproject.component.impl.spring.support.InvocationInterceptor;

public class ComponentRecord {
    public String componentName;
    
    public ClassLoader classLoader;
    
    public MethodInterceptor interceptor;
    
    public StaggeredRefreshApplicationContext cac;

    public ComponentRecord(String componentName, ClassLoader classLoader,
            StaggeredRefreshApplicationContext cac) {
        this.componentName = componentName;
        this.classLoader = classLoader;
        this.cac = cac;
        this.interceptor = new InvocationInterceptor(classLoader);
    }
}
