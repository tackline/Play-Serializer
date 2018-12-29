package tackline.play.serialize;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValueSerializer extends FieldSerializer {
   /* pp */ ValueSerializer(DataOutput out) {
      super(out);
   }
   public static <T> void serialize(DataOutput out, Class<T> clazz, T obj) throws IOException {
      new ValueSerializer(out).serialize(clazz, obj);
   }
   
   @Override /* pp */ Exploder oldObject(Class<?> clazz) throws IOException {
      //TypeParamMap typeMap = new TypeParamMap(clazz, typeArgs);
      // !! We could check whether it has the accompanying genesis method.
      // !! We don't do class hierarchies.
      
      List<Method> methods = serialMethods(clazz);
      List<String> names = new ArrayList<>();
      for (Method method : methods) {
         names.add(method.getName());
      }
      List<String> names_ = Collections.unmodifiableList(names); // sigh sigh
      
      return new Exploder() {
         public List<String> names() {
            return names_;
         }
         public List<Object> explode(Object obj) {
            List<Object> data = new ArrayList<>();
            for (Method method : methods) {
               try {
                  data.add(method.invoke(obj));
               } catch (InvocationTargetException exc) {
                  throw FieldCommon.throwUnchecked(exc);
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
   
   
   static List<Method> serialMethods(Class<?> clazz) {
      List<Method> serialMethods = new ArrayList<>();
      for (Method method : clazz.getDeclaredMethods()) {
         int mods = method.getModifiers();
         if (
            !Modifier.isStatic(mods) &&
            Modifier.isPublic(mods) &&
            method.getReturnType() != void.class &&
            method.getParameterTypes().length == 0 &&
            !isSpecial(method.getName()) &&
            FieldCommon.isUnchecked(method.getGenericExceptionTypes()) && // !! Realise type params!
            !hasCallerSensitive(method.getAnnotations())
         ) {
            serialMethods.add(method);
         }
      }
      return serialMethods;
   }
   private static boolean hasCallerSensitive(Annotation[] annotations) {
      for (Annotation annotation : annotations) {
         // !! This will have to do for now.
         if (annotation.annotationType().getSimpleName().equals("CallerSensitive")) {
            return true;
         }
      }
      return false;
   }
   private static boolean isSpecial(String methodName) {
      return
          methodName.equals("hashCode") ||
          methodName.equals("toString") ||
          methodName.equals("clone") || // Not public/has exception.
          methodName.equals("wait") || // Has exception/void return.
          methodName.equals("notify") || // void return.
          methodName.equals("notifyAll"); // void return.
   }
}
