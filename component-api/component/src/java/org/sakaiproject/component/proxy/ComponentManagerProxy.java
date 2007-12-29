/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 The Sakai Foundation.
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

package org.sakaiproject.component.proxy;

import java.lang.management.ManagementFactory;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.api.ComponentManagerNotAvailableException;
import org.sakaiproject.util.SpringComponentManager;

/**
 * This bean provides access to the component manager, users that need acess to
 * the component manager should create an instance of this bean and it will
 * connect to the ComponentManager.
 * 
 * @author ieb
 */
public class ComponentManagerProxy implements ComponentManager
{

	/**
	 * The component manager we are connecting to
	 */
	private ComponentManager componentManager;

	/**
	 * Start the component manager proxy. If the proxy cant find the parent
	 * component manager it will throw the ComponentManagerNotAvailable
	 * Exception
	 * 
	 * @throws ComponentManagerNotAvailableException
	 */
	public ComponentManagerProxy() throws ComponentManagerNotAvailableException
	{
		try
		{
			// the following sequence takes about 1ns to execute.
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName componentManagerName = new ObjectName(MBEAN_COMPONENT_MANAGER);
			componentManager = (ComponentManager) mbs.invoke(componentManagerName, "getComponentManager", null, null);

//			componentManager = (ComponentManager) mbs.getAttribute(componentManagerName,
//					"ComponentManager");
		}
		catch (Exception ex)
		{
			throw new ComponentManagerNotAvailableException(
					"Failed to get Component Manager ", ex);
		}

	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#close()
	 */
	public void close()
	{
		componentManager.close();
	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#contains(java.lang.Class)
	 */
	public boolean contains(Class iface)
	{
		return componentManager.contains(iface);
	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#contains(java.lang.String)
	 */
	public boolean contains(String ifaceName)
	{
		return componentManager.contains(ifaceName);
	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#get(java.lang.Class)
	 */
	public Object get(Class iface)
	{
		return componentManager.get(iface);
	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#get(java.lang.String)
	 */
	public Object get(String ifaceName)
	{
		return componentManager.get(ifaceName);
	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#getConfig()
	 */
	public Properties getConfig()
	{
		return componentManager.getConfig();
	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#getRegisteredInterfaces()
	 */
	public Set getRegisteredInterfaces()
	{
		return componentManager.getRegisteredInterfaces();
	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#hasBeenClosed()
	 */
	public boolean hasBeenClosed()
	{
		return componentManager.hasBeenClosed();
	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#loadComponent(java.lang.Class,
	 *      java.lang.Object)
	 */
	public void loadComponent(Class iface, Object component)
	{
		componentManager.loadComponent(iface, component);
	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#loadComponent(java.lang.String,
	 *      java.lang.Object)
	 */
	public void loadComponent(String ifaceName, Object component)
	{
		componentManager.loadComponent(ifaceName, component);
	}

	/**
	 * 
	 * @see org.sakaiproject.component.api.ComponentManager#waitTillConfigured()
	 */
	public void waitTillConfigured()
	{
		componentManager.waitTillConfigured();
	}

	/**
	 * @return
	 */
	public ComponentManager getComponentManager()
	{
		return componentManager;
	}

}
