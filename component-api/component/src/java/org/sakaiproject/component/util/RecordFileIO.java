/*
 * Created on 2 Dec 2007
 */
package org.sakaiproject.component.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import org.sakaiproject.component.impl.support.JARRecord;

public class RecordFileIO {
  public static <T> void writeRecordFile(String filename, Iterable<T> recordit,
      RecordWriter<T> writer, String message) {
    File towrite = new File(filename);
    BufferedWriter bw = null;
    try {
      bw = new BufferedWriter(new FileWriter(towrite));
      for (T record : recordit) {
        writer.writeRecord(bw, record);
      }
    }
    catch (Exception e) {
      throw new IllegalArgumentException(message, e);
    }
    finally {
      if (bw != null) {
        try {
          bw.close();
        }
        catch (Throwable t) {
        }
      }
    }
  }

  public static <T> void readRecordFile(String filename, RecordReader reader,
      String message, String deletefailmessage) {
    File toread = new File(filename);
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(toread));
      while (true) {
        String line = br.readLine();
        String[] fields = line.split("\t");
        reader.readRecord(fields);
      }
    }
    catch (Exception e) {
      try {
        toread.delete();
      }
      catch (Exception e2) {
        throw new IllegalArgumentException(deletefailmessage + filename, e2);
      }
      throw new IllegalArgumentException(message + filename, e);

    }
    finally {
      if (br != null) {
        try {
          br.close();
        }
        catch (Throwable t) {
        }
      }
    }
  }
}
