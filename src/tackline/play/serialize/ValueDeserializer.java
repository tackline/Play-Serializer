package tackline.play.serialize;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class ValueDeserializer extends FieldDeserializer {
   /* pp */ ValueDeserializer(DataInput in) {
      super(in);
   }
   public static <T> T deserialize(
      DataInput in, Class<T> clazz
   ) throws IOException {
      return clazz.cast(new ValueDeserializer(in).deserialize(clazz));
   }
   @Override /* pp */ <T> Imploder newObject(
      Type type, Class<T> clazz, TypeParamMap typeMap
   ) throws IOException {
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
//      Map<String,Type> types = new HashMap<>();
      for (int i=0; i<paramNum; ++i) {
         Parameter param = params[i];
         if (!param.isNamePresent()) {
            throw new IllegalArgumentException(
               "Param name not present in <"+genesis+">"
            );
         }
         String name = param.getName();
         names[i] = name;
//         types.put(name, typeMap.substitute(param.getParameterizedType()));
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
      Type type, Class<?> clazz, TypeParamMap typeMap
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
            typeMap.substitute(method.getGenericReturnType()).equals(typeMap.substitute(type)) &&
            FieldCommon.isUnchecked(method.getGenericExceptionTypes())
         ) {
            genesises.add(method);
         }
      }
      return genesises;
   }
}
