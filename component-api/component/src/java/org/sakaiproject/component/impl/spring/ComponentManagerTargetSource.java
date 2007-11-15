/*
 * Created on 12 Nov 2007
 */
package org.sakaiproject.component.impl.spring;

import org.sakaiproject.component.impl.spring.support.BeanRecord;
import org.springframework.aop.TargetSource;
import org.springframework.beans.factory.ObjectFactory;

public class ComponentManagerTargetSource implements TargetSource {
    private Class<?> beanClass;
    private ObjectFactory factory;

    public ComponentManagerTargetSource(Class<?> beanClass, ObjectFactory factory) {
        this.beanClass = beanClass;
        this.factory = factory;
    }

    public Object getTarget() {
        // Probably want to optimise this in the long term - however it means
        // keeping reverse records of proxies
        BeanRecord concrete = (BeanRecord) factory.getObject();
        return concrete.bean;
    }

    public Class<?> getTargetClass() {
        return beanClass;
    }

    public boolean isStatic() {
        return false;
    }

    public void releaseTarget(Object target) throws Exception {
    }

}
