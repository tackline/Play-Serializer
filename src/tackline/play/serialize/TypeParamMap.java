package tackline.play.serialize;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;

class TypeParamMap {
   private final Map<TypeVariable<? extends Class<?>>,Type> typeMap;
   TypeParamMap(TypeVariable<? extends Class<?>>[] typeParams, Type[] typeArgs) {
      if (typeParams.length != typeArgs.length) {
         throw new IllegalArgumentException("Type params not matching type args");
      }
      // !! Of course there's no real semantics defined for equals on TypeVariable
      this.typeMap = FieldCommon.zipMap(typeParams, typeArgs);
   }
   Type substitute(Type type) {
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
