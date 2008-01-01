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

package org.sakaiproject.util;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author ieb
 */
public class SakaiComponentApplicationContextX extends GenericApplicationContext
{

	private Map<String, SakaiComponentApplicationContextX> componentPackages;

	private Object sakaiStartupShutdownMonitor = new Object();

	/**
	 * 
	 */
	public SakaiComponentApplicationContextX()
	{
	}

	/**
	 * @param beanFactory
	 */
	public SakaiComponentApplicationContextX(DefaultListableBeanFactory beanFactory)
	{
		super(beanFactory);
	}

	/**
	 * @param parent
	 */
	public SakaiComponentApplicationContextX(ApplicationContext parent)
	{
		super(parent);
	}

	/**
	 * @param beanFactory
	 * @param parent
	 */
	public SakaiComponentApplicationContextX(DefaultListableBeanFactory beanFactory,
			ApplicationContext parent)
	{
		super(beanFactory, parent);
	}

	/**
	 * We have to ensure that the child contexts are prepared for refrech before
	 * attempting to refresh
	 * 
	 * @see org.springframework.context.support.AbstractApplicationContext#refresh()
	 */
	@Override
	public void refresh() throws BeansException, IllegalStateException
	{
		ClassLoader contextCL = Thread.currentThread().getContextClassLoader();
		try
		{
			SakaiComponentApplicationContextX[] contexts = componentPackages.values()
					.toArray(new SakaiComponentApplicationContextX[0]);
			ConfigurableListableBeanFactory[] beanFactories = new ConfigurableListableBeanFactory[contexts.length];
			Thread.currentThread()
					.setContextClassLoader(this.getClass().getClassLoader());
			ConfigurableListableBeanFactory beanFactory = prepareForRefresh();
			Thread.currentThread().setContextClassLoader(contextCL);

			if (componentPackages != null)
			{
				for (int i = 0; i < contexts.length; i++)
				{
					Thread.currentThread().setContextClassLoader(
							contexts[i].getClass().getClassLoader());
					beanFactories[i] = contexts[i].prepareForRefresh();
					Thread.currentThread().setContextClassLoader(contextCL);
				}
			}
			try
			{
				Thread.currentThread().setContextClassLoader(
						this.getClass().getClassLoader());
				performRefresh(beanFactory);
				Thread.currentThread().setContextClassLoader(contextCL);
				if (componentPackages != null)
				{
					for (int i = 0; i < contexts.length; i++)
					{
						Thread.currentThread().setContextClassLoader(
								contexts[i].getClass().getClassLoader());
						contexts[i].performRefresh(beanFactories[i]);
						Thread.currentThread().setContextClassLoader(contextCL);
					}
				}
				Thread.currentThread().setContextClassLoader(
						this.getClass().getClassLoader());
				completeRefresh(beanFactory);
				Thread.currentThread().setContextClassLoader(contextCL);
				if (componentPackages != null)
				{
					for (int i = 0; i < contexts.length; i++)
					{
						Thread.currentThread().setContextClassLoader(
								contexts[i].getClass().getClassLoader());
						contexts[i].completeRefresh(beanFactories[i]);
						Thread.currentThread().setContextClassLoader(contextCL);
					}
				}
			}
			catch (BeansException bex)
			{
				logger.error("Failed to Refresh context, will destroy all beans ",bex);
				Thread.currentThread().setContextClassLoader(
						this.getClass().getClassLoader());
				abandonRefresh(beanFactory);
				Thread.currentThread().setContextClassLoader(contextCL);
				if (componentPackages != null)
				{
					for (int i = 0; i < contexts.length; i++)
					{
						Thread.currentThread().setContextClassLoader(
								contexts[i].getClass().getClassLoader());
						contexts[i].abandonRefresh(beanFactories[i]);
						Thread.currentThread().setContextClassLoader(contextCL);
					}
				}
				throw bex;
			}
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(contextCL);
		}

	}

	/**
	 * @param beanFactory
	 */
	private void abandonRefresh(ConfigurableListableBeanFactory beanFactory)
	{
		beanFactory.destroySingletons();
	}

	/**
	 * prepare for refresh, but dont do it.
	 */
	protected ConfigurableListableBeanFactory prepareForRefresh()
	{
		synchronized (this.sakaiStartupShutdownMonitor)
		{
			// Prepare this context for refreshing.
			prepareRefresh();

			// Tell the subclass to refresh the internal bean factory.
			ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

			// Prepare the bean factory for use in this context.
			prepareBeanFactory(beanFactory);

			return beanFactory;
		}
	}

	/**
	 * @param factory
	 */
	private void performRefresh(ConfigurableListableBeanFactory beanFactory)
			throws BeansException
	{
		synchronized (this.sakaiStartupShutdownMonitor)
		{
			// Allows post-processing of the bean factory in context
			// subclasses.
			postProcessBeanFactory(beanFactory);

			// Invoke factory processors registered as beans in the context.
			invokeBeanFactoryPostProcessors(beanFactory);

			// Register bean processors that intercept bean creation.
			registerBeanPostProcessors(beanFactory);

			// Initialize message source for this context.
			initMessageSource();

			// Initialize event multicaster for this context.
			initApplicationEventMulticaster();

			// Initialize other special beans in specific context
			// subclasses.
			onRefresh();

			// Check for listener beans and register them.
			registerListeners();

			// Instantiate singletons this late to allow them to access the
			// message source.
			beanFactory.preInstantiateSingletons();

			// Last step: publish corresponding event.
			publishEvent(new ContextRefreshedEvent(this));
		}

	}

	/**
	 * @param factory
	 */
	private void completeRefresh(ConfigurableListableBeanFactory beanFactory)
	{
		synchronized (this.sakaiStartupShutdownMonitor)
		{
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.context.support.AbstractApplicationContext#close()
	 */
	@Override
	public void close()
	{
		synchronized (this.sakaiStartupShutdownMonitor)
		{
			super.close();
		}
	}

	/**
	 * @param componentPackages
	 */
	public void setComponentPackages(
			Map<String, SakaiComponentApplicationContextX> componentPackages)
	{
		this.componentPackages = componentPackages;

	}

}
