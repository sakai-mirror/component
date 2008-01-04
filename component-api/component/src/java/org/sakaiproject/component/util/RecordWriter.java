/*
 * Created on 2 Dec 2007
 */
package org.sakaiproject.component.util;

import java.io.BufferedWriter;
import java.io.IOException;

public interface RecordWriter <T>{
  public void writeRecord(BufferedWriter writer, T record) throws IOException;
}
