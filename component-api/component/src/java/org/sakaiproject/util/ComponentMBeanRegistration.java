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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author ieb
 */
public class ComponentMBeanRegistration implements  ComponentMBeanRegistrationMBean
{

	private static final Log log = LogFactory.getLog(ComponentMBeanRegistration.class);

	private static final String MBEAN_COMPONENT_BASE = "Sakai:type=ComponentPack,name=";

	private List<String> beans = new ArrayList<String>();

	private boolean sucess;

	private boolean demoLoaded;

	private String componentsXmStatus;

	private String demoComponentsXmlStatus;

	private String[] loaderClassLoaders;

	private int loadOrder;

	private long startLoad;

	private long loadTime;

	private String componentsRoot;

	private String componentsDir;

	private String componentName;

	private String[] beansLoaded;

	private Date startLoadDate;
	
	
	public ComponentMBeanRegistration() {
	}


	/**
	 * 
	 */
	public void completeLoad()
	{
		try
		{
			beansLoaded = beans.toArray(new String[0]);
			startLoadDate = new Date(startLoad);

			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

			/*RequiredModelMBean model = new RequiredModelMBean(createMBeanInfo());
			model.setManagedResource(this, "objectReference");
			componentName = componentsDir;
			*/
			if (componentsDir.startsWith(componentsRoot))
			{
				componentName = componentsDir.substring(componentsRoot.length());
			}
			ObjectName componentManager = new ObjectName(MBEAN_COMPONENT_BASE
					+ componentName);
			mbs.registerMBean(this, componentManager);
		}
		catch (Exception ex)
		{
			log.warn("Failed to register mbean for component ", ex);

		}

	}

	/**
	 * @param i
	 */
	public void setLoadOrder(int loadOrder)
	{
		this.loadOrder = loadOrder;

	}

	/**
	 * @param string
	 */
	public void setLoaderClassloader(ClassLoader loader)
	{
		List<String> cls = new ArrayList<String>();
		cls.add(loader.toString());
		ClassLoader cl = loader;
		while (cl.getParent() != null && cl != cl.getParent())
		{
			cl = cl.getParent();
			cls.add(cl.toString());
		}
		loaderClassLoaders = cls.toArray(new String[0]);
	}

	/**
	 * @param dir
	 */
	public void setComponentsDir(File dir)
	{
		componentsXmStatus = getFileStatus(dir);
		componentsDir = dir.getAbsolutePath();
	}

	/**
	 * @param xml
	 */
	public void setComponentsXml(File xml)
	{
		componentsXmStatus = getFileStatus(xml);
	}

	/**
	 * @param xml
	 * @return
	 */
	private String getFileStatus(File file)
	{
		StringBuilder sb = new StringBuilder();
		if ( file.exists() ) {
		sb.append(file.getAbsolutePath()).append("; length=[").append(file.length())
				.append("]; lastModified=[").append(new Date(file.lastModified()))
				.append("]");
		} else {
			sb.append("Not Found: ").append(file.getAbsolutePath());
			
		}
		return sb.toString();
	}

	/**
	 * @param demoXml
	 */
	public void setDemoComponentsXml(File demoXml)
	{
		demoComponentsXmlStatus = getFileStatus(demoXml);
	}

	/**
	 * @param demoLoaded
	 */
	public void setDemoComponentsLoaded(boolean demoLoaded)
	{
		this.demoLoaded = demoLoaded;
	}

	/**
	 * 
	 */
	public void startLoad()
	{
		startLoad = System.currentTimeMillis();
		this.sucess = false;
	}

	/**
	 * 
	 */
	public void endLoad()
	{
		long endLoad = System.currentTimeMillis();
		this.sucess = true;
		loadTime = endLoad - startLoad;
	}

	public String[] getBeansLoaded()
	{
		return beansLoaded;
	}

	public String getComponentName()
	{
		log.info("Getting Component Name "+componentName);
		return componentName;
	}

	public boolean getSucess()
	{
		return sucess;
	}

	public boolean getDemoLoaded()
	{
		return demoLoaded;
	}

	public String getDemoComponentsXmStatus()
	{
		return demoComponentsXmlStatus;
	}
	public String getComponentsXmStatus()
	{
		return componentsXmStatus;
	}

	public String[] getLoaderClassLoaders()
	{
		return loaderClassLoaders;
	}

	public int getLoadOrder()
	{
		return loadOrder;
	}

	public Date getStartLoad()
	{
		return startLoadDate;
	}

	public long getLoadTime()
	{
		return loadTime;
	}

	/**
	 * @return
	 */
	private ModelMBeanInfo createMBeanInfo()
	{

		ModelMBeanAttributeInfo[] mmbai = new ModelMBeanAttributeInfo[10];
		try
		{
			mmbai[0] = new ModelMBeanAttributeInfo("beansLoaded", "Beans Loaded",  this
					.getClass().getMethod("getBeansLoaded", new Class[0]), null);
			mmbai[1] = new ModelMBeanAttributeInfo("componentName", "java.lang.String", this
					.getClass().getMethod("getComponentName", new Class[0]), null);
			mmbai[2] = new ModelMBeanAttributeInfo("sucess", "Load Sucessful",  this
					.getClass().getMethod("getSucess", new Class[0]), null);
			mmbai[3] = new ModelMBeanAttributeInfo("demoLoaded",
					"Loaded Demo Components",  this.getClass().getMethod("getDemoLoaded",
							new Class[0]), null);
			mmbai[4] = new ModelMBeanAttributeInfo("componentsXmStatus",
					"Base Components Status", this.getClass().getMethod(
							"getComponentsXmStatus", new Class[0]), null);
			mmbai[5] = new ModelMBeanAttributeInfo("demoComponentsXmlStatus",
					"Demo Components Status", this.getClass().getMethod(
							"getDemoComponentsXmStatus", new Class[0]), null);
			mmbai[6] = new ModelMBeanAttributeInfo("loaderClassLoaders",
					"Classloader hierarchy", this.getClass().getMethod(
							"getLoaderClassLoaders", new Class[0]), null);
			mmbai[7] = new ModelMBeanAttributeInfo("loadOrder", "Load Order", this
					.getClass().getMethod("getLoadOrder", new Class[0]), null);
			mmbai[8] = new ModelMBeanAttributeInfo("startLoad", "Load Date", this
					.getClass().getMethod("getStartLoad", new Class[0]), null);
			mmbai[9] = new ModelMBeanAttributeInfo("loadTime", "Load Time", this
					.getClass().getMethod("getLoadTime", new Class[0]), null);

			/*
			 * ModelMBeanOperationInfo[] mmboi = new ModelMBeanOperationInfo[7];
			 * mmboi[0] = new ModelMBeanOperationInfo("start", "Start the
			 * Component Manager", null, "void",
			 * ModelMBeanOperationInfo.ACTION); mmboi[1] = new
			 * ModelMBeanOperationInfo("stop", "Stop the Component Manager",
			 * null, "void", ModelMBeanOperationInfo.ACTION); mmboi[2] = new
			 * ModelMBeanOperationInfo("getComponentManager", "Get the Current
			 * Component Manager", null, ComponentManager.class .getName(),
			 * ModelMBeanOperationInfo.INFO); mmboi[3] = new
			 * ModelMBeanOperationInfo("addComponentManagerLifecycleListener",
			 * "Add a listener to the component lifecycle", new
			 * MBeanParameterInfo[] { new MBeanParameterInfo("Lifecycle
			 * Listener", CommonLifecycleListener.class.getName(), "The
			 * Lifecycle Listener to be added") }, "void",
			 * ModelMBeanOperationInfo.ACTION); mmboi[4] = new
			 * ModelMBeanOperationInfo("removeComponentManagerLifecycleListener",
			 * "Remove a listener to the component lifecycle", new
			 * MBeanParameterInfo[] { new MBeanParameterInfo("Lifecycle
			 * Listener", CommonLifecycleListener.class.getName(), "The
			 * Lifecycle Listener to be removed") }, "void",
			 * ModelMBeanOperationInfo.ACTION); mmboi[5] = new
			 * ModelMBeanOperationInfo("getLastLoadDate", "The date the
			 * component manager was last loaded", null, "java.util.Date",
			 * ModelMBeanOperationInfo.INFO); mmboi[6] = new
			 * ModelMBeanOperationInfo("getLoadTime", "The time it took to load
			 * the component manager", null, "long",
			 * ModelMBeanOperationInfo.INFO);
			 */
			/*
			 * mmboi[1] = new ModelMBeanOperationInfo("decPanelValue",
			 * "decrement the meter value", null, "void",
			 * ModelMBeanOperationInfo.ACTION ); mmboi[2] = new
			 * ModelMBeanOperationInfo("getPanelValue", "getter for PanelValue",
			 * null,"Integer", ModelMBeanOperationInfo.INFO); MBeanParameterInfo []
			 * mbpi = new MBeanParameterInfo[1]; mbpi[0] = new
			 * MBeanParameterInfo("inVal", "java.lang.Integer", "value to set");
			 * mmboi[3] = new ModelMBeanOperationInfo("setPanelValue", "setter
			 * for PanelValue", mbpi, "void", ModelMBeanOperationInfo.ACTION);
			 * ModelMBeanConstructorInfo [] mmbci = new
			 * ModelMBeanConstructorInfo[1]; mmbci[0] = new
			 * ModelMBeanConstructorInfo("ClickMeterMod", "constructor for Model
			 * Bean Sample", null);
			 */
		}
		catch (Exception ex)
		{
			throw new RuntimeException("Failed to generate Bean Info",ex);
		}

		return new ModelMBeanInfoSupport(this.getClass().getName(), "Component "
				+ componentName, mmbai, null, null, null);
	}

	/**
	 * @param root
	 */
	public void setRoot(File root)
	{
		componentsRoot = root.getAbsolutePath();
	}

}
