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

import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author ieb
 *
 */
public interface SpringComponentManager
{

	/**
	 * Get the base Application Context from the Spring component manager
	 * @return
	 */
	ConfigurableApplicationContext getApplicationContext();

	/**
	 * Register an Applicaiton Context suplying a component package with the Component Manager
	 * @param file
	 * @param context
	 */
	void registerComponentPackage(String name, ConfigurableApplicationContext context);

}
