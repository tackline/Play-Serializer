package tackline.play.serialize;

import java.lang.reflect.*;
import java.util.*;

class TypeParamMap {
   private final Map<String,KnownType> typeMap;
   TypeParamMap(Class<?> clazz, KnownType[] args) {
      TypeVariable<? extends Class<?>>[] typeParams = clazz.getTypeParameters();
      int len = typeParams.length;
      if (len != args.length) {
         throw new IllegalArgumentException("Type params not matching type args");
      }
      String[] names = new String[len];
      for (int i=0; i<len; ++i) {
         names[i] = typeParams[i].getName();
      }
      this.typeMap = FieldCommon.zipMap(names, args);
   }
   KnownType substitute(Type type) {
      if (type instanceof TypeVariable<?>) {
         TypeVariable<?> var = (TypeVariable<?>)type;
         KnownType actualType = typeMap.get(var.getName());
         if (actualType instanceof TypeVariable<?>) { throw null; }
         if (actualType == null) {
            throw new IllegalArgumentException("Type variable not found");
         } else if (actualType instanceof TypeVariable<?>) {
            throw new IllegalArgumentException("TypeVariable shouldn't substitute for a TypeVariable");
         } else {
            return actualType;
         }
      } else if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType =
            (ParameterizedType)type;
         // This will always be a Class, wont it? No higher-kinded types here, thank you very much.
         Class<?> rawType = (Class<?>)parameterizedType.getRawType();
         Type[] actualTypeArguments =
            parameterizedType.getActualTypeArguments();
         int len = actualTypeArguments.length;
         KnownType[] args = new KnownType[len];
         for (int i=0; i<len; ++i) {
            args[i] = substitute(actualTypeArguments[i]);
         }
         return KnownType.param(rawType, args);
      } else if (type instanceof GenericArrayType) {
         GenericArrayType genericArrayType = (GenericArrayType)type;
         KnownType componentType = substitute(genericArrayType.getGenericComponentType());
         return KnownType.array(componentType);
      } else if (type instanceof Class<?>) {
         return KnownType.clazz((Class<?>)type);
      } else {
         throw new IllegalArgumentException(
            "<"+type+"> has type "+(type.getClass())
         );
      }
   }

}
