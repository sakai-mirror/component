/*
 * Created on 12 Nov 2007
 */
package org.sakaiproject.component.util;

import java.util.ArrayList;
import java.util.List;

import org.sakaiproject.component.util.ClassVisibilityRecord;

public class ClassLoaderUtil {
    public static boolean isVisible(Class<?> totest, ClassLoader loader) {
        // TODO - when we have "KoerperWelten", this test naturally becomes more complex
        ClassLoader its = totest.getClassLoader();
        if (its == null) return true; // "Some implementations represent the boostrap CL as null"
        while (loader != null) {
            if (its == loader) return true;
            loader = loader.getParent();
        }
        return false;
    }
    
    public static ClassVisibilityRecord getVisibilityRecord(Class<?> totest, ClassLoader loader) {
        ClassVisibilityRecord togo = new ClassVisibilityRecord();
        List<Class<?>> superclazzes = ReflectUtil.getSuperclasses(totest);
        List<Class<?>> visint = new ArrayList<Class<?>>();
        for (Class<?> clazz: superclazzes) {
            if (isVisible(clazz, loader)) {
                if (clazz.isInterface()) {
                    visint.add(clazz);
                }
                else if (togo.clazz == null) togo.clazz = clazz;
            }
        }
        togo.interfaces = visint.toArray(new Class<?>[visint.size()]);
        return togo;
    }
}
