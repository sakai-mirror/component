/*
 * Created on 1 Dec 2007
 */
package org.sakaiproject.component.util;

import java.io.IOException;
import java.io.InputStream;

import sun.security.provider.SHA;

public class StreamDigestor {
  /**
   * Returns a byte array representing an SHA-1 digest of the supplied stream.
   * This method closes the supplied stream.
   * 
   * @param stream The stream to be digested
   * @return A byte array holding the required digest.
   * @exception IOException if an I/O error occurs.
   */
  public static byte[] digest(InputStream stream) throws IOException {
    SHA sha = new SHA();
    byte[] buffer = new byte[StreamCopyUtil.PROCESS_BUFFER_SIZE];
    try {
      while (true) {
        int bytesread = stream.read(buffer);
        if (bytesread > 0) {
          sha.update(buffer, 0, bytesread);
        }
        if (bytesread == -1)
          break;
      }
    }
    finally {
      stream.close();
    }
    return sha.digest();
  }

  public static String readableHash(byte[] hash) {
    StringBuilder togo = new StringBuilder();
    ByteToCharBase64.writeBytes(togo, hash, 0, 18, false);
    return togo.toString();
  }
  
}
