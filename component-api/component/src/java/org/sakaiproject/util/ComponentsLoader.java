/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006 The Sakai Foundation.
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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.util.ComponentApplicationContext.ComponentContexts;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * <p>
 * Load the available Sakai components into the shared component manager's Spring ApplicationContext
 * </p>
 */
public class ComponentsLoader
{
	/** Our logger */
	private static Log M_log = LogFactory.getLog(ComponentsLoader.class);
	
	private List<ComponentApplicationContext> componentApplicationContexts;
	
	public ComponentsLoader()
	{
		if (ComponentApplicationContext.getComponentContextsOption() != ComponentContexts.NONE)
		{
			componentApplicationContexts = new ArrayList<ComponentApplicationContext>();
		}
	}

	/**
	 * 
	 */
	public void load(ConfigurableApplicationContext ac, String componentsRoot)
	{
		try
		{
			// get a list of the folders in the root
			File root = new File(componentsRoot);

			// make sure it's a dir.
			if (!root.isDirectory())
			{
				M_log.warn("load: root not directory: " + componentsRoot);
				return;
			}

			// what component packages are there?
			File[] packageArray = root.listFiles();

			if (packageArray == null)
			{
				M_log.warn("load: empty directory: " + componentsRoot);
				return;
			}
			
			List<File> packages = new ArrayList<File>(Arrays.asList(packageArray));

			// for testing, we might reverse load order
			final int reverse = System.getProperty("sakai.components.reverse.load") != null ? -1 : 1;

			// assure a consistent order - sort these files
			Collections.sort(packages, new Comparator<File>()
			{
				public int compare(File o1, File o2)
				{
					File f1 = o1;
					File f2 = o2;
					int sort = f1.compareTo(f2);
					return sort * reverse;
				}
			});
			
			M_log.info("load: loading components from: " + componentsRoot);

			// process the packages
			for (File packageDir : packages)
			{
				// if a valid components directory
				if (validComponentsPackage(packageDir))
				{
					loadComponentPackage(packageDir, ac);
				}
				else
				{
					M_log.warn("load: skipping non-package entry: " + packageDir);
				}
			}
		}
		catch (Throwable t)
		{
			M_log.warn("load: exception: " + t, t);
		}
	}

	/**
	 * Load one component package into the AC
	 * 
	 * @param packageRoot
	 *        The file path to the component package
	 * @param containerApplicationContext
	 *        The ApplicationContext to load into
	 */
	protected void loadComponentPackage(File dir, ConfigurableApplicationContext containerApplicationContext)
	{
		// setup the classloader onto the thread
		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
		ClassLoader componentClassLoader = getComponentClassLoader(dir, containerApplicationContext);

		M_log.info("loadComponentPackage: " + dir);
		
		if (ComponentApplicationContext.getComponentContextsOption() == ComponentContexts.NONE)
		{
			Thread.currentThread().setContextClassLoader(componentClassLoader);

			// make a reader
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader((BeanDefinitionRegistry) containerApplicationContext.getBeanFactory());

			// In Spring 2, classes aren't loaded during bean parsing unless this
			// classloader property is set.
			reader.setBeanClassLoader(componentClassLoader);

			try
			{
				reader.loadBeanDefinitions(getDefaultConfigResources(dir));
			}
			catch (Throwable t)
			{
				M_log.warn("Exception loading component package at " + dir, t);
			}
			finally
			{
				// restore the context loader
				Thread.currentThread().setContextClassLoader(currentClassLoader);
			}
		} 
		else if (ComponentApplicationContext.getComponentContextsOption() == ComponentContexts.ALL)
		{
			ComponentApplicationContext componentApplicationContext = new ComponentApplicationContext(containerApplicationContext, componentClassLoader, getDefaultConfigResources(dir));
			componentApplicationContext.refreshBeanDefinitions();
			componentApplicationContexts.add(componentApplicationContext);
		}
	}
	
	/**
	 */
	public void refreshAllComponentApplicationContexts()
	{
		if (ComponentApplicationContext.getComponentContextsOption() != ComponentContexts.NONE)
		{
			for (ComponentApplicationContext componentApplicationContext : componentApplicationContexts)
			{
				// Note that this may have already been triggered by cross-component
				// dependencies.
				componentApplicationContext.refreshPostProcessors();
			}
			for (ComponentApplicationContext componentApplicationContext : componentApplicationContexts)
			{
				componentApplicationContext.refreshSingletons();
			}
		}
	}
	
	private Resource[] getDefaultConfigResources(File directory)
	{
		List<Resource> configResources = new ArrayList<Resource>();
		File webinf = new File(directory, "WEB-INF");
		File defaultConfigFile = new File(webinf, "components.xml");
		if (defaultConfigFile.exists())
		{
			try 
			{
				configResources.add(new FileSystemResource(defaultConfigFile.getCanonicalPath()));
			} catch (IOException e) 
			{
				M_log.error(e);
			}
		}
		if ("true".equalsIgnoreCase(System.getProperty("sakai.demo"))) 
		{
			File demoConfigFile = new File(webinf, "components-demo.xml");
			if (demoConfigFile.exists())
			{
				try 
				{
					configResources.add(new FileSystemResource(demoConfigFile.getCanonicalPath()));
				} catch (IOException e) {
					M_log.error(e);
				}
			}			
		}
		Resource[] resourceArray = configResources.toArray(new Resource[0]);
		return resourceArray;
	}

	/**
	 * Test if this File is a valid components package directory.
	 * 
	 * @param dir
	 *        The file to test
	 * @return true if it is a valid components package directory, false if not.
	 */
	protected boolean validComponentsPackage(File dir)
	{
		// valid if this is a directory with a WEB-INF directory below with a components.xml file
		if ((dir != null) && (dir.isDirectory()))
		{
			File webinf = new File(dir, "WEB-INF");
			if ((webinf != null) && (webinf.isDirectory()))
			{
				File xml = new File(webinf, "components.xml");
				if ((xml != null) && (xml.isFile()))
				{
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Create the class loader for this component package
	 * 
	 * @param dir
	 *        The package's root directory.
	 * @return A class loader, whose parent is this class's loader, which has the classes/ and jars for this component.
	 */
	protected ClassLoader getComponentClassLoader(File dir, ApplicationContext containerApplicationContext)
	{
		// collect as a List, turn into an array after
		List<URL> urls = new Vector<URL>();

		File webinf = new File(dir, "WEB-INF");

		// put classes/ on the classpath
		File classes = new File(webinf, "classes");
		if ((classes != null) && (classes.isDirectory()))
		{
			try
			{
				URL url = new URL("file:" + classes.getCanonicalPath() + "/");
				urls.add(url);
			}
			catch (MalformedURLException e)
			{
				if (M_log.isWarnEnabled()) M_log.warn(e);
			}
			catch (IOException e)
			{
				if (M_log.isWarnEnabled()) M_log.warn(e);
			}
		}

		// put each .jar file onto the classpath
		File lib = new File(webinf, "lib");
		if ((lib != null) && (lib.isDirectory()))
		{
			File[] jars = lib.listFiles(new FileFilter()
			{
				public boolean accept(File file)
				{
					return (file.isFile() && file.getName().endsWith(".jar"));
				}
			});

			if (jars != null)
			{
				for (int j = 0; j < jars.length; j++)
				{
					try
					{
						URL url = new URL("file:" + jars[j].getCanonicalPath());
						urls.add(url);
					}
					catch (MalformedURLException e)
					{
						if (M_log.isWarnEnabled()) M_log.warn(e);
					}
					catch (IOException e)
					{
						if (M_log.isWarnEnabled()) M_log.warn(e);
					}
				}
			}
		}

		// make the array from the list
		URL[] urlArray = (URL[]) urls.toArray(new URL[urls.size()]);

		// make the classloader - my loader is parent
		URLClassLoader loader = new URLClassLoader(urlArray, containerApplicationContext.getClassLoader());

		return loader;
	}
}
