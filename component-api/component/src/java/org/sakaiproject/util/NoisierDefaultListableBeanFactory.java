/**********************************************************************************
 *
 * $Id$
 *
 ***********************************************************************************
 *
 * Copyright (c) 2007 The Regents of the University of California
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

package org.sakaiproject.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ComponentBeanFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * In Spring 1.2.6 and 1.2.8, when there's a problem pre-instantiating any
 * singleton bean, all existing singleton beans are immediately destroyed.
 * Unfortunately for developers, this destruction occurs without any
 * explanation: neither the problematic bean's name nor the error stack is
 * logged. This subclasses the default bean factory to log the failing bean's
 * name and a stack trace of the exception.
 */
public class NoisierDefaultListableBeanFactory extends DefaultListableBeanFactory
		implements ComponentBeanFactory
{

	private Map<String, ComponentBeanFactory> defaultListableBeanFactories;

	private Map<String, Object> exportedBeans;
	
	private Map<String, Object> externalReferences = new ConcurrentHashMap<String, Object>();

	public void preInstantiateSingletons() throws BeansException
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Pre-instantiating singletons in factory [" + this + "]");
		}

		// The superclass's variable by this name is declared private.
		String[] beanDefinitionNames = getBeanDefinitionNames();
		String beanName = null; // Remember in case of an exception
		try
		{
			// for (Iterator it = this.beanDefinitionNames.iterator();
			// it.hasNext();) {
			for (int i = 0; i < beanDefinitionNames.length; i++)
			{
				beanName = beanDefinitionNames[i];

				if (!containsSingleton(beanName) && containsBeanDefinition(beanName))
				{
					RootBeanDefinition bd = getMergedBeanDefinition(beanName, false);
					if (!bd.isAbstract() && bd.isSingleton() && !bd.isLazyInit())
					{
						if (bd.hasBeanClass()
								&& FactoryBean.class.isAssignableFrom(bd.getBeanClass()))
						{
							FactoryBean factory = (FactoryBean) getBean(FACTORY_BEAN_PREFIX
									+ beanName);
							if (factory.isSingleton())
							{
								getBean(beanName);
							}
						}
						else
						{
							getBean(beanName);
						}
					}
				}
			}
		}
		catch (BeansException ex)
		{
			// Destroy already created singletons to avoid dangling resources.
			logger.error("Failed to preinstantiate the singleton named " + beanName
					+ ". Destroying all Spring beans.", ex);
			try
			{
				destroySingletons();
			}
			catch (Throwable ex2)
			{
				logger.error("Pre-instantiating singletons failed, "
						+ "and couldn't destroy already created singletons", ex2);
			}
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.beans.factory.support.AbstractBeanFactory#getBean(java.lang.String)
	 */
	@Override
	public Object getBean(String name) throws BeansException
	{
		try
		{
			if (containsBean(name))
			{
				return getLocalBean(name);
			}
		}
		catch (BeansException nsbde)
		{
		}

		Object bean = exportedBeans.get(name);
		if (bean != null)
		{
			logger.warn("Bean " + name + " has been exported ");
			externalReferences.put(name, bean);
			return bean;
		}

		for (ComponentBeanFactory bf : defaultListableBeanFactories.values())
		{
			if (bf.containsBean(name))
			{
				try
				{
					bean = bf.getLocalBean(name);
					logger.warn("Bean " + name + " Exported from " + bf);
					exportedBeans.put(name, bean);
					externalReferences.put(name, bean);
					return bean;

				}
				catch (BeansException nsbde2)
				{
					logger.warn("Failed to get Bean " + name, nsbde2);
				}
			}
		}
		throw new NoSuchBeanDefinitionException(name);
	}

	/**
	 * @param arg0
	 * @return
	 */
	public Object getLocalBean(String arg0)
	{
		return super.getBean(arg0);

	}

	/**
	 * @return the defaultListableBeanFactories
	 */
	public Map<String, ComponentBeanFactory> getDefaultListableBeanFactories()
	{
		return defaultListableBeanFactories;
	}

	/**
	 * @param defaultListableBeanFactories
	 *        the defaultListableBeanFactories to set
	 */
	public void setDefaultListableBeanFactories(
			Map<String, ComponentBeanFactory> defaultListableBeanFactories)
	{
		this.defaultListableBeanFactories = defaultListableBeanFactories;
	}

	/**
	 * @return the exportedBeans
	 */
	public Map<String, Object> getExportedBeans()
	{
		return exportedBeans;
	}

	/**
	 * @param exportedBeans
	 *        the exportedBeans to set
	 */
	public void setExportedBeans(Map<String, Object> exportedBeans)
	{
		this.exportedBeans = exportedBeans;
	}

}
