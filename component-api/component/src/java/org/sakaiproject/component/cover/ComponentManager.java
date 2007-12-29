/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006 The Sakai Foundation.
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

package org.sakaiproject.component.cover;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * ComponentManager is a static Cover for the
 * {@link org.sakaiproject.component.api.ComponentManager Component Manager};
 * see that interface for usage details.
 * </p>
 * <p>
 * This cover is special. As a cover for the component manager, it cannot use
 * the component manager to find the instance. Instead, this is where a static
 * single-instance singleton ComponentManger of a particular type is created.
 * </p>
 */
public class ComponentManager
{
	/** A component manager - use the Spring based one. */
	private static org.sakaiproject.component.api.ComponentManager m_componentManager = null;

	/**
	 * If true, covers will cache the components they find once - good for
	 * production, bad for some unit testing.
	 */
	public static final boolean CACHE_COMPONENTS = true;

	private static final Log log = LogFactory.getLog(ComponentManager.class);

	public static java.lang.String SAKAI_COMPONENTS_ROOT_SYS_PROP = org.sakaiproject.component.api.ComponentManager.SAKAI_COMPONENTS_ROOT_SYS_PROP;

	private static Object m_syncObj = new Object();

	/**
	 * Access the component manager of the single instance.
	 * 
	 * @return The ComponentManager.
	 */
	public static org.sakaiproject.component.api.ComponentManager getInstance()
	{
		try
		{

			// the following sequence takes about 1ns to execute.
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName componentManager = new ObjectName(
					org.sakaiproject.component.api.ComponentManager.MBEAN_COMPONENT_MANAGER);
			
			m_componentManager = (org.sakaiproject.component.api.ComponentManager) mbs.invoke(componentManager, "getComponentManager", null, null);
			/*
			 m_componentManager = (org.sakaiproject.component.api.ComponentManager) mbs
					.getAttribute(componentManager, "ComponentManager");
					*/
		}
		catch (Exception ex)
		{
			log.error("Unable to get reference to component Manager ", ex);
		}
		log.info("Got ComponentManager as "+m_componentManager);
		return m_componentManager;
	}

	public static Object get(Class iface)
	{
		return getInstance().get(iface);
	}

	public static Object get(String ifaceName)
	{
		return getInstance().get(ifaceName);
	}

	public static boolean contains(Class iface)
	{
		return getInstance().contains(iface);
	}

	public static boolean contains(String ifaceName)
	{
		return getInstance().contains(ifaceName);
	}

	public static Set getRegisteredInterfaces()
	{
		return getInstance().getRegisteredInterfaces();
	}

	public static void loadComponent(Class iface, Object component)
	{
		getInstance().loadComponent(iface, component);
	}

	public static void loadComponent(String ifaceName, Object component)
	{
		getInstance().loadComponent(ifaceName, component);
	}

	public static void close()
	{
		getInstance().close();
	}

	public static java.util.Properties getConfig()
	{
		return getInstance().getConfig();
	}

	public static void waitTillConfigured()
	{
		getInstance();
	}

	public static boolean hasBeenClosed()
	{
		return getInstance().hasBeenClosed();
	}
}
