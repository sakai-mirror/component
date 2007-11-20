/**********************************************************************************
*
* $Id: BeanFactoryPostProcessorCreator.java 37753 2007-11-05 21:10:09Z ray@media.berkeley.edu $
*
***********************************************************************************
*
* Copyright (c) 2007 The Regents of the University of California
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

package org.sakaiproject.component.impl.spring;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

/**
 * Allows for programmatic creation of BeanFactoryPostProcessor objects.
 */
public interface BeanFactoryPostProcessorCreator {
	/**
	 * @return new BeanFactoryPostProcessor beans
	 */
	public BeanFactoryPostProcessor[] getBeanFactoryPostProcessors();
}
