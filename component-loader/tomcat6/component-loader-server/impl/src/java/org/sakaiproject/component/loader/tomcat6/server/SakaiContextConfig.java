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

package org.sakaiproject.component.loader.tomcat6.server;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.startup.ContextConfig;

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

	private static long permGenUsedStart;

	private static long codeCacheUsedStart;

	private static long edenSpaceUsedStart;

	private static long tenuredGenUsedStart;

	private static long survivorSpaceUsedStart;
	
	static {
		baseLine();
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
			measure();
		}
	}

	/**
	 * 
	 */
	private void measure()
	{
		try
		{
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			System.runFinalization();
			Runtime.getRuntime().gc();

			CompositeData permGen = (CompositeData) mbs.getAttribute(new ObjectName(
					"java.lang:type=MemoryPool,name=Perm Gen"), "Usage");
			CompositeData tenuredGen = (CompositeData) mbs.getAttribute(new ObjectName(
					"java.lang:type=MemoryPool,name=Tenured Gen"), "Usage");
			CompositeData codeCache = (CompositeData) mbs.getAttribute(new ObjectName(
					"java.lang:type=MemoryPool,name=Code Cache"), "Usage");
			CompositeData edenSpace = (CompositeData) mbs.getAttribute(new ObjectName(
					"java.lang:type=MemoryPool,name=Eden Space"), "Usage");
			CompositeData survivorSpace = (CompositeData) mbs.getAttribute(
					new ObjectName("java.lang:type=MemoryPool,name=Survivor Space"),
					"Usage");
			long permGenUsedEnd = Long.parseLong(String.valueOf(permGen.get("used")));
			long codeCacheUsedEnd = Long.parseLong(String.valueOf(codeCache.get("used")));
			long edenSpaceUsedEnd = Long.parseLong(String.valueOf(edenSpace.get("used")));
			long tenuredGenUsedEnd = Long.parseLong(String.valueOf(tenuredGen.get("used")));
			long survivorSpaceUsedEnd = Long.parseLong(String.valueOf(survivorSpace.get("used")));

			log.info("           Permgen Used " + (permGenUsedEnd - permGenUsedStart) / (1024) + " KB");
			log.info("           Code Cache Used " + (codeCacheUsedEnd -codeCacheUsedStart) / (1024)
					+ " KB");
			log.info("           Eden Used " + (edenSpaceUsedEnd - edenSpaceUsedStart) / (1024) + " KB");
			log.info("           Tenured Used " + (tenuredGenUsedEnd - tenuredGenUsedStart) / (1024) + " KB");
			log.info("           Survivour Used " + (survivorSpaceUsedEnd - survivorSpaceUsedStart) / (1024)
					+ " KB");
			
			permGenUsedStart = permGenUsedEnd;
			codeCacheUsedStart = codeCacheUsedEnd;
			edenSpaceUsedStart = edenSpaceUsedEnd;
			tenuredGenUsedStart = tenuredGenUsedEnd;
			survivorSpaceUsedStart = survivorSpaceUsedEnd;
			
		}
		catch (Exception ex)
		{
			log.info("Failed to measure ", ex);
		}

	}

	/**
	 * 
	 */
	private static void baseLine()
	{
		try
		{
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			System.runFinalization();
			Runtime.getRuntime().gc();

			CompositeData permGen = (CompositeData) mbs.getAttribute(new ObjectName(
					"java.lang:type=MemoryPool,name=Perm Gen"), "Usage");
			CompositeData tenuredGen = (CompositeData) mbs.getAttribute(new ObjectName(
					"java.lang:type=MemoryPool,name=Tenured Gen"), "Usage");
			CompositeData codeCache = (CompositeData) mbs.getAttribute(new ObjectName(
					"java.lang:type=MemoryPool,name=Code Cache"), "Usage");
			CompositeData edenSpace = (CompositeData) mbs.getAttribute(new ObjectName(
					"java.lang:type=MemoryPool,name=Eden Space"), "Usage");
			CompositeData survivorSpace = (CompositeData) mbs.getAttribute(
					new ObjectName("java.lang:type=MemoryPool,name=Survivor Space"),
					"Usage");
			permGenUsedStart= Long.parseLong(String.valueOf(permGen.get("used")));
			codeCacheUsedStart = Long.parseLong(String.valueOf(codeCache.get("used")));
			edenSpaceUsedStart = Long.parseLong(String.valueOf(edenSpace.get("used")));
			tenuredGenUsedStart = Long.parseLong(String.valueOf(tenuredGen.get("used")));
			survivorSpaceUsedStart = Long.parseLong(String.valueOf(survivorSpace.get("used")));
			log.info("Taken Baseline ");
		}
		catch (Exception ex)
		{
			log.info("Failed to measure ", ex);
		}

	}

}
