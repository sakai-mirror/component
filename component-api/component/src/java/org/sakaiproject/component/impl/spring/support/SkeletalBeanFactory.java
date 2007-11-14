/*
 * Created on 12 Nov 2007
 */
package org.sakaiproject.component.impl.spring.support;

import org.sakaiproject.component.impl.BeanLocator;
import org.sakaiproject.component.impl.BeanLocatorAcceptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/** The class of the core shared context. It needs to be at *least* an
 * AbstractBeanFactory so that parent definitions can be resolved from 
 * child contexts. It also keeps the functionality of auto-creating proxies.
 * These are created *extremely* late, since in general we cannot perform
 * reliable introspection to determine the proxy type to create until after
 * we have seen the concrete bean type.
 * @author Antranig Basman (antranig@caret.cam.ac.uk)
 *
 */

public class SkeletalBeanFactory extends DefaultListableBeanFactory implements BeanLocatorAcceptor {
    private BeanLocator locator;

    // override this core impl method from AbstractBeanFactory
    public Object getBean(String name, Class requiredType, Object[] args) {
        Object singleton = getSingleton(name);
        if (singleton == null) {
            singleton = locator.locateBean(name);
            registerSingleton(name, singleton);
        }
        return getObjectForBeanInstance(singleton, name, null);
    }
    
    public void setBeanLocator(BeanLocator locator) {
       this.locator = locator;
    }
}
