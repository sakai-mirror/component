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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * This is just a version of Spring's PropertyOverrideConfigurer that lets the
 * bean name be specified Sakai-style before the property name instead of after.
 * (For example "myBean@the.property" instead of "the.property@myBean".)
 */
public class ReversiblePropertyOverrideConfigurer extends PropertyOverrideConfigurer {
	private static Log log = LogFactory.getLog(ReversiblePropertyOverrideConfigurer.class);
	private boolean beanNameAtEnd = true;
	private String beanNameSeparator;	// Private in the superclass, so we need to stash our own copy.

	protected void processKey(ConfigurableListableBeanFactory factory, String key, String value)
	throws BeansException {
		if (this.beanNameAtEnd) {
			int separatorIndex = key.indexOf(this.beanNameSeparator);
			if (separatorIndex >= 0) {
				String beanProperty = key.substring(0, separatorIndex);
				String beanName = key.substring(separatorIndex+1);
				key = beanName + this.beanNameSeparator + beanProperty;
			}
			super.processKey(factory, key, value);
		}
	}

	/**
	 * A legal PropertyOverrideConfigurer property must be
	 * specified differently for proxies than for target objects.
	 *
	 *   object.testMode@org.sakaiproject.email.api.EmailService=true
	 *
	 * not
	 *
	 *   testMode@org.sakaiproject.email.api.EmailService=true
	 *
	 * To support the proof-of-concept everything-proxied
	 * duplicate-objects-for-every-service-ID approach,
	 * avoid setting properties against component-created proxies and instead count on
	 * component contexts to apply SakaiProperties overriders locally.
	 *
	 * When we implement explicitly declared service proxies, we
	 * can decide which bean to target on a case by case basis,
	 * eliminating this override.
	 */
	@Override
	protected void applyPropertyValue(ConfigurableListableBeanFactory factory,
			String beanName, String property, String value) {
		BeanDefinition bd = factory.getBeanDefinition(beanName);
		String className = bd.getBeanClassName();
		if ((className != null) && className.equals(ProxyFactoryBean.class.getName())) {
			MutablePropertyValues mpv = bd.getPropertyValues();
			PropertyValue targetSourceProperty = mpv.getPropertyValue("targetSource");
			if (targetSourceProperty != null) {
				Object targetRoot = targetSourceProperty.getValue();
				if (targetRoot instanceof AbstractBeanDefinition) {
					String targetClassName = ((AbstractBeanDefinition)targetRoot).getBeanClassName();
					if (targetClassName.equals("org.springframework.aop.target.dynamic.BeanFactoryRefreshableTargetSource")) {
						return;
					}
				}
			}
		}

		super.applyPropertyValue(factory, beanName, property, value);
	}

	public void setBeanNameSeparator(String beanNameSeparator) {
		this.beanNameSeparator = beanNameSeparator;
		super.setBeanNameSeparator(beanNameSeparator);
	}

	public boolean isBeanNameAtEnd() {
		return beanNameAtEnd;
	}
	public void setBeanNameAtEnd(boolean beanNameAtEnd) {
		this.beanNameAtEnd = beanNameAtEnd;
	}

}
