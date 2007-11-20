/**********************************************************************************
 * $URL: https://source.sakaiproject.org/svn/component/trunk/component-api/component/src/java/org/sakaiproject/component/impl/SpringCompMgr.java $
 * $Id: SpringCompMgr.java 34877 2007-09-10 19:19:04Z lance@indiana.edu $
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006, 2007 The Sakai Foundation.
 * 
 * Licensed under the Educational Community License, Version 1.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *      http://www.opensource.org/licenses/ecl1.php
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.component.impl.spring.support;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.spring.SpringComponentManager;
import org.sakaiproject.component.impl.spring.BeanFactoryPostProcessorCreator;
import org.sakaiproject.component.impl.spring.ComponentRecord;
import org.sakaiproject.component.impl.spring.ComponentRecords;
import org.sakaiproject.component.impl.spring.ContextProcessor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>
 * SpringCompMgr manages API implementation components using the Springframework
 * ApplicationContext.
 * </p>
 * <p>
 * See the {@link org.sakaiproject.api.kernel.component.ComponentManager}interface
 * for details.
 * </p>
 */
public class SpringComponentManagerImpl implements SpringComponentManager,
        ContextProcessor {
    /** Our log (commons). */
    private static Log M_log = LogFactory
            .getLog(SpringComponentManagerImpl.class);

    /**
     * System property to control if we close on jvm shutdown (if set) or on the
     * loss of our last child (if not set).
     */
    protected final static String CLOSE_ON_SHUTDOWN = "sakai.component.closeonshutdown";

    protected final static String DEFAULT_CONFIGURATION_FILE = "classpath:/org/sakaiproject/config/sakai-configuration.xml";
    protected final static String CONFIGURATION_FILE_NAME = "sakai-configuration.xml";

    /** The Spring Application Context. */
    private SkeletalBeanFactory rootFactory;

    private ConfigurableApplicationContext rootContext;

    /** The already created components given to manage (their interface names). */
    protected Map<String, Boolean> m_loadedComponents = new ConcurrentHashMap<String, Boolean>();

    /** A count of the # of child AC's that call us parent. */
    protected int m_childCount = 0;

    private List<BeanFactoryPostProcessor> processors = new ArrayList<BeanFactoryPostProcessor>();

    /** A set of properties used when configuring components. */
    protected Properties m_config;

    /** Records that close has been called. */
    protected boolean m_hasBeenClosed = false;

    // Temporary coincidence - one day, this manager will itself be in a
    // component.
    private ClassLoader componentsClassLoader = getClass().getClassLoader();

    private ComponentManagerCore componentManagerCore;

    /**
     * Initialize.
     */
    public SpringComponentManagerImpl() {
        // Note: don't init here, init after it's fully constructed
        // (and if it's being constructed by the cover, after the cover has set
        // it's instance variable).
        // othewise when singletons are instantiated, if they call a Cover or
        // Discovery in the init(),
        // the component manager cover will not yet have this object! -ggolden
    }

    public void init() {
        ClassLoader stored = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(componentsClassLoader);
            initImpl();
        } finally {
            Thread.currentThread().setContextClassLoader(stored);
        }
    }

    private void initCore() {
        rootFactory = new SkeletalBeanFactory();
        ComponentRecords records = new ComponentRecords();
        componentManagerCore = new ComponentManagerCore();
        componentManagerCore.setComponentsClassLoader(componentsClassLoader);
        componentManagerCore.setRootContext(rootFactory);
        componentManagerCore.setRecords(records);
        rootFactory.setBeanLocator(componentManagerCore);
        rootContext = new GenericApplicationContext(rootFactory);
        loadRootContext();
        acquirePostProcessors();
    }

    private void loadRootContext() {
        List<String> configLocationList = new ArrayList<String>();
        configLocationList.add(DEFAULT_CONFIGURATION_FILE);
        String localConfigLocation = System.getProperty("sakai.home")
                + CONFIGURATION_FILE_NAME;
        File configFile = new File(localConfigLocation);
        if (configFile.exists()) {
            configLocationList.add("file:" + localConfigLocation);
        }
        // Create a new XmlBeanDefinitionReader for the given BeanFactory.
        XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(
                rootFactory);
        beanDefinitionReader.setBeanClassLoader(Thread.currentThread()
                .getContextClassLoader());
        beanDefinitionReader.setResourceLoader(rootContext);
        beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(
                rootContext));

        if (configLocationList.size() > 0) {
            beanDefinitionReader.loadBeanDefinitions(configLocationList
                    .toArray(new String[configLocationList.size()]));
        }
        rootContext.refresh();
    }

    private void acquirePostProcessors() {
        String[] postProcessorCreatorNames = rootFactory.getBeanNamesForType(
                BeanFactoryPostProcessorCreator.class, false, false);
        for (int i = 0; i < postProcessorCreatorNames.length; i++) {
            BeanFactoryPostProcessorCreator postProcessorCreator = (BeanFactoryPostProcessorCreator) rootFactory
                    .getBean(postProcessorCreatorNames[i]);
            for (BeanFactoryPostProcessor bfpp : postProcessorCreator
                    .getBeanFactoryPostProcessors()) {
                processors.add(bfpp);
            }
        }
    }

    /**
     * Initialize the component manager.
     */
    private void initImpl() {
        if (rootFactory != null)
            return;

        // if configured (with the system property CLOSE_ON_SHUTDOWN set),
        // create a shutdown task to close when the JVM closes
        // (otherwise we will close in removeChildAc() when the last child is
        // gone)
        if (System.getProperty(CLOSE_ON_SHUTDOWN) != null) {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    close();
                }
            });
        }

        // Make sure a "sakai.home" system property is set.
        ensureSakaiHome();
        checkSecurityPath();

        initCore();

        // load component packages
        loadComponents();

        // get our special log handler started before the rest
        try {
            rootFactory
                    .getBean("org.sakaiproject.log.api.LogConfigurationManager");
        } catch (Throwable t) {
            M_log.warn(t.getMessage(), t);
        }

        try {
            this.componentManagerCore.refreshAll();
        } catch (Throwable t) {
            M_log.warn(t.getMessage(), t);
        }
    }

    /**
     * Access the ApplicationContext
     * 
     * @return the ApplicationContext
     */
    public ApplicationContext getRootContext() {
        return this.rootContext;
    }

    /**
     * Finalize.
     */
    protected void finalize() {
        close();
    }

    /**
     * {@inheritDoc}
     */
    public Object get(Class<?> iface) {
        Object component = null;

        try {
            component = rootFactory.getBean(iface.getName(), iface);
        } catch (NoSuchBeanDefinitionException e) {
            // This is an expected outcome, we don't usually want logs
            if (M_log.isDebugEnabled())
                M_log.debug("get(" + iface.getName() + "): " + e, e);
        } catch (Throwable t) {
            M_log.warn("get(" + iface.getName() + "): ", t);
        }

        return component;
    }

    /**
     * {@inheritDoc}
     */
    public Object get(String ifaceName) {
        Object component = null;

        try {
            component = rootFactory.getBean(ifaceName);
        } catch (NoSuchBeanDefinitionException e) {
            // This is an expected outcome, we don't usually want logs
            if (M_log.isDebugEnabled())
                M_log.debug("get(" + ifaceName + "): " + e, e);
        } catch (Throwable t) {
            M_log.warn("get(" + ifaceName + "): ", t);
        }

        return component;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Class<?> iface) {
        boolean found = rootFactory.containsBeanDefinition(iface.getName());

        return found;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(String ifaceName) {
        boolean found = rootFactory.containsBeanDefinition(ifaceName);

        return found;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getRegisteredInterfaces() {
        Set<String> rv = new HashSet<String>();

        // get the registered ones
        String[] names = rootFactory.getBeanDefinitionNames();
        for (int i = 0; i < names.length; i++) {
            rv.add(names[i]);
        }

        // add the loaded ones
        for (String loaded : m_loadedComponents.keySet()) {
            rv.add(loaded);
        }

        return rv;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        m_hasBeenClosed = true;
        rootFactory.destroySingletons();
    }

    /**
     * {@inheritDoc}
     */
    public void loadComponent(Class<?> iface, Object component) {
        // Spring doesn't list these in getBeanDefinitionNames, so we keep track
        m_loadedComponents.put(iface.getName(), Boolean.TRUE);

        // TODO: make sure we proxy it!
        rootFactory.registerSingleton(iface.getName(), component);
    }

    /**
     * {@inheritDoc}
     */
    public void loadComponent(String ifaceName, Object component) {
        // Spring doesn't list these in getBeanDefinitionNames, so we keep track
        m_loadedComponents.put(ifaceName, Boolean.TRUE);

        rootFactory.registerSingleton(ifaceName, component);
    }

    /**
     * Locate the component loader, and load any available components.
     */
    protected void loadComponents() {
        // see if we can find a components loader
        ComponentsLoaderImpl loader = new ComponentsLoaderImpl(this);

        // locate the components root
        // if we have our system property set, use it
        String componentsRoot = System
                .getProperty(SAKAI_COMPONENTS_ROOT_SYS_PROP);
        if (componentsRoot == null) {
            // if we are in Catalina, place it at
            // ${catalina.home}/components/
            String catalina = getCatalina();
            if (catalina != null) {
                componentsRoot = catalina + File.separatorChar + "components"
                        + File.separatorChar;
            }
        }

        if (componentsRoot == null) {
            M_log
                    .warn("loadComponents: cannot estabish a root directory for the components packages");
            return;
        }

        // make sure this is set
        System.setProperty(SAKAI_COMPONENTS_ROOT_SYS_PROP, componentsRoot);

        // load components
        loader.load(componentsRoot);

    }

    /**
     * Increment the count of ACs that call this one parent.
     */
    public synchronized void addChildAc() {
        m_childCount++;
    }

    /**
     * Decrement the count of ACs that call this one parent.
     */
    public synchronized void removeChildAc() {
        m_childCount--;

        // if we are not using the shutdown hook, close() when the m_childCount
        // == 0
        if ((m_childCount == 0)
                && (System.getProperty(CLOSE_ON_SHUTDOWN) == null)) {
            close();
        }
    }

    /**
     * Check the environment for catalina's base or home directory.
     * 
     * @return Catalina's base or home directory.
     */
    protected String getCatalina() {
        String catalina = System.getProperty("catalina.base");
        if (catalina == null) {
            catalina = System.getProperty("catalina.home");
        }

        return catalina;
    }

    /**
     * @inheritDoc
     */
    public Properties getConfig() {
        return m_config;
    }

    /**
     * @inheritDoc
     */
    public void waitTillConfigured() {
        // Nothing really to do - the cover takes care of this -ggolden
    }

    /**
     * @inheritDoc
     */
    public boolean hasBeenClosed() {
        return m_hasBeenClosed;
    }

    public void registerComponentContext(ComponentRecord record) {
        ConfigurableListableBeanFactory clbf = record.cac.getBeanFactory();
        for (BeanFactoryPostProcessor bfpp : processors) {
            bfpp.postProcessBeanFactory(clbf);
        }
        componentManagerCore.registerComponentContext(record);
        record.cac.setParent(rootContext);
    }

    public void unregisterComponentContext(String componentName) {
        // TODO: consider "delay/blocking" concept. Also if we can recognise any
        // of their ClassLoaders, remember to clean out any stray singletons.
        throw new UnsupportedOperationException(
                "Unloading of components not supported");
    }

    private void ensureSakaiHome() {
        // find a path to sakai files on the app server - if not set, set it
        String sakaiHomePath = System.getProperty("sakai.home");
        if (sakaiHomePath == null) {
            String catalina = getCatalina();
            if (catalina != null) {
                sakaiHomePath = catalina + File.separatorChar + "sakai"
                        + File.separatorChar;
            }
        }

        // strange case...
        if (sakaiHomePath == null) {
            sakaiHomePath = File.separatorChar + "usr" + File.separatorChar
                    + "local" + File.separatorChar + "sakai"
                    + File.separatorChar;
        }
        if (!sakaiHomePath.endsWith(File.separator))
            sakaiHomePath = sakaiHomePath + File.separatorChar;

        final File sakaiHomeDirectory = new File(sakaiHomePath);
        if (!sakaiHomeDirectory.exists()) // no sakai.home directory exists,
                                            // try to create one
        {
            if (sakaiHomeDirectory.mkdir()) {
                M_log
                        .debug("Created sakai.home directory at: "
                                + sakaiHomePath);
            } else {
                M_log.warn("Could not create sakai.home directory at: "
                        + sakaiHomePath);
            }
        }

        // make sure it's set properly
        System.setProperty("sakai.home", sakaiHomePath);
    }

    private void checkSecurityPath() {
        // check for the security home
        String securityPath = System.getProperty("sakai.security");
        if (securityPath != null) {
            // make sure it's properly slashed
            if (!securityPath.endsWith(File.separator))
                securityPath = securityPath + File.separatorChar;
            System.setProperty("sakai.security", securityPath);
        }
    }

}
