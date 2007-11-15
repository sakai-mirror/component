/*
 * Created on 13 Nov 2007
 */
package org.sakaiproject.component.impl.spring.support;

import org.sakaiproject.component.impl.spring.StaggeredRefreshApplicationContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;

public class ComponentApplicationContextImpl extends GenericApplicationContext 
  implements StaggeredRefreshApplicationContext {

    // We copy the entire implementation of refresh() to omit the
    // preinstantiateSingletons call. We cannot perform this at startup for
    // fear of creating "phantom cycles" amidst the components.
    // Unfortunately the monitor is not available to us here so we must
    // proceed on trust that noone else attempts to refresh the context.

    public void bareRefresh() throws BeansException, IllegalStateException {
        // Prepare this context for refreshing.
        prepareRefresh();

        // Tell the subclass to refresh the internal bean factory.
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // Prepare the bean factory for use in this context.
        prepareBeanFactory(beanFactory);

        try {
            // Allows post-processing of the bean factory in context subclasses.
            postProcessBeanFactory(beanFactory);

            // Invoke factory processors registered as beans in the context.
            invokeBeanFactoryPostProcessors(beanFactory);

            // Register bean processors that intercept bean creation.
            registerBeanPostProcessors(beanFactory);

            // Initialize message source for this context.
            initMessageSource();

            // Initialize event multicaster for this context.
            initApplicationEventMulticaster();

            // Initialize other special beans in specific context subclasses.
            onRefresh();

            // Check for listener beans and register them.
            registerListeners();

            // Instantiate singletons this late to allow them to access the
            // message source.
            // beanFactory.preInstantiateSingletons();

            // Last step: publish corresponding event.
            // publishEvent(new ContextRefreshedEvent(this));
        }

        catch (BeansException ex) {
            // Destroy already created singletons to avoid dangling resources.
            beanFactory.destroySingletons();
            throw ex;
        }
    }

    public void concludeRefresh() {
        try {
            getBeanFactory().preInstantiateSingletons();

            // Last step: publish corresponding event.
            publishEvent(new ContextRefreshedEvent(this));
        }

        catch (BeansException ex) {
            // Destroy already created singletons to avoid dangling resources.
            getBeanFactory().destroySingletons();
            throw ex;
        }
    }
}
