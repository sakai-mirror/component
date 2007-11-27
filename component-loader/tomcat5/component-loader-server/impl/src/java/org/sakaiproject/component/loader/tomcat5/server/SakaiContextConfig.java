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

package org.sakaiproject.component.loader.tomcat5.server;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.startup.ContextConfig;
import org.sakaiproject.component.loader.common.stats.MemoryStats;
import org.sakaiproject.component.loader.common.stats.NewMemoryStats;
import org.sakaiproject.component.loader.common.stats.OldMemoryStats;

/**
 * This class needs to be attached to the Host container inside tomcat, so that
 * it can control the lifecycle of the webapps
 * <pre>
 *  <Host name="localhost" appBase="webapps"
 *      unpackWARs="true" autoDeploy="true"
 *      xmlValidation="false" xmlNamespaceAware="false"
 *      configClass="org.sakaiproject.component.loader.tomcat6.server.SakaiContextConfig"
 *      >
 *
 *
 * </pre>
 * @author ieb
 */
public class SakaiContextConfig extends ContextConfig
{

	private static MemoryStats oldMemoryStats = new OldMemoryStats();
	private static MemoryStats newMemoryStats = new NewMemoryStats();
	
	static {
		oldMemoryStats.baseLine();
		newMemoryStats.baseLine();
	}

	/**
	 * 
	 */
	public SakaiContextConfig()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.catalina.LifecycleListener#lifecycleEvent(org.apache.catalina.LifecycleEvent)
	 */
	public void lifecycleEvent(LifecycleEvent event)
	{
		String type = event.getType();
		super.lifecycleEvent(event);
		
		if (Lifecycle.AFTER_START_EVENT.equals(type))
		{
			log.info(event.getSource()+oldMemoryStats.measure()+newMemoryStats.measure());
		}
	}


}
