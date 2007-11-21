package org.sakaiproject.component.loader.tomcat6.server;

import javax.management.ObjectName;

import org.apache.catalina.Engine;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.catalina.ServerFactory;
import org.apache.catalina.Service;
import org.apache.catalina.core.StandardService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.loader.common.CommonLifecycle;

public class SakaiLoader implements LifecycleListener
{

	private static final Log log = LogFactory.getLog(SakaiLoader.class);

	// private static final String MBEAN_SHARED_CLASSLOADER =
	// "Catalina:type=ServerClassLoader,name=shared";

	private static final String MBEAN_CONTAINER = "Catalina:type=Host,host=localhost";

	private static final String COMPONENT_MANAGER_CLASS = "org.sakaiproject.component.loader.shared.SharedComponentManager";

	private CommonLifecycle componentManager;

	private ClassLoader sharedClassloader;

	private Engine engine;

	public void lifecycleEvent(LifecycleEvent event)
	{
		try
		{
			String type = event.getType();
			log.info("At " + type);
			if (Lifecycle.INIT_EVENT.equals(type))
			{
				log.info("INIT");
			}
			else if (Lifecycle.BEFORE_START_EVENT.equals(type))
			{
				log.info("Before Start");
				startComponentManager();
			}
			else if (Lifecycle.START_EVENT.equals(type))
			{
				log.info("Start");
			}
			else if (Lifecycle.AFTER_START_EVENT.equals(type))
			{
				log.info("After Start");
			}
			else if (Lifecycle.PERIODIC_EVENT.equals(type))
			{
				log.info("Periodic");
			}
			else if (Lifecycle.BEFORE_STOP_EVENT.equals(type))
			{
				System.err.println("Before Stop");
			}
			else if (Lifecycle.STOP_EVENT.equals(type))
			{
				System.err.println("Stop");
			}
			else if (Lifecycle.AFTER_STOP_EVENT.equals(type))
			{
				System.err.println("After Stop");
				stopComponentManager();
			}
			else if (Lifecycle.DESTROY_EVENT.equals(type))
			{
				System.err.println("Destroy ");
			}
			else
			{
				log.warn("Unrecognised Container Lifecycle Event ");
			}
		}
		catch (Exception ex)
		{
			log.error("Failed to start Component Context ", ex);
		}
	}


	private void startComponentManager() throws Exception
	{
		ObjectName pname = new ObjectName(MBEAN_CONTAINER);
		Service service = getService(pname);
		log.warn("Got service as " + service);
		engine = (Engine) service.getContainer();
		log.warn("Got engine as " + engine + " with classloader "
				+ engine.getClass().getClassLoader() + " and with parent classloader "
				+ engine.getParentClassLoader());
		sharedClassloader = engine.getParentClassLoader();
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(sharedClassloader);
		try
		{
			Class clazz = sharedClassloader.loadClass(COMPONENT_MANAGER_CLASS);
			componentManager = (CommonLifecycle) clazz.newInstance();
			log.info("Starting Component Manager " + clazz.getName());
			componentManager.start();
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
	}

	private void stopComponentManager() throws Exception
	{
		System.err.println("Stopping Component Manger");
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(sharedClassloader);
		try
		{
			componentManager.stop();
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
	}

	private Service getService(ObjectName oname) throws Exception
	{

		String domain = oname.getDomain();
		Server server = ServerFactory.getServer();
		Service[] services = server.findServices();
		StandardService service = null;
		for (int i = 0; i < services.length; i++)
		{
			service = (StandardService) services[i];
			if (domain.equals(service.getObjectName().getDomain()))
			{
				break;
			}
		}
		if (!service.getObjectName().getDomain().equals(domain))
		{
			throw new Exception("Service with the domain is not found");
		}
		return service;

	}

}
