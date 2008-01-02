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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.aop.target.dynamic.BeanFactoryRefreshableTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.Resource;

/**
 * For the most part, the ContainerApplicationContext /
 * ComponentApplicationContext relationship behaves like a normal Spring
 * parent-child hierarchy. The major differences are:
 * <ul>
 * <li> After (or while) loading its own bean definitions, the child adds
 * appropriate proxy definitions to the parent. This lets the child expose
 * service implementations.
 * <li> Before bean post-processing, the child adds parent-managed
 * post-processors to its post-processor list. This allows centralized
 * configuration.
 * <li> Because most Sakai components aren't truly independent contexts,
 * refreshing a component may call for beans being loaded from an
 * as-yet-unrefreshed component context. To handle this, the normal "refresh"
 * cycle is broken into three parts and implicit partial refresh is supported.
 * </ul>
 * Although the usual refresh method is split up to hook into container-wide
 * initialization, all required extra tasks will also still take effect in a
 * normal call to "refresh()". The intent is to provide at least some support
 * for reloading or addition of components after container initialization.
 */
public class ComponentApplicationContext extends AbstractXmlApplicationContext
{
	private static Log log = LogFactory.getLog(ComponentApplicationContext.class);

	public static final String COMPONENT_CONTEXTS_PROPERTY = "sakaiComponentContexts";

	public static final String COMPONENT_CONTEXTS_NONE = "none";

	public static final String COMPONENT_CONTEXTS_ALL = "all";

	public static final String COMPONENT_CONTEXTS_AUTO = "auto";

	public static final String COMPONENT_CONTEXTS_EXPLICIT = "explicit";

	public static enum ComponentContexts {
		NONE, ALL, EXPLICIT, AUTO
	};

	private Resource[] configResources;

	private boolean postProcessorsRefreshed = false;

	private final Object postProcessorsRefreshedMonitor = new Object();

	private String componentName;

	public static ComponentContexts getComponentContextsOption()
	{
		String property = System.getProperty(COMPONENT_CONTEXTS_PROPERTY,
				COMPONENT_CONTEXTS_ALL);
		ComponentContexts componentContextsOption = ComponentContexts.ALL;
		if (property.equalsIgnoreCase(COMPONENT_CONTEXTS_NONE))
		{
			componentContextsOption = ComponentContexts.NONE;
		}
		else if (property.equalsIgnoreCase(COMPONENT_CONTEXTS_ALL))
		{
			componentContextsOption = ComponentContexts.ALL;
		}
		else if (property.equalsIgnoreCase(COMPONENT_CONTEXTS_EXPLICIT))
		{
			componentContextsOption = ComponentContexts.EXPLICIT;
		}
		else if (property.equals(COMPONENT_CONTEXTS_AUTO))
		{
			componentContextsOption = ComponentContexts.AUTO;
		}
		return componentContextsOption;
	}

	public ComponentApplicationContext(String name, ConfigurableApplicationContext parent,
			ClassLoader componentClassLoader, Resource[] configResources)
	{
		super(parent);
		componentName = name;
		setClassLoader(componentClassLoader);
		this.configResources = configResources;
	}

	/**
	 * This is the first part of the normal "refresh()" cycle, plus a special
	 * step to export service (or what might be service) proxies to the parent
	 * context. When we support explicit declaration of services, that special
	 * step can be eliminated.
	 */
	public void refreshBeanDefinitions()
	{
		prepareRefresh();
		obtainFreshBeanFactory();
		exportAllBeanDefinitions();
	}

	public void refreshBeanDefinitionsNoExport()
	{
		prepareRefresh();
		obtainFreshBeanFactory();
	}

	/**
	 * This is the second part of the normal "refresh()" cycle, taking us from
	 * the point at which bean definitions have been loaded to the point right
	 * before non-lazy-init singletons are instantiated.
	 */
	public void refreshPostProcessors()
	{
		synchronized (postProcessorsRefreshedMonitor)
		{
			if (!postProcessorsRefreshed)
			{
				// Head off infinite regress in case of internal calls to
				// "getBean(name)".
				postProcessorsRefreshed = true;

				ConfigurableListableBeanFactory beanFactory = getBeanFactory();

				// Prepare the bean factory for use in this context.
				prepareBeanFactory(beanFactory);

				try
				{
					// Allows post-processing of the bean factory in context
					// subclasses.
					postProcessBeanFactory(beanFactory);

					// Invoke factory processors registered as beans in the
					// context.
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
				}
				catch (BeansException ex)
				{
					// Destroy already created singletons to avoid dangling
					// resources.
					beanFactory.destroySingletons();
					throw ex;
				}
			}
		}
	}

	/**
	 * This is the third and last part of the normal "refresh()" cycle,
	 * instantiating non-lazy-init singletons and publishing a context-refreshed
	 * event.
	 */
	public void refreshSingletons()
	{
		ConfigurableListableBeanFactory beanFactory = getBeanFactory();
		try
		{
			// Instantiate singletons this late to allow them to access the
			// message source.
			beanFactory.preInstantiateSingletons();

			// Last step: publish corresponding event.
			publishEvent(new ContextRefreshedEvent(this));
		}
		catch (BeansException ex)
		{
			// Destroy already created singletons to avoid dangling resources.
			beanFactory.destroySingletons();
			throw ex;
		}
	}

	@Override
	protected void initBeanDefinitionReader(XmlBeanDefinitionReader beanDefinitionReader)
	{
		beanDefinitionReader.setBeanClassLoader(getClassLoader());
	}

	/**
	 * Spring opens this method for overriding to allow registration of special
	 * BeanPostProcessors such as those managed by the container context.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
	{
		// Add container's post-processors (AKA "sakai.properties").
		AbstractApplicationContext parentContext = (AbstractApplicationContext) getParent();
		for (BeanFactoryPostProcessor parentPostProcesssor : (List<BeanFactoryPostProcessor>) parentContext
				.getBeanFactoryPostProcessors())
		{
			addBeanFactoryPostProcessor(parentPostProcesssor);
		}

		super.postProcessBeanFactory(beanFactory);
	}

	/**
	 * Service-level dependencies may pull in beans before their component
	 * contexts have been refreshed. Guard against that here.
	 */
	@Override
	public Object getBean(String name) throws BeansException
	{
		synchronized (postProcessorsRefreshedMonitor)
		{
			if (!postProcessorsRefreshed)
			{
				refreshPostProcessors();
			}
		}
		return super.getBean(name);
	}

	protected void exportAllBeanDefinitions()
	{
		DefaultListableBeanFactory containerBeanFactory = (DefaultListableBeanFactory) getInternalParentBeanFactory();

		// All-or-nothing proxying of services. Every single bean
		// definition in the component factory gets exported as
		// a globally-visible auto-interface-detection proxy definition,
		// whether that's the intended behavior or not. Similar
		// proxies could be managed more predictably by a number of
		// mechanisms -- this noisy approach is just a proof of concept.
		StringBuilder sb = new StringBuilder();
		String[] componentBeanDefinitionNames = getBeanFactory().getBeanDefinitionNames();
		for (int i = 0; i < componentBeanDefinitionNames.length; i++)
		{
			String componentBeanId = componentBeanDefinitionNames[i];
			/*
			 * <bean id="COMPONENT_BEAN_ID"
			 * class="org.springframework.aop.framework.ProxyFactoryBean">
			 * <property name="autodetectInterfaces" value="true"/> <property
			 * name="targetSource" <bean
			 * class="BeanFactoryRefreshableTargetSource"> <constructor-arg
			 * value="COMPONENT_BEAN_FACTORY"/> <constructor-arg
			 * value="COMPONENT_BEAN_ID"/> </bean> </property> </bean>
			 */
			if (true)
			{
				BeanDefinition beanDefinition = getBeanFactory().getBeanDefinition(componentBeanId);
				containerBeanFactory.registerBeanDefinition(componentBeanId, beanDefinition);
				sb.append("\n\tregisterBeanDefinition ").append(componentBeanId).append(":").append(beanDefinition);
				String[] aliases = getAliases(componentBeanId);
				for ( String alias : aliases ) {
					if ( !componentBeanId.equals(alias) ) {
						sb.append(",as ").append(alias);
						containerBeanFactory.registerAlias(componentBeanId, alias);						
					}
				}
			}
			else
			{
				BeanDefinitionBuilder proxyBuilder = BeanDefinitionBuilder
						.rootBeanDefinition(ProxyFactoryBean.class.getName());
				proxyBuilder.addPropertyValue("autodetectInterfaces", "true");
				BeanDefinitionBuilder targetBuilder = BeanDefinitionBuilder
						.rootBeanDefinition(BeanFactoryRefreshableTargetSource.class
								.getName());
				targetBuilder.addConstructorArg(this);
				targetBuilder.addConstructorArg(componentBeanId);
				proxyBuilder.addPropertyValue("targetSource", targetBuilder
						.getBeanDefinition());
				proxyBuilder.setLazyInit(true);
				containerBeanFactory.registerBeanDefinition(componentBeanId, proxyBuilder
						.getBeanDefinition());
				BeanDefinition beanDefinition = proxyBuilder.getBeanDefinition();
				log.info("registerBeanDefinition "+componentBeanId+":" + beanDefinition);

			}
		}
		log.info("Exported "+sb.toString());

		// To preserve current behavior as much as possible, we should also copy
		// component aliases (including those defined by the "name" attribute),
		// but
		// there's no easy way to get at that list.
	}

	protected Resource[] getConfigResources()
	{
		return configResources;
	}

	/**
	 * @param beanName
	 * @return
	 */
	public boolean exportBeanDefinition(String beanName)
	{
		DefaultListableBeanFactory containerBeanFactory = (DefaultListableBeanFactory) getInternalParentBeanFactory();
		String[] beanNames = getBeanDefinitionNames();
		for (String localBeanName : beanNames)
		{
			if (beanName.equals(localBeanName))
			{
				BeanDefinition beanDefinition = getBeanFactory().getBeanDefinition(beanName);
				containerBeanFactory.registerBeanDefinition(beanName, beanDefinition);
				log.info("registerBeanDefinition "+beanName+":" + beanDefinition);
				return true;
			}
		}
		return false;
	}

	/**
	 * @return
	 */
	public String getComponentName()
	{
		return componentName;
	}

}
