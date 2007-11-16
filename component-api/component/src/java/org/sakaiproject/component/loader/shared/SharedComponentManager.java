/**
 * 
 */
package org.sakaiproject.component.loader.shared;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;
import javax.management.openmbean.CompositeData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.api.ComponentManager;
import org.sakaiproject.component.impl.SpringCompMgr;
import org.sakaiproject.component.loader.common.CommonLifecycle;
import org.sakaiproject.component.loader.common.CommonLifecycleEvent;
import org.sakaiproject.component.loader.common.CommonLifecycleListener;

/**
 * @author ieb
 */
public class SharedComponentManager implements CommonLifecycle
{ 

	
	/**
	 * @throws MBeanException
	 * @throws RuntimeOperationsException
	 */
	public SharedComponentManager() throws MBeanException, RuntimeOperationsException
	{
		super();
	}

	private static final Log log = LogFactory.getLog(SharedComponentManager.class);

	private SpringCompMgr cm;

	private CopyOnWriteArraySet<CommonLifecycleListener> listeners = new CopyOnWriteArraySet<CommonLifecycleListener>();

	private Date lastLoadDate;

	private long loadTime;

	public void start()
	{
		log
				.info("Component Manager is starting =========================================================================");
		try
		{
			long start = System.currentTimeMillis();
			lifecycleEvent(CommonLifecycleEvent.BEFORE_START);
			lastLoadDate = new Date();
			cm = new SpringCompMgr(null);

			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			
			RequiredModelMBean model = new RequiredModelMBean(createMBeanInfo());
			model.setManagedResource(this, "objectReference");
			ObjectName componentManager = new ObjectName(
					ComponentManager.MBEAN_COMPONENT_MANAGER);
			mbs.registerMBean(model, componentManager);
			
			

			cm.init();
			
			System.runFinalization();
			Runtime.getRuntime().gc();
			
			CompositeData permGen = (CompositeData) mbs.getAttribute(new ObjectName("java.lang:type=MemoryPool,name=Perm Gen"), "Usage");
			CompositeData tenuredGen = (CompositeData) mbs.getAttribute(new ObjectName("java.lang:type=MemoryPool,name=Tenured Gen"), "Usage");
			CompositeData codeCache = (CompositeData) mbs.getAttribute(new ObjectName("java.lang:type=MemoryPool,name=Code Cache"), "Usage");
			CompositeData edenSpace = (CompositeData) mbs.getAttribute(new ObjectName("java.lang:type=MemoryPool,name=Eden Space"), "Usage");
			CompositeData survivorSpace = (CompositeData) mbs.getAttribute(new ObjectName("java.lang:type=MemoryPool,name=Survivor Space"), "Usage");
			long permGenUsed = Long.parseLong(String.valueOf(permGen.get("used")));
			long codeCacheUsed = Long.parseLong(String.valueOf(codeCache.get("used")));
			long edenSpaceUsed = Long.parseLong(String.valueOf(edenSpace.get("used")));
			long tenuredGenUsed = Long.parseLong(String.valueOf(tenuredGen.get("used")));
			long survivorSpaceUsed = Long.parseLong(String.valueOf(survivorSpace.get("used")));
			
			log.info("           Permgen Used "+permGenUsed/(1024*1024)+" MB");
			log.info("           Code Cache Used "+codeCacheUsed/(1024*1024)+" MB");
			log.info("           Eden Used "+edenSpaceUsed/(1024*1024)+" MB");
			log.info("           Tenured Used "+tenuredGenUsed/(1024*1024)+" MB");
			log.info("           Survivour Used "+survivorSpaceUsed/(1024*1024)+" MB");
			
			lifecycleEvent(CommonLifecycleEvent.START);
			lifecycleEvent(CommonLifecycleEvent.AFTER_START);
			loadTime = System.currentTimeMillis() - start;

		}
		catch (Throwable ex)
		{
			log.error("Failed to start ComponentManager ", ex);
			System.exit(10);
		}
		log
				.info("Component Manager Start Complete =========================================================================");

	}

	/**
	 * @return
	 */
	private ModelMBeanInfo createMBeanInfo()
	{
		Descriptor lastLoadDate = new DescriptorSupport(new String[] {
	                 "name=LastLoadDate",
	                 "descriptorType=attribute",
	                 "default=0",
	                 "displayName=Last Load Date",
	                 "getMethod=getLastLoadDate"
	                }
	                );
		Descriptor lastLoadTime = new DescriptorSupport(new String[] {
                "name=LastLoadTime",
                "descriptorType=attribute",
                "default=0",
                "displayName=Last Load Time",
                "getMethod=getLoadTime"
               }
               );

	    ModelMBeanAttributeInfo [] mmbai = new ModelMBeanAttributeInfo[2];
	    mmbai[0] = new ModelMBeanAttributeInfo("LastLoadDate","java.util.Date",
	      "Last Load Date",  true,false, false, lastLoadDate);
	    
	    mmbai[1] = new ModelMBeanAttributeInfo("LastLoadTime","java.lang.Long",
	  	      "Last Load Time",  true,false, false, lastLoadTime);

	    ModelMBeanOperationInfo [] mmboi = new ModelMBeanOperationInfo[7];

	    mmboi[0] = new ModelMBeanOperationInfo("start", 
	    "Start the Component Manager", null, "void", ModelMBeanOperationInfo.ACTION
	    );
	    mmboi[1] = new ModelMBeanOperationInfo("stop", 
	    	    "Stop the Component Manager", null, "void", ModelMBeanOperationInfo.ACTION
	    	    );
	    mmboi[2] = new ModelMBeanOperationInfo("getComponentManager", 
	    	    "Get the Current Component Manager", null, ComponentManager.class.getName(), ModelMBeanOperationInfo.INFO
	    	    );
	    
	    mmboi[3] = new ModelMBeanOperationInfo("addComponentManagerLifecycleListener", 
	    	    "Add a listener to the component lifecycle", 
	    	    new MBeanParameterInfo[] {new MBeanParameterInfo("Lifecycle Listener",CommonLifecycleListener.class.getName(),"The Lifecycle Listener to be added") }, 
	    	    "void", ModelMBeanOperationInfo.ACTION
	    	    );
	    mmboi[4] = new ModelMBeanOperationInfo("removeComponentManagerLifecycleListener", 
	    	    "Remove a listener to the component lifecycle", 
	    	    new MBeanParameterInfo[] {new MBeanParameterInfo("Lifecycle Listener",CommonLifecycleListener.class.getName(),"The Lifecycle Listener to be removed") }, 
	    	    "void", ModelMBeanOperationInfo.ACTION
	    	    );
	    mmboi[5] = new ModelMBeanOperationInfo("getLastLoadDate", 
	    	    "The date the component manager was last loaded", 
	    	    null, 
	    	    "java.util.Date", ModelMBeanOperationInfo.INFO
	    	    );
	    mmboi[6] = new ModelMBeanOperationInfo("getLoadTime", 
	    	    "The time it took to load the component manager", 
	    	    null, 
	    	    "long", ModelMBeanOperationInfo.INFO
	    	    );
	    

	    /*
	    mmboi[1] = new ModelMBeanOperationInfo("decPanelValue", 
	    "decrement the meter value", null, "void", ModelMBeanOperationInfo.ACTION
	    );

	    mmboi[2] = new ModelMBeanOperationInfo("getPanelValue", 
	    "getter for PanelValue", null,"Integer", ModelMBeanOperationInfo.INFO);

	    MBeanParameterInfo [] mbpi = new MBeanParameterInfo[1];
	    mbpi[0] =  new MBeanParameterInfo("inVal", "java.lang.Integer", 
	      "value to set");
	    mmboi[3] = new ModelMBeanOperationInfo("setPanelValue",
	      "setter for PanelValue", mbpi, "void", ModelMBeanOperationInfo.ACTION);


	    ModelMBeanConstructorInfo [] mmbci = new ModelMBeanConstructorInfo[1];
	    mmbci[0] = new ModelMBeanConstructorInfo("ClickMeterMod", 
	    "constructor for Model Bean Sample", null);
		*/
	    
	    return new ModelMBeanInfoSupport(this.getClass().getName(),
	    "Sakai Component Manager", mmbai, null, mmboi, null);
	}

	public void stop()
	{
		log.info("Component Manager is stopping");
		try
		{
			lifecycleEvent(CommonLifecycleEvent.BEFORE_STOP);
			lifecycleEvent(CommonLifecycleEvent.STOP);
			cm.close();
			lifecycleEvent(CommonLifecycleEvent.AFTER_START);
		}
		catch (Throwable ex)
		{
			log.error("Failed to stop ComponentManager ", ex);
		}

	}

	public void destroy()
	{
		log.info("Component Manager is stopping");
		try
		{
			lifecycleEvent(CommonLifecycleEvent.DESTROY);
			listeners.clear();
		}
		catch (Throwable ex)
		{
			log.error("Failed to stop ComponentManager ", ex);
		}

	}

	public Object getLifecycleObject()
	{
		return cm;
	}

	public org.sakaiproject.component.api.ComponentManager getComponentManager()
	{
		return cm;
	}
	
	/**
	 * Fire the lifecycle events 
	 * @param event
	 */
	protected void lifecycleEvent(CommonLifecycleEvent event) {
		for ( CommonLifecycleListener l : listeners) {
			l.lifecycleEvent(event);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.component.loader.shared.SharedComponentManagerMBean#addComponentManagerLifecycleListener(org.sakaiproject.component.loader.common.CommonLifecycleListener)
	 */
	public void addComponentManagerLifecycleListener(CommonLifecycleListener listener)
	{
		if (!listeners.contains(listener))
		{
			listeners.add(listener);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.component.loader.shared.SharedComponentManagerMBean#removeComponentManagerLifecycleListener(org.sakaiproject.component.loader.common.CommonLifecycleListener)
	 */
	public void removeComponentManagerLifecycleListener(CommonLifecycleListener listener)
	{
		listeners.remove(listener);
	}

	public Date getLastLoadDate() {
		return lastLoadDate;
	}
	
	public long getLoadTime() {
		return loadTime;
	}

}
