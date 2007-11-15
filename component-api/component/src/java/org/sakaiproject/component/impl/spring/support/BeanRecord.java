/*
 * Created on 13 Nov 2007
 */
package org.sakaiproject.component.impl.spring.support;

import org.sakaiproject.component.impl.spring.ComponentRecord;

/** A record for a specific bean retrieved from a component context. As well
 * as the raw bean itself (before proxying) the record reports the record for
 * the component itself.
 * 
 * @author Antranig Basman (antranig@caret.cam.ac.uk)
 */

public class BeanRecord {
    public Object bean;
    public ComponentRecord record;
    public BeanRecord(Object bean, ComponentRecord record) {
        super();
        this.bean = bean;
        this.record = record;
    }
}
