/*
 * Created on 12 Nov 2007
 */
package org.sakaiproject.component.impl.spring.support;

import java.util.HashSet;
import java.util.Set;

import org.sakaiproject.component.impl.BeanLocator;
import org.sakaiproject.component.impl.BeanLocatorAcceptor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
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
    private boolean coreInitialized;
    private Set<String> coreBeans = new HashSet<String>();

    // override this core impl method from AbstractBeanFactory
    public Object getBean(String name, Class requiredType, Object[] args) {
        Object singleton = getSingleton(name);
        if (singleton == null) {
            if (coreBeans.contains(name)) {
                return super.getBean(name, requiredType, args);
            }
            singleton = locator.locateBean(name);
            registerSingleton(name, singleton);
        }
        return getObjectForBeanInstance(singleton, name, null);
    }
    
    public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
      throws BeanDefinitionStoreException {
        super.registerBeanDefinition(beanName, beanDefinition);
        if (!coreInitialized) {
            coreBeans.add(beanName);
        }
    }
    
    public void setCoreInitialized() {
        coreInitialized = true;
    }
    
    public void setBeanLocator(BeanLocator locator) {
       this.locator = locator;
    }
}
