package tackline.play.serialize;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

class ValueReflecter implements Reflecter {
   /*
   public Map<String,KnownType> properties(
      KnownType type, Class<?> clazz, KnownType[] typeArgs
   ) {
      TypeParamMap typeMap = new TypeParamMap(clazz, typeArgs);
      Set<Method> genesises = genesisMethods(type, clazz, typeMap);
      if (genesises.isEmpty()) {
         throw new IllegalArgumentException(
            "No genesis method for <"+clazz+">."
         );
      }
      Method genesis = genesises.iterator().next();
      return Arrays.stream(genesis.getParameters())
         .collect(Collectors.toMap(
            p -> p.getName(),
            p -> {
               String name = p.getName();
               // !! Method.getTypeParameters?
               KnownType newType = typeMap.substitute(p.getParameterizedType());
               try {
                  Method getter = clazz.getMethod(name);
                  if (!isSerialMethod(getter)) {
                     throw new IllegalArgumentException(
                        "getter method isn't"
                     );
                  }
                  KnownType oldType = typeMap.substitute(getter.getGenericReturnType());
                  if (!oldType.equals(newType)) {
                     throw new IllegalArgumentException(
                        "genesis vs getter method type mismatch"
                     );
                  }
                  return newType;
               } catch (NoSuchMethodException exc) {
                  throw new IllegalArgumentException(
                     "no matching getter"
                  );
               }
            }
         ));
   }
   */
   
   public Exploder oldObject(Class<?> clazz) {
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
         if (isSerialMethod(method)) {            serialMethods.add(method);
         }
      }
      return serialMethods;
   }
   static boolean isSerialMethod(Method method) {
      int mods = method.getModifiers();
      return
         !Modifier.isStatic(mods) &&
         Modifier.isPublic(mods) &&
         method.getReturnType() != void.class &&
         method.getParameterTypes().length == 0 &&
         !isSpecial(method.getName()) &&
         FieldCommon.isUnchecked(method.getGenericExceptionTypes()) && // !! Realise type params!
         !hasCallerSensitive(method.getAnnotations());
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
   public <T> Imploder newObject(
      KnownType type, Class<T> clazz, TypeParamMap typeMap
   ) {
      Set<Method> genesises = genesisMethods(type, clazz, typeMap);
      if (genesises.isEmpty()) {
         throw new IllegalArgumentException(
            "No genesis method for <"+clazz+">."
         );
      }
      Method genesis = genesises.iterator().next();
      Parameter[] params = genesis.getParameters();
      int paramNum = params.length;
      String[] names = new String[paramNum];
      for (int i=0; i<paramNum; ++i) {
         Parameter param = params[i];
         if (!param.isNamePresent()) {
            throw new IllegalArgumentException(
               "Param name not present in <"+genesis+">"
            );
         }
         String name = param.getName();
         names[i] = name;
      }
      
      return new Imploder() {
         @Override public Object implode(Map<String, Object> data) {
            Object[] args = new Object[paramNum];
            for (int i=0; i<paramNum; ++i) {
               String name = names[i];
               if (!data.containsKey(name)) {
                  throw new IllegalArgumentException(
                     "Name <"+name+"> not present in stream."
                  );
               }
               args[i] = data.get(name);
            }
            try {
               return clazz.cast(genesis.invoke(null, args));
            } catch (
               IllegalAccessException |
               IllegalArgumentException exc
            ) {
               // This can't happen.
               // !! We don't like this aspect of the reflection API.
               throw new Error(exc);
            } catch (InvocationTargetException exc) {
               throw FieldCommon.throwUnchecked(exc);
            }
         }
      };
   }
   private static Set<Method> genesisMethods(
      KnownType type, Class<?> clazz, TypeParamMap typeMap
   ) {
      if (!Modifier.isPublic(clazz.getModifiers())) {
         throw new IllegalArgumentException(
            "Cannot create from a non-public class"
         );
      }
      Set<Method> genesises = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
         int mods = method.getModifiers();
         if (
            Modifier.isStatic(mods) &&
            Modifier.isPublic(mods) &&
            method.getName().equals("of") &&
            typeMap.substitute(method.getGenericReturnType()).equals(type) &&
            FieldCommon.isUnchecked(method.getGenericExceptionTypes())
         ) {
            genesises.add(method);
         }
      }
      return genesises;
   }
}
