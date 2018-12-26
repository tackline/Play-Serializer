package tackline.play.serialize;

import java.io.DataOutput;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

public class ValueSerializer extends FieldSerializer {
   /* pp */ ValueSerializer(DataOutput out) {
      super(out);
   }
   public static <T> void serialize(DataOutput out, Class<T> clazz, T obj) throws IOException {
      new ValueSerializer(out).serialize(clazz, obj);
   }
   
   @Override /* pp */ void object(Class<?> clazz, Type[] typeArgs, Object obj) throws IOException {
      TypeParamMap typeMap = new TypeParamMap(clazz, typeArgs);
      @SuppressWarnings("unused")
      Constructor<?> ctor = FieldCommon.nullaryConstructor(clazz); // !! This will go.
      // !! We don't do class hierarchies.
      
      for (Method method : serialMethods(clazz)) {
         out.writeUTF(method.getName());
         Type type = method.getGenericReturnType();
         try {
            if (type instanceof Class<?> && ((Class<?>)type).isPrimitive()) {
               if (type == boolean.class) {
                  out.writeBoolean((Boolean)method.invoke(obj));
               } else if (type == byte.class) {
                  out.writeByte((Byte)method.invoke(obj));
               } else if (type == char.class) {
                  out.writeChar((Character)method.invoke(obj));
               } else if (type == short.class) {
                  out.writeShort((Short)method.invoke(obj));
               } else if (type == int.class) {
                  out.writeInt((Integer)method.invoke(obj));
               } else if (type == long.class) {
                  out.writeLong((Long)method.invoke(obj));
               } else if (type == float.class) {
                  out.writeFloat((Float)method.invoke(obj));
               } else if (type == double.class) {
                  out.writeDouble((Double)method.invoke(obj));
               } else {
                  throw new Error("Unknown primitive type");
               }
            } else {
               Object propertyObj = method.invoke(obj);
               serialize(typeMap.substitute(type), propertyObj);
            }
         } catch (InvocationTargetException exc) {
            throw FieldCommon.throwUnchecked(exc);
         } catch (IllegalAccessException exc) {
            // This can't happen.
            // !! We don't like this aspect of the reflection API.
            throw new Error(exc);
         }
      }
      out.writeUTF("."); // End of class indicator.
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
            isUnchecked(method.getGenericExceptionTypes()) && // !! Realise type params!
            !hasCallerSensitive(method.getAnnotations())
         ) {
            serialMethods.add(method);
         }
      }
      return serialMethods;
   }
   private static boolean isUnchecked(Type[] excTypes) {
      for (Type excType : excTypes) {
         if (!(excType instanceof Class<?>)) {
            return false;
         }
         Class<?> excClass = (Class<?>)excType;
         if (!(
            excClass.isAssignableFrom(RuntimeException.class) || // !! check
            excClass.isAssignableFrom(Error.class) // !! check
         )) {
            return false;
         }
      }
      return true;
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
