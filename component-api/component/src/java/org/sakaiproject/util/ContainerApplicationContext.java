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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.util.ComponentApplicationContext.ComponentContexts;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.ResourceEntityResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Hook some Sakai-specific operations into the normal ApplicationContext
 * refresh cycle: read component manager configuration files, give creators of
 * PostProcessor objects (e.g., SakaiProperties) a chance to do their work, and
 * load a few central components before the rest.
 */
public class ContainerApplicationContext extends GenericApplicationContext
{

	private static final ComponentContexts COMPONENT_CONTEXT = ComponentApplicationContext
			.getComponentContextsOption();

	private static Log log = LogFactory.getLog(ContainerApplicationContext.class);

	private NoisierDefaultListableBeanFactory nbf;

	private String[] initialSingletonNames;

	private String[] configLocations;

	private Map<String, ComponentApplicationContext> childContexts = new ConcurrentHashMap<String, ComponentApplicationContext>();

	private Map<String, Object> exportedBeans = new ConcurrentHashMap<String, Object>();

	public ContainerApplicationContext()
	{
		super(new NoisierDefaultListableBeanFactory());
	}

	/**
	 * Load component manager configurations. A more normal hook for this is the
	 * "refreshBeanFactory" method, but it's declared final by
	 * GenericApplicationContext.
	 */
	protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory)
	{
		super.prepareBeanFactory(beanFactory);
		try
		{
			loadBeanDefinitions(beanFactory);
		}
		catch (IOException e)
		{
			throw new ApplicationContextException(
					"I/O error parsing XML document for application context ["
							+ getDisplayName() + "]", e);
		}
	}

	protected void loadBeanDefinitions(ConfigurableListableBeanFactory beanFactory)
			throws IOException
	{
		// Create a new XmlBeanDefinitionReader for the given BeanFactory.
		XmlBeanDefinitionReader beanDefinitionReader = new XmlBeanDefinitionReader(
				(BeanDefinitionRegistry) beanFactory);
		beanDefinitionReader.setBeanClassLoader(Thread.currentThread()
				.getContextClassLoader());
		beanDefinitionReader.setResourceLoader(this);
		beanDefinitionReader.setEntityResolver(new ResourceEntityResolver(this));

		if (configLocations != null)
		{
			beanDefinitionReader.loadBeanDefinitions(configLocations);
		}
	}

	/**
	 * Before post-processing, load beans which have declared that they want to
	 * add post-processors dynamically.
	 */
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
	{
		invokePostProcessorCreators(beanFactory);
		super.postProcessBeanFactory(beanFactory);
	}

	/**
	 * Load initial beans before going through the default logic.
	 */
	protected void onRefresh() throws BeansException
	{
		if (initialSingletonNames != null)
		{
			for (int i = 0; i < initialSingletonNames.length; i++)
			{
				getBean(initialSingletonNames[i]);
			}
		}
		super.onRefresh();
	}

	/**
	 * Add bean-created post processors.
	 * 
	 * @param beanFactory
	 */
	public void invokePostProcessorCreators(ConfigurableListableBeanFactory beanFactory)
	{
		String[] postProcessorCreatorNames = beanFactory.getBeanNamesForType(
				BeanFactoryPostProcessorCreator.class, false, false);
		for (int i = 0; i < postProcessorCreatorNames.length; i++)
		{
			BeanFactoryPostProcessorCreator postProcessorCreator = (BeanFactoryPostProcessorCreator) beanFactory
					.getBean(postProcessorCreatorNames[i]);
			for (BeanFactoryPostProcessor beanFactoryPostProcessor : postProcessorCreator
					.getBeanFactoryPostProcessors())
			{
				// Add to the container.
				addBeanFactoryPostProcessor(beanFactoryPostProcessor);
			}
		}
	}

	/**
	 * @param initialSingletonNames
	 *        initial configuration beans to load, behaving as universal
	 *        "dependsOn" targets
	 */
	public void setInitialSingletonNames(String[] initialSingletonNames)
	{
		this.initialSingletonNames = initialSingletonNames;
	}

	/**
	 * @param configLocations
	 *        file paths of XML bean definition files to load before refresh
	 */
	public void setConfigLocations(String[] configLocations)
	{
		this.configLocations = configLocations;
	}

	/**
	 * @param context
	 */
	public void registerChildApplicationContext(String name,
			ComponentApplicationContext context)
	{
		childContexts.put(name, context);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.context.support.GenericApplicationContext#getBeanDefinition(java.lang.String)
	 */
	@Override
	public BeanDefinition getBeanDefinition(String beanName) throws BeansException
	{
		if (COMPONENT_CONTEXT == ComponentContexts.AUTO)
		{
			try
			{
				return getLocalBeanDefinition(beanName);
			}
			catch (BeansException bex)
			{

			}

			if (childContexts != null)
			{
				for (ComponentApplicationContext context : childContexts.values())
				{
					try
					{
						if (context.exportBeanDefinition(beanName))
						{
							return getBeanDefinition(beanName);
						}
					}
					catch (BeansException bex)
					{
					}
				}
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}
		else
		{
			return super.getBeanDefinition(beanName);
		}
	}

	/**
	 * @param beanName
	 * @return
	 */
	private BeanDefinition getLocalBeanDefinition(String beanName)
	{
		return super.getBeanDefinition(beanName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String,
	 *      java.lang.Class)
	 */
	@Override
	public Object getBean(String beanName, Class requiredType) throws BeansException
	{
		if (COMPONENT_CONTEXT == ComponentContexts.AUTO)
		{
			try
			{
				return getLocalBean(beanName, requiredType);
			}
			catch (BeansException bex)
			{

			}

			if (childContexts != null)
			{
				for (ComponentApplicationContext context : childContexts.values())
				{
					if (context.containsBean(beanName))
					{
						try
						{
							Object o = context.getBean(beanName, requiredType);
							exportedBeans.put(beanName, o);
							return o;
						}
						catch (BeansException bex)
						{
						}
					}
				}
			}
			throw new NoSuchBeanDefinitionException(beanName);
		}
		else
		{
			return super.getBean(beanName, requiredType);
		}
	}

	/**
	 * @param beanName
	 * @param requiredType
	 * @return
	 */
	private Object getLocalBean(String beanName, Class requiredType)
	{
		return super.getBean(beanName, requiredType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.context.support.AbstractApplicationContext#getBean(java.lang.String)
	 */
	@Override
	public Object getBean(String beanName) throws BeansException
	{
		if (COMPONENT_CONTEXT == ComponentContexts.AUTO)
		{
			try
			{
				return getLocalBean(beanName);
			}
			catch (BeansException bex)
			{

			}

			if (childContexts != null)
			{
				log.info("Trying child Contexts ");
				for (ComponentApplicationContext context : childContexts.values())
				{
					if (context.containsBean(beanName))
					{
						try
						{

							Object o = context.getBean(beanName);
							exportedBeans.put(beanName, o);
							return o;
						}
						catch (BeansException bex)
						{
							log
									.info("Failed to get Bean of Name " + beanName
											+ " from " + context.getComponentName() + " "
											+ bex.getMessage());
						}
					}
				}
			}
			log.info("No bean found of name " + beanName);
			throw new NoSuchBeanDefinitionException(beanName);
		}
		else
		{
			return super.getBean(beanName);
		}
	}

	/**
	 * @param beanName
	 * @return
	 */
	private Object getLocalBean(String beanName)
	{
		return super.getBean(beanName);
	}

}
