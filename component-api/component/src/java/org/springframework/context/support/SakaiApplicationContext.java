package org.springframework.context.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * Spring extension that logs failure details when singleton preinstantion fails.
 * 
 * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman</a>
 *
 */
public class SakaiApplicationContext extends ClassPathXmlApplicationContext {

	public SakaiApplicationContext(String configLocation) throws BeansException {
		super(configLocation);
	}
	
	protected DefaultListableBeanFactory createBeanFactory() {
		return new DefaultListableBeanFactory(getInternalParentBeanFactory()) {
			public void preInstantiateSingletons() throws BeansException {
				if (logger.isInfoEnabled()) {
					logger.info("Pre-instantiating singletons in factory [" + this + "]");
				}
				String currentBeanName = null;
				try {
					for (int i=0; i<getBeanDefinitionNames().length; i++) {
						String beanName = getBeanDefinitionNames()[i];
						currentBeanName = beanName;
						if (!containsSingleton(beanName) && containsBeanDefinition(beanName)) {
							RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
							if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit()) {
								if (bd.hasBeanClass() && FactoryBean.class.isAssignableFrom(bd.getBeanClass())) {
									FactoryBean factory = (FactoryBean) getBean(FACTORY_BEAN_PREFIX + beanName);
									if (factory.isSingleton()) {
										getBean(beanName);
									}
								}
								else {
									getBean(beanName);
								}
							}
						}
					}
				}
				catch (BeansException ex) {
					// Destroy already created singletons to avoid dangling resources.
					try {
						logger.error("Failed to preinstantiate " + currentBeanName + " as a singleton.  Destroying all spring beans.");
						ex.printStackTrace();
						destroySingletons();
					}
					catch (Throwable ex2) {
						logger.error("Pre-instantiating singletons failed, " +
								"and couldn't destroy already created singletons", ex2);
					}
					throw ex;
				}
			}
		};
	}
}
