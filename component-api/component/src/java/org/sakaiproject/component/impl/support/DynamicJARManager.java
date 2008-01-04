/*
 * Created on 1 Dec 2007
 */
package org.sakaiproject.component.impl.support;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.util.RecordFileIO;
import org.sakaiproject.component.util.RecordReader;
import org.sakaiproject.component.util.RecordWriter;
import org.sakaiproject.component.util.StreamDigestor;
import org.springframework.util.FileCopyUtils;

/** Maintains a hashed collection of JAR artefacts and their correspondence
 * with their occurence in a source collection - largely to evade file locking
 * issues on platforms with deficient filesystems (Windows).
 * 
 * @author Antranig Basman (amb26@ponder.org.uk)
 */

public class DynamicJARManager {
  private static Log M_log = LogFactory.getLog(DynamicJARManager.class);
  // Map of a JAR hash onto a JAR record
  private Map<String, JARRecord> hashmap = new HashMap<String, JARRecord>();

  // Map of a discovery path onto a JAR record - in this case "jarName" is a full FS path
  private Map<String, JARRecord> discoverymap = new HashMap<String, JARRecord>();

  // Map of a JAR name onto a JAR record list
  private Map<String, List<JARRecord>> namemap = new HashMap<String, List<JARRecord>>();

  private String cacheBase;

  private String CACHE_BASE = "jar-cache-base";
  private String RECORDS_FILE = "jar-records.txt";
  private String DISCOVERY_FILE = "discovery-records.txt";

  private String recordToCachePath(JARRecord record) {
    return cacheBase + '/' + record.hash + '/' + record.jarName;
  }

  private String getRecordsPath() {
    return cacheBase + '/' + RECORDS_FILE;
  }
  private String getDiscoveryPath() {
    return cacheBase + '/' + DISCOVERY_FILE;
  }

  public void setWriteableRoot(String writeableRoot) {
    cacheBase = writeableRoot + '/' + CACHE_BASE;
  }

  public void init() {
    loadRecords();
    verifyRecords();
    reconstructRecords();
    writeRecords();
  }

  
  public File getCachedJAR(File newJar) throws IOException {
    String fullpath = newJar.getCanonicalPath();
    JARRecord disc = discoverymap.get(fullpath);
    // if this is not a previously discovered JAR, register it anew
    if (disc == null || disc.modtime != newJar.lastModified()) {
      disc = generateJARRecord(newJar);
      disc.jarName = fullpath;
      discoverymap.put(fullpath, disc);
      JARRecord cached = hashmap.get(disc.hash);
      if (cached != null) {
        return getCachedJar(cached);
      }
      File discpath = getCachedJar(disc);
      FileCopyUtils.copy(newJar, discpath);
      return discpath;
    }
    else
      return getCachedJar(disc);
  }

  private File getCachedJar(JARRecord disc) {
    return new File(cacheBase + '/' + disc.hash);
  }

  
  private void loadRecords() {
    RecordFileIO.readRecordFile(getRecordsPath(), new RecordReader() {

      public void readRecord(String[] fields) {
        JARRecord record = new JARRecord(fields[0], fields[1], Long
            .parseLong(fields[2]));
        addRecord(record);
      }},  
       "Missing or corrupt JAR cache index discovered, will delete and recreate ",
      "Exception deleting JAR cache file "
      );
    
    RecordFileIO.readRecordFile(getDiscoveryPath(), new RecordReader() {

      public void readRecord(String[] fields) {
        JARRecord record = new JARRecord(fields[0], fields[1], Long
            .parseLong(fields[2]));
        discoverymap.put(record.jarName, record);
      }},  
       "Missing or corrupt discovery cache index discovered, will delete and recreate ",
      "Exception deleting discovery cache file "
      );
    
  }

  private RecordWriter<JARRecord> JARRecordWriter = new RecordWriter<JARRecord>() {
    public void writeRecord(BufferedWriter writer, JARRecord record) throws IOException {
      writer.write(record.hash + "\t" + record.jarName + "\t"
          + record.modtime + "\n");
    }
  };
  
  public void writeRecords() {
    try {
      RecordFileIO.writeRecordFile(getRecordsPath(), hashmap.values(),
          JARRecordWriter
          , "Error writing JAR cache index to path " + getDiscoveryPath());
      
      RecordFileIO.writeRecordFile(getRecordsPath(), discoverymap.values(),
         JARRecordWriter, "Error writing Discovery cache index to path " + getRecordsPath());
    }
    catch (Exception e) {
      M_log.warn(e);
    }

  }

  private void verifyRecords() {
    List<JARRecord> bads = new ArrayList<JARRecord>();
    for (JARRecord record : hashmap.values()) {
      String cachePath = recordToCachePath(record);
      File cacheFile = new File(cachePath);
      if (!cacheFile.exists()) {
        bads.add(record);
      }
    }
    for (JARRecord bad : bads) {
      removeRecord(bad);
    }
  }

  private void reconstructRecords() {
    File root = new File(cacheBase);
    if (!root.exists()) {
      root.mkdir();
    }
    File[] dirs = root.listFiles();
    for (File dir: dirs) {
      if (!dir.isDirectory())
        continue;
      File[] files = dir.listFiles();
      if (files.length != 1) {
        M_log.warn("Cache directory " + dir + " does not contain exactly one file - deleting");
        for (File file: files) {
          file.delete();
        }
        dir.delete();
        continue;
      }
      File jar = files[0];
      JARRecord existing = hashmap.get(dir.getName());
      if (existing != null) {
        // if the mod time does not match, blast it and rehash the file
        if (existing.modtime == (jar.lastModified())) {
          existing = null;
        }
      }
      if (existing == null) {
        JARRecord newrecord = generateJARRecord(jar);
        addRecord(newrecord);
      }
    }
  }

  private JARRecord generateJARRecord(File jar) {
    try {
      InputStream is = new FileInputStream(jar);
      byte[] digest = StreamDigestor.digest(is);
      String hash = StreamDigestor.readableHash(digest);
      return new JARRecord(hash, jar.getName(), jar.lastModified());
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Error reading JAR file at path "
          + jar, e);
    }
  }

  private void removeRecord(JARRecord bad) {
    hashmap.remove(bad.hash);
  }

  private void addRecord(JARRecord record) {
    hashmap.put(record.hash, record);

  }

}
