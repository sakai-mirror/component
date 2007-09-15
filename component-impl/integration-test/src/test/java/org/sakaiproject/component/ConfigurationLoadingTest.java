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

package org.sakaiproject.component;

import java.net.URL;

import junit.extensions.TestSetup;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.component.test.ITestComponent;
import org.sakaiproject.test.SakaiTestBase;

/**
 *
 */
public class ConfigurationLoadingTest extends SakaiTestBase {
	private static Log log = LogFactory.getLog(ConfigurationLoadingTest.class);
	
	private ServerConfigurationService serverConfigurationService;
	
	public static Test suite() {
		TestSetup setup = new TestSetup(new TestSuite(ConfigurationLoadingTest.class)) {
			protected void setUp() throws Exception {
				initializeSakaiHome();
				try {
					oneTimeSetup();
				} catch (Exception e) {
					log.warn(e);
				}
			}
			protected void tearDown() throws Exception {	
				oneTimeTearDown();
			}
		};
		return setup;
	}
	
	public void setUp() throws Exception {
		serverConfigurationService = (ServerConfigurationService)getService(ServerConfigurationService.class.getName());
	}
	
	public void testSakaiProperties() throws Exception {
		// This test assumes that your "sakai.properties" file sets
		// "loadedTomcatSakaiProperties" to true, doesn't change the
		// default "gatewaySiteId" property, and changes the default value of a
		// TestComponent string property.
		Assert.assertTrue(serverConfigurationService.getString("loadedTomcatSakaiProperties").equals("true"));
		Assert.assertTrue(serverConfigurationService.getString("gatewaySiteId").equals("!gateway"));
		ITestComponent testComponent = (ITestComponent)getService(ITestComponent.class.getName());
		Assert.assertTrue(testComponent.getOverrideString1().equals("nondefault"));
		Assert.assertTrue(testComponent.getPlaceholderString1().equals("nondefault"));
		log.warn("serverId=" + testComponent.getServerId());
		String testBean = (String)getService("org.sakaiproject.component.test.String");
		Assert.assertTrue(testBean.equals("local"));
	}
	
	public static void initializeSakaiHome() {
		URL propertiesUrl = ConfigurationLoadingTest.class.getClassLoader().getResource("sakai.properties");
		if (log.isDebugEnabled()) log.debug("propertiesUrl=" + propertiesUrl);
		if (propertiesUrl != null) {
			String propertiesFileName = propertiesUrl.getFile();
			String sakaiHomeDir = propertiesFileName.substring(0, propertiesFileName.lastIndexOf("sakai.properties") - 1);
			System.setProperty("test.sakai.home", sakaiHomeDir);
		}
	}
}
