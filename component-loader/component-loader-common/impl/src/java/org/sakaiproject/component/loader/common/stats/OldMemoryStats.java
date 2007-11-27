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

package org.sakaiproject.component.loader.common.stats;

/**
 * @author ieb
 *
 */
public class OldMemoryStats extends AbstractStats implements MemoryStats
{

	private static final String[] NAMES = new String[]{
		 "java.lang:type=MemoryPool,name=Perm Gen",
		 "java.lang:type=MemoryPool,name=Tenured Gen",
		 "java.lang:type=MemoryPool,name=Code Cache",
		 "java.lang:type=MemoryPool,name=Eden Space",
		 "java.lang:type=MemoryPool,name=Survivor Space"
	};

	private static final String[] LABELS = {
		"        Permgen Used ",
		"        Tenured Used ",
		"     Code Cache Used ",
		"           Eden Used ",
		"       Survivor Used "
	};
	/**
	 * @return
	 */
	protected  String[] getLables()
	{
		return LABELS;
	}

	/**
	 * @return
	 */
	protected  String[] getNames()
	{
		return NAMES;
	}
	

}
