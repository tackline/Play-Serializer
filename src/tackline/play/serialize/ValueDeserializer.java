package tackline.play.serialize;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class ValueDeserializer extends FieldDeserializer {
   /* pp */ ValueDeserializer(DataInput in) {
      super(in);
   }
   public static <T> T deserialize(DataInput in, Class<T> clazz) throws IOException {
      return clazz.cast(new ValueDeserializer(in).deserialize(clazz));
   }
   @Override /* pp */ <T> T object(Type type, Class<T> clazz, Type[] typeArgs) throws IOException {
      TypeParamMap typeMap = new TypeParamMap(clazz, typeArgs);
      Set<Method> genesises = genesisMethods(type, clazz, typeMap);
      if (genesises.isEmpty()) {
         throw new IllegalArgumentException("No genesis method for <"+clazz+">.");
      }
      Method genesis = genesises.iterator().next();
      Parameter[] params = genesis.getParameters();
      int paramNum = params.length;
      String[] names = new String[paramNum];
      Map<String,Type> types = new HashMap<>();
      for (int i=0; i<paramNum; ++i) {
         Parameter param = params[i];
         if (!param.isNamePresent()) {
            throw new IllegalArgumentException("Param name not present in <"+genesis+">");
         }
         String name = param.getName();
         names[i] = name;
         types.put(name, typeMap.substitute(param.getParameterizedType()));
      }
      Map<String, Object> nameArgs = new HashMap<>();
      for (;;) {
         String name = in.readUTF();
         if (name.equals(".")) {
            break;
         }
         Type propertyType = types.get(name);
         if (propertyType == null) {
            throw exc("Unexpected property name in stream <"+name+">");
         }
         final Object fieldObj;
         if (propertyType instanceof Class<?> && ((Class<?>)propertyType).isPrimitive()) {
            if (propertyType == boolean.class) {
               fieldObj = in.readBoolean();
            } else if (propertyType == byte.class) {
               fieldObj = in.readByte();
            } else if (propertyType == char.class) {
               fieldObj = in.readChar();
            } else if (propertyType == short.class) {
               fieldObj = in.readShort();
            } else if (propertyType == int.class) {
               fieldObj = in.readInt();
            } else if (propertyType == long.class) {
               fieldObj = in.readLong();
            } else if (propertyType == float.class) {
               fieldObj = in.readFloat();
            } else if (propertyType == double.class) {
               fieldObj = in.readDouble();
            } else {
               throw new Error("Unknown primitive type");
            }
         } else {
            fieldObj =  deserialize(propertyType);
         }
         nameArgs.put(name, fieldObj);
      }
      Object[] args = new Object[paramNum];
      for (int i=0; i<paramNum; ++i) {
         String name = names[i];
         if (!nameArgs.containsKey(name)) {
            throw exc("Name not present in stream.");
         }
         args[i] = nameArgs.get(name);
      }
      
      try {
         return clazz.cast(genesis.invoke(null, args));
      } catch (IllegalAccessException | IllegalArgumentException exc) {
         // This can't happen.
         // !! We don't like this aspect of the reflection API.
         throw new Error(exc);
      } catch (InvocationTargetException exc) {
         throw FieldCommon.throwUnchecked(exc);
      }
   }
   static Set<Method> genesisMethods(Type type, Class<?> clazz, TypeParamMap typeMap) {
      if (!Modifier.isPublic(clazz.getModifiers())) {
         throw new IllegalArgumentException("Cannot create from a non-public class");
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
