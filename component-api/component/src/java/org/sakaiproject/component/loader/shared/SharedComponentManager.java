/**
 * 
 */
package org.sakaiproject.component.loader.shared;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeOperationsException;

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
public class SharedComponentManager implements CommonLifecycle,
		SharedComponentManagerMBean
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
			ObjectName componentManager = new ObjectName(
					ComponentManager.MBEAN_COMPONENT_MANAGER);
			mbs.registerMBean(this, componentManager);

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
