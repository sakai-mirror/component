/*
 * Created on 13 Nov 2007
 */
package org.sakaiproject.component.impl.spring.support;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class InvocationInterceptor implements MethodInterceptor {

    private ClassLoader componentClassLoader;
    
    public Object invoke(MethodInvocation invocation) throws Throwable {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(componentClassLoader);
        try {
            return invocation.proceed();
        }
        finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    public InvocationInterceptor(ClassLoader componentClassLoader) {
        this.componentClassLoader = componentClassLoader;
    }

}
