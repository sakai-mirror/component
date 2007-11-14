/*
 * Created on 13 Nov 2007
 */
package org.sakaiproject.component.impl.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ConfigurableApplicationContext;

public interface StaggeredRefreshApplicationContext extends ConfigurableApplicationContext {

    public void bareRefresh() throws BeansException, IllegalStateException;

    public void concludeRefresh();

}