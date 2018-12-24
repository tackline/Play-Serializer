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
   
   static Type substituteTypeParams(
      Map<TypeVariable<? extends Class<?>>,Type> typeMap, Type type
   ) {
      if (type instanceof TypeVariable<?>) {
         Type actualType = typeMap.get(type);
         if (actualType instanceof TypeVariable<?>) { throw null; }
         if (actualType == null) {
            throw new IllegalArgumentException("Type variable not found");
         } else if (actualType instanceof TypeVariable<?>) {
            throw new IllegalArgumentException("TypeVariable shouldn't substitute for a TypeVariable");
         } else {
            return actualType;
         }
      } else if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)type;
         Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
         int len = actualTypeArguments.length;
         Type[] actualActualTypeArguments = new Type[len];
         for (int i=0; i<len; ++i) {
            actualActualTypeArguments[i] = substituteTypeParams(typeMap, actualTypeArguments[i]);
         }
         // This will always be a Class, wont it? No higher-kinded types here, thank you very much.
         Type actualRawType = substituteTypeParams(typeMap, parameterizedType.getRawType());
         Type actualOwnerType = substituteTypeParams(typeMap, parameterizedType.getOwnerType());
         return new ParameterizedType() {
            public Type[] getActualTypeArguments() {
               return actualActualTypeArguments.clone();
            }
            public Type getRawType() {
               return actualRawType;
            }
            public Type getOwnerType() {
               return actualOwnerType;
            }
         };
      } else if (type instanceof GenericArrayType) {
         GenericArrayType genericArrayType = (GenericArrayType)type;
         Type componentType = genericArrayType.getGenericComponentType();
         Type actualComponentType = substituteTypeParams(typeMap, componentType);
         if (actualComponentType instanceof TypeVariable<?>) { throw null; }
         return new GenericArrayType() {
            // !! getTypeName? toString? equals? hashCode?
            public Type getGenericComponentType() {
               return actualComponentType;
            }
         };
      } else {
         return type;
      }
   }
}