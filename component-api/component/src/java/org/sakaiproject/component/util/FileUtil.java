/*
 * Created on 3 Dec 2007
 */
package org.sakaiproject.component.util;

import java.io.File;

public class FileUtil {
    /**
     * Test if this File is a valid components package directory.
     * 
     * @param dir
     *        The file to test
     * @return true if it is a valid components package directory, false if not.
     */
    public static boolean validComponentsPackage(File dir) {
        // valid if this is a directory with a WEB-INF directory below with a components.xml file
        if ((dir != null) && (dir.isDirectory()))
        {
            File webinf = new File(dir, "WEB-INF");
            if ((webinf != null) && (webinf.isDirectory()))
            {
                File xml = new File(webinf, "components.xml");
                if ((xml != null) && (xml.isFile()))
                {
                    return true;
                }
            }
        }

        return false;
    }
}
