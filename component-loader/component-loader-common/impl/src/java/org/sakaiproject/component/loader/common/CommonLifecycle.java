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

package org.sakaiproject.component.loader.common;


/**
 * Objects that have a lifecycle can implement this. 
 * @author ieb
 *
 */
public interface CommonLifecycle{


	

	/**
	 * Starts the object
	 */
	void start();

	/**
	 * stops the object
	 */
	void stop();
	
	/**
	 * get a reference to the object with a lifecycle
	 * @return
	 */
	Object getLifecycleObject();

	/**
	 * Destroy, no more life
	 */
	void destroy();
}
