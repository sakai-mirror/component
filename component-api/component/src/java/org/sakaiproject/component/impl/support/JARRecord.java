/*
 * Created on 1 Dec 2007
 */
package org.sakaiproject.component.impl.support;

public class JARRecord {
  public JARRecord(String hash, String jarName, long modtime) {
    this.hash = hash;
    this.jarName = jarName;
  }
  public String hash;
  public String jarName;
  public long modtime;
}
