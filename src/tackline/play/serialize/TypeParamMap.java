package tackline.play.serialize;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

class TypeParamMap {
   private final Map<String,Type> typeMap;
   TypeParamMap(Class<?> clazz, Type[] typeArgs) {
      TypeVariable<? extends Class<?>>[] typeParams = clazz.getTypeParameters();
      int len = typeParams.length;
      if (len != typeArgs.length) {
         throw new IllegalArgumentException("Type params not matching type args");
      }
      String[] names = new String[len];
      for (int i=0; i<len; ++i) {
         names[i] = typeParams[i].getName();
      }
      this.typeMap = FieldCommon.zipMap(names, typeArgs);
   }
   Type substitute(Type type) {
      if (type instanceof TypeVariable<?>) {
         Type actualType = typeMap.get(((TypeVariable<?>)type).getName());
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
            actualActualTypeArguments[i] = substitute(actualTypeArguments[i]);
         }
         // This will always be a Class, wont it? No higher-kinded types here, thank you very much.
         Type actualRawType = substitute(parameterizedType.getRawType());
         Type actualOwnerType = substitute(parameterizedType.getOwnerType());
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
         Type actualComponentType = substitute(componentType);
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
