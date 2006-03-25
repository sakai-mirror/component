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

package org.sakaiproject.component.api;

/**
 * <p>
 * Load the available Sakai components into the shared component manager
 * </p>
 */
public interface ComponentsLoader
{
	/**
	 * Load components into this manager.
	 * 
	 * @param mgr
	 *        The component manager instance.
	 * @param componentsRoot
	 *        The file system canonical path to the directory where components packages are found.
	 */
	void load(ComponentManager mgr, String componentsRoot);
}
