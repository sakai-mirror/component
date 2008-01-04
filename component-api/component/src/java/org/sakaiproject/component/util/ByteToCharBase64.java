/*
 * Created on 1 Dec 2007
 */
package org.sakaiproject.component.util;

/** This class encodes supplied byte data as characters in a modified Base64 system.
 * It accepts data as byte arrays and native types, which are written direct
 * to a supplied CharWrap as plain ASCII characters.
 */
public class ByteToCharBase64 {
  /** This array maps the characters to their 6-bit values */
  public final static char pem_array[] = {
 //  0   1   2   3   4   5   6   7
    'A','B','C','D','E','F','G','H', // 0
    'I','J','K','L','M','N','O','P', // 1
    'Q','R','S','T','U','V','W','X', // 2
    'Y','Z','a','b','c','d','e','f', // 3
    'g','h','i','j','k','l','m','n', // 4
    'o','p','q','r','s','t','u','v', // 5
    'w','x','y','z','0','1','2','3', // 6
    '4','5','6','7','8','9','(','_'  // 7
    };

  /** Writes the supplied byte data to the supplied CharWrap in Base64-encoded
   * form.
   * @param out The CharWrap to receive the encoded characters.
   * @param data A byte array holding the data to be encoded.
   * @param offset The offset within the byte array of the data to be encoded.
   * @param len The length of the data to be encoded.
   * @param pad <code>true</code> if the output data should be right-padded 
   * with the <code>A</code> character. NB, the Base64 standard is actually
   * to right-pad with the <code>=</code> character, look into this.
   */
  public static void writeBytes(StringBuilder out, byte data[], int offset, int len,
                boolean pad) {
    byte b0, b1, b2;
    int index;
    while (len > 0) {
      // not correct! This should be right-aligned!!!
      int thislen = len % 3;
      if (thislen == 1) { // 1 byte - send out aaaaaabb 0000
    b0 = data[offset];
    index = (b0 >>> 2) & 0x3F;
    if (pad || index != 0) {out.append(pem_array[index]); pad = true;}
    index = (b0 << 4) & 0x30;
    if (pad || index != 0) {out.append(pem_array[index]); pad = true;}
    } 
      else if (thislen == 2) { // 2 byte - aaaaaabb bbbbcccc 00
    b0 = data[offset];
    b1 = data[offset+1];
    index = (b0 >>> 2) & 0x3F;
    if (pad || index != 0) {out.append(pem_array[index]); pad = true;}
    index = ((b0 << 4) & 0x30) + ((b1 >>> 4) & 0xf);
    if (pad || index != 0) {out.append(pem_array[index]); pad = true;}
    index = (b1 << 2) & 0x3c;
    if (pad || index != 0) {out.append(pem_array[index]); pad = true;}
    } 
      else { // 3 byte - aaaaaabb bbbbcccc ccdddddd
    b0 = data[offset];
    b1 = data[offset+1];
    b2 = data[offset+2];
    index = (b0 >>> 2) & 0x3F;
    if (pad || index != 0) {out.append(pem_array[index]); pad = true;}
    index = ((b0 << 4) & 0x30) + ((b1 >>> 4) & 0xf);
    if (pad || index != 0) {out.append(pem_array[index]); pad = true;}
    index = ((b1 << 2) & 0x3c) + ((b2 >>> 6) & 0x3);
    if (pad || index != 0) {out.append(pem_array[index]); pad = true;}
    index = b2 & 0x3F;
    if (pad || index != 0) {out.append(pem_array[index]); pad = true;}
    }
      len -= 3; offset += 3;
      }
    }
}
