package tackline.play.serialize;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

class FieldReflecter implements Reflecter {
   /*
   public Map<String,KnownType> properties(
      Class<?> clazz, KnownType[] args
   ) {
      TypeParamMap typeMap = new TypeParamMap(clazz, args);
      return
         FieldCommon.serialFields(clazz).stream()
            .collect(Collectors.toMap(
               f -> f.getName(),
               f -> typeMap.substitute(f.getGenericType())
            ));
   }
*/
   public Exploder oldObject(Class<?> clazz) {
      List<Field> fields = FieldCommon.serialFields(clazz);
      List<String> names = new ArrayList<>();
      for (Field field : fields) {
         names.add(field.getName());
      }
      List<String> names_ = Collections.unmodifiableList(names); // sigh sigh
      return new Exploder() {
         public List<String> names() {
            return names_;
         }
         public List<Object> explode(Object obj) {
            List<Object> data = new ArrayList<>();
            for (Field field : fields) {
               try {
                  data.add(field.get(obj));
               } catch (IllegalAccessException exc) {
                  // This can't happen.
                  // !! We don't like this aspect of the reflection API.
                  throw new Error(exc);
               }
            }
            return data;
         }
      };
   }
   // !! type & tpyeMap for ValueDeserializer
   public <T> Imploder newObject(KnownType type, Class<T> clazz, TypeParamMap typeMap) {
      Constructor<T> ctor = FieldCommon.nullaryConstructor(clazz);
      java.security.AccessController.doPrivileged(
         (java.security.PrivilegedAction<Void>)() -> {
            ctor.setAccessible(true);
            return null;
         }
      );
      Map<String,Field> nameFields =
         FieldCommon.serialFields(clazz).stream()
            .collect(Collectors.toMap(f -> f.getName(), f -> f));

      return new Imploder() {
         @Override public Object implode(Map<String, Object> data) {
            T obj;
            try {
               obj = ctor.newInstance();
            } catch (
               InstantiationException |
               IllegalAccessException |
               IllegalArgumentException exc
            ) {
               // !! We don't like this API - this cannot happen...
               throw new Error(exc);
            } catch (InvocationTargetException exc) {
               throw FieldCommon.throwUnchecked(exc);
            }
            for (
               Map.Entry<String,Field> entry : nameFields.entrySet()
            ) {
               String name = entry.getKey();
               if (!data.containsKey(name)) {
                  throw new IllegalArgumentException(
                     "Name not present in stream."
                  );
               }
               try {
                  entry.getValue().set(obj, data.get(name));
               } catch (IllegalAccessException exc) {
                  // This can't happen.
                  // !! We don't like this aspect of the reflection API.
                  throw new Error(exc);
               }
            }
            return obj;
         }
      };
   }
}
