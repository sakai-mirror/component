/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
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

/**********************************************************************************
 * code modified from: org.springframework.beans.factory.config.PropertyOverrideConfigurer from Spring 1.1.5
 *
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyResourceConfigurer;

/**
 * <p>
 * Sakai's extension to the Spring PropertyOverrideConfigurer - allow our dotted bean ids, use @ as a separator between the bean id and the property name.
 * </p>
 * <p>
 * This could be an extension, just defining a new processKey(), but for the *private* members that the extension does not have access to...
 * </p>
 */
public class PropertyOverrideConfigurer extends PropertyResourceConfigurer
{
	/** Our logger. */
	private static Log M_log = LogFactory.getLog(PropertyOverrideConfigurer.class);

	protected boolean ignoreInvalidKeys = false;

	/** Contains names of beans that have overrides */
	protected Set<String> beanNames = Collections.synchronizedSet(new HashSet<String>());

	/** Here are all the name=value entries we read in. */
	protected Map<String, String> m_entries = new HashMap<String, String>();

	/**
	 * Access the value of the entry with this name key
	 * 
	 * @param name
	 *        The key to search for.
	 * @return The value for this key, or null if not defined.
	 */
	public String getValue(String name)
	{
		return (String) m_entries.get(name);
	}

	/**
	 * Set whether to ignore invalid keys. Default is false.
	 * <p>
	 * If you ignore invalid keys, keys that do not follow the 'beanName.property' format will just be logged as warning. This allows to have arbitrary other keys in a properties file.
	 */
	public void setIgnoreInvalidKeys(boolean ignoreInvalidKeys)
	{
		this.ignoreInvalidKeys = ignoreInvalidKeys;
	}

	protected void processProperties(ConfigurableListableBeanFactory beanFactory, Properties props) throws BeansException
	{
		for (Enumeration en = props.propertyNames(); en.hasMoreElements();)
		{
			String key = (String) en.nextElement();

			String value = props.getProperty(key);
			m_entries.put(key, value);

			try
			{
				processKey(beanFactory, key, value);
			}
			catch (BeansException ex)
			{
				// String msg = "Could not process key [" + key + "] in PropertyOverrideConfigurer";
				// if (this.ignoreInvalidKeys)
				// {
				// if (M_log.isDebugEnabled())
				// {
				// M_log.debug(msg + ": " + ex.getMessage());
				// }
				// else if (M_log.isWarnEnabled())
				// {
				// M_log.warn(msg + ": " + ex.getMessage());
				// }
				// }
				// else
				// {
				// throw new BeanInitializationException(msg, ex);
				// }
			}
		}
	}

	/**
	 * Process the given key as 'beanName@property' entry.
	 */
	protected void processKey(ConfigurableListableBeanFactory factory, String key, String value) throws BeansException
	{
		int atIndex = key.indexOf('@');
		if (atIndex == -1)
		{
			throw new BeanInitializationException("Invalid key [" + key + "]: expected 'property@beanName'");
		}
		String beanProperty = key.substring(0, atIndex);
		String beanName = key.substring(atIndex + 1);
		this.beanNames.add(beanName);
		applyPropertyValue(factory, beanName, beanProperty, value);
		if (M_log.isDebugEnabled())
		{
			// if there's a password in the key, let's not display the value
			if (key.indexOf("password") != -1)
			{
				M_log.debug("Property '" + key + "' set to [***]");
			}
			else
			{
				M_log.debug("Property '" + key + "' set to [" + value + "]");
			}
		}
	}

	/**
	 * Apply the given property value to the corresponding bean.
	 */
	protected void applyPropertyValue(ConfigurableListableBeanFactory factory, String beanName, String property, String value)
	{
		BeanDefinition bd = factory.getBeanDefinition(beanName);
		bd.getPropertyValues().addPropertyValue(property, value);
	}

	/**
	 * Were there overrides for this bean? Only valid after processing has occurred at least once.
	 * 
	 * @param beanName
	 *        name of the bean to query status for
	 * @return whether there were property overrides for the named bean
	 */
	public boolean hasPropertyOverridesFor(String beanName)
	{
		return this.beanNames.contains(beanName);
	}
}
