/*
 * Created on 12 Nov 2007
 */
package org.sakaiproject.component.impl.spring.support;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.impl.BeanLocator;
import org.sakaiproject.component.impl.BeanLocatorAcceptor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanFactory;
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
    private static Log log = LogFactory.getLog(SkeletalBeanFactory.class);
    
    private BeanLocator locator;
    private boolean coreInitialized;
    private Set<String> coreBeans = new HashSet<String>();

    // override this core impl method from AbstractBeanFactory
    public Object getBean(String name, Class requiredType, Object[] args) {
        Object singleton = getSingleton(name);
        if (singleton == null) {
            if (name.startsWith(AbstractBeanFactory.FACTORY_BEAN_PREFIX)) {
                name = name.substring(AbstractBeanFactory.FACTORY_BEAN_PREFIX.length());
            }
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
        if (!coreBeans.contains(beanName) || !coreInitialized) {
            super.registerBeanDefinition(beanName, beanDefinition);
        }
        else {
            log.debug("Rejecting core bean def " + beanName);
        }
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
