/**********************************************************************************
 * $URL$
 * $Id$
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

package org.sakaiproject.component.impl;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.api.ConfigurationLoader;
import org.sakaiproject.util.ComponentsLoader;
import org.sakaiproject.util.NoisierDefaultListableBeanFactory;
import org.sakaiproject.util.PropertyOverrideConfigurer;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>
 * SpringCompMgr manages API implementation components using the Springframework ApplicationContext.
 * </p>
 * <p>
 * See the {@link org.sakaiproject.api.kernel.component.ComponentManager}interface for details.
 * </p>
 */
public class SpringCompMgr implements ComponentManager
{
	/** Our log (commons). */
	private static Log M_log = LogFactory.getLog(SpringCompMgr.class);

	/** System property to control if we close on jvm shutdown (if set) or on the loss of our last child (if not set). */
	protected final static String CLOSE_ON_SHUTDOWN = "sakai.component.closeonshutdown";

	/** The Spring Application Context. */
	protected ConfigurableApplicationContext m_ac = null;

	/** The already created components given to manage (their interface names). */
	protected Set m_loadedComponents = new HashSet();

	/** A count of the # of child AC's that call us parent. */
	protected int m_childCount = 0;

	/** A set of properties used when configuring components. */
	protected Properties m_config = null;

	/** Records that close has been called. */
	protected boolean m_hasBeenClosed = false;
	
	private ConfigurationLoader configurationLoader;

	/**
	 * Initialize.
	 * 
	 * @param parent
	 *        A ComponentManager in which this one gets nested, or NULL if this is this top one.
	 */
	public SpringCompMgr(ComponentManager parent)
	{
		// Note: don't init here, init after it's fully constructed
		// (and if it's being constructed by the cover, after the cover has set it's instance variable).
		// othewise when singletons are instantiated, if they call a Cover or Discovery in the init(),
		// the component manager cover will not yet have this object! -ggolden
	}

	/**
	 * Initialize the component manager.
	 */
	public void init()
	{
		if (m_ac != null) return;

		m_ac = new GenericApplicationContext(new NoisierDefaultListableBeanFactory());

		// load component packages
		loadComponents();

		// if configured (with the system property CLOSE_ON_SHUTDOWN set), create a shutdown task to close when the JVM closes
		// (otherwise we will close in removeChildAc() when the last child is gone)
		if (System.getProperty(CLOSE_ON_SHUTDOWN) != null)
		{
			Runtime.getRuntime().addShutdownHook(new Thread()
			{
				public void run()
				{
					close();
				}
			});
		}

		// find a path to sakai files on the app server - if not set, set it
		String sakaiHomePath = System.getProperty("sakai.home");
		if (sakaiHomePath == null)
		{
			String catalina = getCatalina();
			if (catalina != null)
			{
				sakaiHomePath = catalina + File.separatorChar + "sakai" + File.separatorChar;
			}
		}

		// strange case...
		if (sakaiHomePath == null)
		{
			sakaiHomePath = File.separatorChar + "usr" + File.separatorChar + "local" + File.separatorChar + "sakai"
					+ File.separatorChar;
		}
		if (!sakaiHomePath.endsWith(File.separator)) sakaiHomePath = sakaiHomePath + File.separatorChar;

		final File sakaiHomeDirectory = new File(sakaiHomePath);
		if(!sakaiHomeDirectory.exists()) // no sakai.home directory exists, try to create one
		{
			if(sakaiHomeDirectory.mkdir())
			{
				M_log.debug("Created sakai.home directory at: "
						+ sakaiHomePath);
			}
			else
			{
				M_log.warn("Could not create sakai.home directory at: "
						+ sakaiHomePath);
			}
		}
		
		// make sure it's set properly
		System.setProperty("sakai.home", sakaiHomePath);
		
		// Collect values from all the properties files.
		configurationLoader = (ConfigurationLoader)m_ac.getBean("org.sakaiproject.component.api.ConfigurationLoader");
		m_config = configurationLoader.getProperties();

		// post process the definitions from components with overrides from these properties
		// - these get injected into the beans
		try
		{
			PropertyOverrideConfigurer pushProcessor = new PropertyOverrideConfigurer();
			pushProcessor.setProperties(m_config);
			pushProcessor.setIgnoreInvalidKeys(true);
			pushProcessor.postProcessBeanFactory(m_ac.getBeanFactory());
		}
		catch (Throwable t)
		{
			M_log.warn(t.getMessage(), t);
		}

		// post process the definitions from components (now overridden with our property overrides) to satisfy any placeholder
		// values
		try
		{
			PropertyPlaceholderConfigurer pullProcessor = new PropertyPlaceholderConfigurer();
			pullProcessor.setProperties(m_config);
			pullProcessor.postProcessBeanFactory(m_ac.getBeanFactory());
		}
		catch (Throwable t)
		{
			M_log.warn(t.getMessage(), t);
		}

		// get our special log handler started before the rest
		try
		{
			m_ac.getBean("org.sakaiproject.log.api.LogConfigurationManager");
		}
		catch (Throwable t)
		{
			M_log.warn(t.getMessage(), t);
		}

		try
		{
			// get the singletons loaded
			m_ac.refresh();
		}
		catch (Throwable t)
		{
			M_log.warn(t.getMessage(), t);
		}
	}

	/**
	 * Access the ApplicationContext
	 * 
	 * @return the ApplicationContext
	 */
	public ConfigurableApplicationContext getApplicationContext()
	{
		return m_ac;
	}

	/**
	 * Finalize.
	 */
	protected void finalize()
	{
		close();
	}

	/**
	 * {@inheritDoc}
	 */
	public Object get(Class iface)
	{
		Object component = null;

		try
		{
			component = m_ac.getBean(iface.getName(), iface);
		}
		catch (NoSuchBeanDefinitionException e)
		{
			// This is an expected outcome, we don't usually want logs
			if (M_log.isDebugEnabled()) M_log.debug("get(" + iface.getName() + "): " + e, e);
		}
		catch (Throwable t)
		{
			M_log.warn("get(" + iface.getName() + "): ", t);
		}

		return component;
	}

	/**
	 * {@inheritDoc}
	 */
	public Object get(String ifaceName)
	{
		Object component = null;

		try
		{
			component = m_ac.getBean(ifaceName);
		}
		catch (NoSuchBeanDefinitionException e)
		{
			// This is an expected outcome, we don't usually want logs
			if (M_log.isDebugEnabled()) M_log.debug("get(" + ifaceName + "): " + e, e);
		}
		catch (Throwable t)
		{
			M_log.warn("get(" + ifaceName + "): ", t);
		}

		return component;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean contains(Class iface)
	{
		boolean found = m_ac.containsBeanDefinition(iface.getName());

		return found;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean contains(String ifaceName)
	{
		boolean found = m_ac.containsBeanDefinition(ifaceName);

		return found;
	}

	/**
	 * {@inheritDoc}
	 */
	public Set getRegisteredInterfaces()
	{
		Set rv = new HashSet();

		// get the registered ones
		String[] names = m_ac.getBeanDefinitionNames();
		for (int i = 0; i < names.length; i++)
		{
			rv.add(names[i]);
		}

		// add the loaded ones
		for (Iterator iLoaded = m_loadedComponents.iterator(); iLoaded.hasNext();)
		{
			String loaded = (String) iLoaded.next();
			rv.add(loaded);
		}

		return rv;
	}

	/**
	 * {@inheritDoc}
	 */
	public void close()
	{
		m_hasBeenClosed = true;
		m_ac.close();
	}

	/**
	 * {@inheritDoc}
	 */
	public void loadComponent(Class iface, Object component)
	{
		// Spring doesn't list these in getBeanDefinitionNames, so we keep track
		m_loadedComponents.add(iface.getName());

		m_ac.getBeanFactory().registerSingleton(iface.getName(), component);
	}

	/**
	 * {@inheritDoc}
	 */
	public void loadComponent(String ifaceName, Object component)
	{
		// Spring doesn't list these in getBeanDefinitionNames, so we keep track
		m_loadedComponents.add(ifaceName);

		m_ac.getBeanFactory().registerSingleton(ifaceName, component);
	}

	/**
	 * Locate the component loader, and load any available components.
	 */
	protected void loadComponents()
	{
		ComponentsLoader loader = new ComponentsLoader();

		// locate the components root
		// if we have our system property set, use it
		String componentsRoot = System.getProperty(SAKAI_COMPONENTS_ROOT_SYS_PROP);
		if (componentsRoot == null)
		{
			// if we are in Catalina, place it at ${catalina.home}/components/
			String catalina = getCatalina();
			if (catalina != null)
			{
				componentsRoot = catalina + File.separatorChar + "components" + File.separatorChar;
			}
		}

		if (componentsRoot == null)
		{
			M_log.warn("loadComponents: cannot estabish a root directory for the components packages");
			return;
		}

		// make sure this is set
		System.setProperty(SAKAI_COMPONENTS_ROOT_SYS_PROP, componentsRoot);

		// load components
		loader.load(this, componentsRoot);
	}

	/**
	 * Increment the count of ACs that call this one parent.
	 */
	public synchronized void addChildAc()
	{
		m_childCount++;
	}

	/**
	 * Decrement the count of ACs that call this one parent.
	 */
	public synchronized void removeChildAc()
	{
		m_childCount--;

		// if we are not using the shutdown hook, close() when the m_childCount == 0
		if ((m_childCount == 0) && (System.getProperty(CLOSE_ON_SHUTDOWN) == null))
		{
			close();
		}
	}

	/**
	 * Check the environment for catalina's base or home directory.
	 * 
	 * @return Catalina's base or home directory.
	 */
	protected String getCatalina()
	{
		String catalina = System.getProperty("catalina.base");
		if (catalina == null)
		{
			catalina = System.getProperty("catalina.home");
		}

		return catalina;
	}

	/**
	 * @inheritDoc
	 */
	public Properties getConfig()
	{
		return m_config;
	}

	/**
	 * @inheritDoc
	 */
	public void waitTillConfigured()
	{
		// Nothing really to do - the cover takes care of this -ggolden
	}

	/**
	 * @inheritDoc
	 */
	public boolean hasBeenClosed()
	{
		return m_hasBeenClosed;
	}
}
