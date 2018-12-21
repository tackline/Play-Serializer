package tackline.play.serialize;

import java.lang.reflect.*;
import java.util.*;

class FieldCommon {
   private FieldCommon() {
   }
   static <T> Constructor<T> nullaryConstructor(Class<T> clazz) {
      if (Modifier.isAbstract(clazz.getModifiers())) {
         throw new IllegalArgumentException("Cannot construct abstract class");
      }
      if (!Modifier.isPublic(clazz.getModifiers())) {
         throw new IllegalArgumentException("Cannot construct a non-public class");
      }
      try {
         Constructor<T>  ctor = clazz.getDeclaredConstructor();
         for (Class<?> excType : ctor.getExceptionTypes()) {
            if (!(
               Error.class.isAssignableFrom(excType) || 
               RuntimeException.class.isAssignableFrom(excType)
            )) {
               throw new IllegalArgumentException("Constructor throws unchecked exception");
            }
         }
         return ctor;
      } catch (NoSuchMethodException exc) {
         throw new IllegalArgumentException("We must have a nullary argument constructor");
      }
   }
   
   static List<Field> serialFields(Class<?> clazz) {
      List<Field> serialFields = new ArrayList<>();
      for (Field field : clazz.getDeclaredFields()) {
         int mods = field.getModifiers();
         if (!Modifier.isStatic(mods) && !Modifier.isTransient(mods)) {
            if (Modifier.isFinal(mods)) {
               throw new IllegalArgumentException("We can't set final fields");
            }
            java.security.AccessController.doPrivileged(
               (java.security.PrivilegedAction<Void>)() -> {
                  field.setAccessible(true);
                  return null;
               }
            );
            serialFields.add(field);
         }
      }
      return serialFields;
   }
}