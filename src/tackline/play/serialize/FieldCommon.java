package tackline.play.serialize;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

class FieldCommon { // !! This name is no longer accurate.
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
         if (!isUnchecked(ctor.getGenericExceptionTypes())) {
            throw new IllegalArgumentException("Constructor throws unchecked exception");
         }
         return ctor;
      } catch (NoSuchMethodException exc) {
         throw new IllegalArgumentException("We must have a nullary argument constructor");
      }
   }
   static boolean isUnchecked(Type[] excTypes) {
      for (Type excType : excTypes) {
         if (!(excType instanceof Class<?>)) {
            return false;
         }
         Class<?> excClass = (Class<?>)excType;
         if (!(
            excClass.isAssignableFrom(RuntimeException.class) || // !! check
            excClass.isAssignableFrom(Error.class) // !! check
         )) {
            return false;
         }
      }
      return true;
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
   static <K,V> Map<K,V> zipMap(K[] keys, V[] values) {
      if (keys.length != values.length) {
         throw new IllegalArgumentException();
      }
      if (keys.length == 0) {
         return Collections.emptyMap();
      }
      Map<K,V> map = new HashMap<>();
      for (int i=0 ; i<keys.length; ++i) {
         map.put(keys[i], values[i]);
      }
      return Collections.unmodifiableMap(map);
   }
   
   static Error throwUnchecked(InvocationTargetException exc) {
      Throwable target = exc.getTargetException();
      if (target instanceof Error) {
         throw (Error)target;
      } else if (target instanceof RuntimeException) {
         throw (RuntimeException)target;
      } else {
         // Somebody has been very naughty.
         // !! We don't like this API - we should have already checked.
         throw new Error(target);
      }
   }
   /** Safe, but only if T is also safe. */
   public static <T> List<T> safe(List<T> unsafe) {
      return Collections.unmodifiableList(new ArrayList<>(unsafe));
   }
   static ObjectFormat format(Class<?> clazz) {
      Map<String,Field> nameFields = serialFields(clazz).stream()
         .collect(Collectors.toMap(f -> f.getName(), f -> f));
      List<String> names = new ArrayList<>(nameFields.keySet());
      List<DataFormat> dataFormats = new ArrayList<>();
      List<Type> types = new ArrayList<>();
      for (String name : names) {
         Type fieldType = nameFields.get(name).getGenericType();
         dataFormats.add(
             fieldType == boolean.class ? DataFormat.BOOLEAN :
             fieldType == byte   .class ? DataFormat.BYTE    :
             fieldType == char   .class ? DataFormat.CHAR    :
             fieldType == short  .class ? DataFormat.SHORT   :
             fieldType == int    .class ? DataFormat.INT     :
             fieldType == long   .class ? DataFormat.LONG    :
             fieldType == float  .class ? DataFormat.FLOAT   :
             fieldType == double .class ? DataFormat.DOUBLE  :
                                          DataFormat.REF
          );
          types.add(fieldType); // !! For REF only
      }
      return ObjectFormat.of(names, dataFormats, types);
   }
}