package tackline.play.serialize;

import java.lang.reflect.*;
import java.util.*;

/** A type without an type variables or wildcards. */
abstract class KnownType {
   private KnownType() {}
//   public static KnownType of(Type type) {
//      if (type instanceof Class<?>) {
//         return clazz((Class<?>)type);
//      } else if (type instanceof ParameterizedType) {
//         ParameterizedType paramType = (ParameterizedType)type;
//         // Does this case always work - or can there be String<>?
//         Class<?> rawType = (Class<?>)(paramType.getRawType());
//         Type[] typeArgs = paramType.getActualTypeArguments();
//         int len = typeArgs.length;
//         KnownType[] args = new KnownType[len];
//         for (int i=0; i<len; ++i) {
//            args[i] = of(typeArgs[i]);
//         }
//         //return new ProxyType(type);
//         return param(rawType, args);
//      } else if (type instanceof GenericArrayType) {
//         GenericArrayType arrayType = (GenericArrayType)type;
//         KnownType component = of(arrayType.getGenericComponentType());
//         return array(component);
//      } else {
//         throw new IllegalArgumentException(
//           "<"+type+"> has type "+(type.getClass())
//          );
//      }
//   }
   public static KnownType clazz(Class<?> clazz) {
      return new KnownType() {
         /*
         public Type type() {
            return clazz;
         }
         */
         public <R, EXC extends Throwable> R extractParameters(
            ParameterExtract<R, EXC> extracted
         ) throws EXC {
            return clazz.isArray() ?
               extracted.array(KnownType.clazz(clazz.getComponentType())) :
               extracted.class_(clazz, new KnownType[0]);
         }
         public Object newArray(int len) {
            return Array.newInstance(clazz, len);
         }
         Class<?> getRawType() {
            return clazz;
         }
      };
   }
   // (top-level -> owner type is null)
   public static KnownType param(
      Class<?> rawType, KnownType[] args
   ) {
      if (rawType.isArray()) {
         throw new IllegalArgumentException("Parameterized array??");
      }
      if (rawType.getTypeParameters().length != args.length) {
         // !! should also check compatibility.
         throw new IllegalArgumentException("Wrong number of params");
      }
      return new KnownType() {
         /*
         public Type type() {
            return new ParameterizedType() {
               public Type[] getActualTypeArguments() {
                  int len = args.length;
                  Type[] copy = new Type[len];
                  for (int i=0; i<len; ++i) {
                     copy[i] = args[i].type();
                  }
                  return copy;
               }
               public Type getRawType() {
                  return rawType;
               }
               public Type getOwnerType() {
                  return null;
               }
               // !! Forget equals...
            };
         }
         */
         public  <R, EXC extends Throwable> R extractParameters(
            ParameterExtract<R, EXC> extracted
         ) throws EXC {
            return extracted.class_(rawType, args);
         }
         public Object newArray(int len) {
            return Array.newInstance(rawType, len);
         }
         Class<?> getRawType() {
            return rawType;
         }
      };
   }
   public static KnownType array(
      KnownType component
   ) {
      // !! check length
      return new KnownType() {
         /*
         public Type type() {
            return new GenericArrayType() {
               public Type getGenericComponentType() {
                  return component.type();
               }
               // !! Forget equals...
            };
         }
         */
         public <R, EXC extends Throwable> R extractParameters(
            ParameterExtract<R, EXC> extracted
         ) throws EXC {
            return extracted.array(component);
         }
         public Object newArray(int len) {
            // This works for all really.
            return Array.newInstance(getRawType(), len);
         }
         Class<?> getRawType() {
            // Got a better way of doing this?
            return Array.newInstance(component.getRawType(), 0).getClass();
         }
      };
   }
   
   abstract Class<?> getRawType();
   
//   public abstract Type type();
   
   public abstract Object newArray(int len);
   
   public abstract <R, EXC extends Throwable> R extractParameters(
      ParameterExtract<R, EXC> extracted
   ) throws EXC;
   
   @Override public boolean equals(Object obj) {
      if (!(obj instanceof KnownType)) {
         return false;
      }
      KnownType other = (KnownType)obj;
      return extractParameters(new ParameterExtract<Boolean,Error>() {
         public Boolean class_(Class<?> rawType, KnownType[] args) {
            return other.extractParameters(new ParameterExtract<Boolean,Error>() {
               public Boolean class_(Class<?> rawTypeOther, KnownType[] argsOther) {
                  return rawType==rawTypeOther && Arrays.equals(args, argsOther);
               }
               public Boolean array(KnownType componentOther) {
                  return false;
               }
            });
         }
         public Boolean array(KnownType component) {
            return other.extractParameters(new ParameterExtract<Boolean,Error>() {
               public Boolean class_(Class<?> rawTypeOther, KnownType[] argsOther) {
                  return false;
               }
               public Boolean array(KnownType componentOther) {
                  return component.equals(componentOther);
               }
            });
         }
      });
   }
   public int hashCode() {
      return 1;
   }
}
