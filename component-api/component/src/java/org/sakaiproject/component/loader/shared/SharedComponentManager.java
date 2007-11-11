/**
 * 
 */
package org.sakaiproject.component.loader.shared;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.Descriptor;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import javax.management.modelmbean.RequiredModelMBean;

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

	public void start()
	{
		log
				.info("Component Manager is starting =========================================================================");
		try
		{
			lifecycleEvent(CommonLifecycleEvent.BEFORE_START);
			cm = new SpringCompMgr(null);

			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			
			RequiredModelMBean model = new RequiredModelMBean(createMBeanInfo());
			model.setManagedResource(this, "objectReference");
			ObjectName componentManager = new ObjectName(
					ComponentManager.MBEAN_COMPONENT_MANAGER);
			mbs.registerMBean(model, componentManager);
			
			

			cm.init();
			lifecycleEvent(CommonLifecycleEvent.START);
			lifecycleEvent(CommonLifecycleEvent.AFTER_START);

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
		/*
		Descriptor atDesc = new DescriptorSupport(new String[] {
	                 "name=ComponentManager",
	                 "descriptorType=attribute",
	                 "default=0",
	                 "displayName=Sakai ComponentManager",
	                 "getMethod=getComponentManager"
	                }
	                );

	    ModelMBeanAttributeInfo [] mmbai = new ModelMBeanAttributeInfo[1];
	    mmbai[0] = new ModelMBeanAttributeInfo("ComponentManager","org.sakaiproject.component.api.ComponentManager",
	      "The Sakai Component Manager",  true,false, false, atDesc);
	      */

	    ModelMBeanOperationInfo [] mmboi = new ModelMBeanOperationInfo[3];

	    mmboi[0] = new ModelMBeanOperationInfo("start", 
	    "Start the Component Manager", null, "void", ModelMBeanOperationInfo.ACTION
	    );
	    mmboi[1] = new ModelMBeanOperationInfo("stop", 
	    	    "Stop the Component Manager", null, "void", ModelMBeanOperationInfo.ACTION
	    	    );
	    mmboi[2] = new ModelMBeanOperationInfo("getComponentManager", 
	    	    "Get the Current Component Manager", null, "org.sakaiproject.component.api.ComponentManager", ModelMBeanOperationInfo.INFO
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
	    "Sakai Component Manager", null, null, mmboi, null);
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
	
	

}
