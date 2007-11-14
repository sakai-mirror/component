/*
 * Created on 12 Nov 2007
 */
package org.sakaiproject.component.impl.spring.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.impl.BeanLocator;
import org.sakaiproject.component.impl.spring.ComponentRecords;
import org.sakaiproject.component.impl.spring.ComponentManagerTargetSource;
import org.sakaiproject.component.impl.spring.ComponentRecord;
import org.sakaiproject.component.impl.spring.ContextProcessor;
import org.sakaiproject.component.util.ClassLoaderUtil;
import org.sakaiproject.component.util.ClassVisibilityRecord;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContextException;

/**
 * The core logic implementation for the Component Manager. Accepts registration
 * of incoming new Spring contexts, and will populate records such that proxies
 * will be delivered from the root context on demand.
 * <p>
 * Implementation of BeanLocator interface is exterior interface that will cause
 * a proxy to be dispensed for a bean with the given name. This proxy is
 * expected to be good "for all time", and should only be called once during the
 * lifetime of the manager for each name.
 * 
 * @author Antranig Basman (antranig@caret.cam.ac.uk)
 * 
 */

public class ComponentManagerCore implements ContextProcessor, BeanLocator {

    private static Log log = LogFactory.getLog(ComponentManagerCore.class);

    private DefaultListableBeanFactory rootContext;

    private ComponentRecords records;

    private ClassLoader classLoader;

    private Map<String, Object> straySingletons = new ConcurrentHashMap<String, Object>();

    public void setRootContext(DefaultListableBeanFactory rootContext) {
        this.rootContext = rootContext;
    }

    public void setComponentsClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void setRecords(ComponentRecords records) {
        this.records = records;
    }

    private BeanRecord locateBeanConcrete(String name) {
        String componentName = records.getComponentForBean(name);
        if (componentName == null) {
            throw new NoSuchBeanDefinitionException(name);
        }
        if (componentName == ComponentRecords.STRAY_SINGLETON) {
            Object bean = straySingletons.get(name);
            return new BeanRecord(bean, null);
        }
        ComponentRecord record = records.getRecord(componentName);
        if (record == null) {
            // TODO: better semantics here for blocking/momentary reloading
            throw new IllegalArgumentException("Error locating bean " + name
                    + ": component has been unloaded");
        }
        Object concrete = null;
        try {
            if (!record.cac.isActive()) {
                record.cac.bareRefresh();
            }
            concrete = record.cac.getBean(name);
        } catch (RuntimeException e) {
            log.error("Failed to preinstantiate the singleton named " + name
                    + ". Destroying all Spring beans.", e);
            throw e;
        }

        return new BeanRecord(concrete, record);
    }

    public void registerSingleton(String name, Object bean) {
        records
                .registerComponentForBean(name,
                        ComponentRecords.STRAY_SINGLETON);
        straySingletons.put(name, bean);
    }

    // Called when we require a CONCRETE load of a bean, this should construct a
    // proxy and
    // also immediately trigger load of the concrete bean if it has not already
    // been fetched.
    public Object locateBean(final String name) {
        ObjectFactory getter = new ObjectFactory() {
            public Object getObject() throws BeansException {
                return locateBeanConcrete(name);
            }
        };

        Object proxy = createProxy(getter);
        return proxy;
    }

    public Object createProxy(ObjectFactory getter) {
        BeanRecord concrete = (BeanRecord) getter.getObject();
        ClassVisibilityRecord cvr = ClassLoaderUtil.getVisibilityRecord(
                concrete.bean.getClass(), classLoader);

        ProxyFactoryBean pfb = new ProxyFactoryBean();
        pfb.setInterfaces(cvr.interfaces);
        if (cvr.clazz != null && cvr.clazz != Object.class) {
            pfb.setProxyTargetClass(true);
        }
        ComponentManagerTargetSource cmts = new ComponentManagerTargetSource(
                cvr.clazz, getter);
        pfb.setTargetSource(cmts);
        pfb.setBeanFactory(rootContext);
        if (concrete.record != null) {
            pfb.addAdvice(concrete.record.interceptor);
        }
        try {
            return pfb.getObject();
        }
        catch (Exception e) {
            log.info("Failed to construct proxy for bean of class " + cvr.clazz + ": bean will be delivered concretely: " + e.getMessage());
            return concrete.bean;
        }
    }

    public void registerComponentContext(ComponentRecord record) {
        records.registerComponent(record);
        String[] defs = record.cac.getBeanDefinitionNames();
        ConfigurableListableBeanFactory clbf = record.cac.getBeanFactory();
        for (String defname : defs) {
            BeanDefinition def = clbf.getBeanDefinition(defname);
            if (def.isAbstract()) {
                // All abstract definitions can go upstairs. They will only give
                // rise
                // to local beans.
                rootContext.registerBeanDefinition(defname, def);
            } else {
                if (!def.isSingleton()) {
                    log
                            .warn("Skipping bean definition "
                                    + defname
                                    + " from context "
                                    + record.componentName
                                    + " from export: only singleton definitions can be exported");
                } else {
                    rootContext.registerBeanDefinition(defname, def);
                    records.registerComponentForBean(defname,
                            record.componentName);
                }
            }
        }

    }

    public void unregisterComponentContext(String componentName) {
        // TODO: consider "delay/blocking" concept. Also if we can recognise any
        // of their ClassLoaders, remember to clean out any stray singletons.
        throw new UnsupportedOperationException(
                "Unloading of components not supported");
    }

    public void refreshAll() {
        for (ComponentRecord record : records) {
            try {
                if (!record.cac.isActive()) {
                    record.cac.refresh();
                }
                else {
                    record.cac.concludeRefresh();
                }
            } catch (Exception e) {
                String message = "Error refreshing component context "
                        + record.componentName;                       
                log.error(message, e);
            }
        }
    }

}
