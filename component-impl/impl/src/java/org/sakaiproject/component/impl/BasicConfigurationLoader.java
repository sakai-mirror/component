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

package org.sakaiproject.component.impl;

import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ConfigurationLoader;
import org.springframework.core.io.ClassPathResource;

/**
 *
 */
public class BasicConfigurationLoader implements ConfigurationLoader {
	private static Log M_log = LogFactory.getLog(BasicConfigurationLoader.class);
	
	private Properties properties;
	
	public void init() {
		properties = new Properties();
		
		String sakaiHomePath = System.getProperty("sakai.home");

		// check for the security home
		String securityPath = System.getProperty("sakai.security");
		if (securityPath != null)
		{
			// make sure it's properly slashed
			if (!securityPath.endsWith(File.separator)) securityPath = securityPath + File.separatorChar;
			System.setProperty("sakai.security", securityPath);
		}

		// Collect values from all the properties files: the later ones loaded override settings from prior.

		// Start with the distributed defaults from the classpath
		try
		{
			ClassPathResource rsrc = new ClassPathResource("org/sakaiproject/config/sakai.properties");
			if (rsrc.exists())
			{
				properties.load(rsrc.getInputStream());
			}
		}
		catch (Throwable t)
		{
			M_log.warn(t.getMessage(), t);
		}

		// read all the files from the home path that are properties files
		// TODO: not quite yet -ggolden
		// readDirectoryPropertiesFiles(sakaiHomePath);

		// TODO: deprecated placeholder.properties from sakai.home - remove in a later version of Sakai -ggolden
		readPropertyFile(
				sakaiHomePath,
				"placeholder.properties",
				"Deprecated use of placeholder.properties.  This file will not be read in future versions of Sakai.  Merge its content with the sakai.properties file.");

		// these are potentially re-reading, but later wins over earlier, so we assure the order is preserved
		readPropertyFile(sakaiHomePath, "sakai.properties");
		readPropertyFile(sakaiHomePath, "local.properties");

		// add last the security.properties
		readPropertyFile(securityPath, "security.properties");

		// auto-set the server id if missing
		if (!properties.containsKey("serverId"))
		{
			try
			{
				String id = InetAddress.getLocalHost().getHostName();
				properties.put("serverId", id);
			}
			catch (UnknownHostException e)
			{ // empty catch block
				M_log.trace("UnknownHostException expected: " + e.getMessage(), e);
			}
		}

		// set some system properties from the configuration values
		promotePropertiesToSystem(properties);
	}


	/**
	 * Read in a property file.
	 * 
	 * @param fileDirectory
	 *        The file's path.
	 * @param propertyFileName
	 *        The file name.
	 */
	private void readPropertyFile(String fileDirectory, String propertyFileName)
	{
		readPropertyFile(fileDirectory, propertyFileName, null);
	}

	/**
	 * Read in a property file.
	 * 
	 * @param fileDirectory
	 *        The file's path.
	 * @param propertyFileName
	 *        The file name.
	 * @param loadMessage
	 *        A message to show after loading.
	 */
	private void readPropertyFile(String fileDirectory, String propertyFileName, String loadMessage)
	{
		try
		{
			File f = new File(fileDirectory + propertyFileName);
			if (f.exists())
			{
				properties.load(new FileInputStream(f));

				if (loadMessage != null)
				{
					M_log.warn(loadMessage);
				}

				M_log.info("loaded properties file: " + fileDirectory + propertyFileName);
			}
		}
		catch (Throwable t)
		{
			M_log.warn(t.getMessage(), t);
		}
	}


	/**
	 * If the properties has any of the values we need to set as sakai system properties, set them.
	 * 
	 * @param props
	 *        The property override configurer with some override settings.
	 */
	private void promotePropertiesToSystem(Properties props)
	{
		String serverId = props.getProperty("serverId");
		if (serverId != null)
		{
			System.setProperty("sakai.serverId", serverId);
		}

		// for the request filter
		String uploadMax = props.getProperty("content.upload.max");
		if (uploadMax != null)
		{
			System.setProperty("sakai.content.upload.max", uploadMax);
		}

		// for the request filter
		String uploadCeiling = props.getProperty("content.upload.ceiling");
		if (uploadCeiling != null)
		{
			System.setProperty("sakai.content.upload.ceiling", uploadCeiling);
		}

		// for the request filter
		String uploadDir = props.getProperty("content.upload.dir");
		if (uploadDir != null)
		{
			System.setProperty("sakai.content.upload.dir", uploadDir);
		}

		if (props.getProperty("force.url.secure") != null)
		{
			try
			{
				// make sure it is an int
				Integer.parseInt(props.getProperty("force.url.secure"));
				System.setProperty("sakai.force.url.secure", props.getProperty("force.url.secure"));
			}
			catch (Throwable e)
			{
				M_log.warn("force.url.secure set to a non numeric value: " + props.getProperty("force.url.secure"), e);
			}
		}
	}

	public Properties getProperties() {
		return properties;
	}
}
