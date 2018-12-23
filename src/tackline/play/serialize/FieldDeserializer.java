package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

public final class FieldDeserializer {
   private static class Ref {
      private final Class<?> type;
      private final Object obj;
      public /*<T>*/ Ref(Class<?> type, Object obj) {
         this.type = type;
         this.obj = obj;
      }
   }
   private final Map<Long,Ref> backRefs = new HashMap<>();
   private final DataInput in;
   private FieldDeserializer(DataInput in) {
      this.in = in;
   }
   public static <T> T deserialize(DataInput in, Class<T> clazz) throws IOException {
      return clazz.cast(new FieldDeserializer(in).deserialize(clazz));
   }
   public Object deserialize(Type type) throws IOException {
      if (type instanceof Class<?>) {
         return refType((Class<?>)type, new Type[0]);
      } else if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)type;
         Type rawType = parameterizedType.getRawType();
         if (rawType instanceof Class<?>) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            return refType((Class<?>)rawType, typeArgs);
         } else {
            throw exc("Don't know what that raw type is supposed to be");
         }
      } else {
         throw exc("Type <"+type.getClass()+"> of Type not supported, <"+type+">");
      }
   }
   private Object refType(Class<?> clazz, Type[] typeArgs) throws IOException {
      String clazzName = in.readUTF();
      if (clazzName.equals("%")) {
         // !! Doesn't check type.
         return backRef(clazz);
      } else if (clazzName.equals("!")) {
         matchType(clazz.getName(), in.readUTF());
         return null;
      } else {
         matchType(clazz.getName(), clazzName);
         Object obj = clazz.isArray() ? array(clazz) : object(clazz, typeArgs);
         labelForBackRef(clazz, obj);
         return obj;
      }
   }
   private Object backRef(Class<?> clazz) throws IOException {
      long id = in.readLong();
      Ref ref = backRefs.get(id);
      if (ref == null) {
         throw new IOException("Read an non-existent back ref");
      }
      if (ref.type != clazz) {
         throw new IOException("Back ref is of the wrong type");
      }
      return ref.obj;
   }
   private void labelForBackRef(Class<?> clazz, Object obj) throws IOException {
      backRefs.put(in.readLong(), new Ref(clazz, obj));
   }
   private <T> T object(Class<T> clazz, Type[] typeArgs) throws IOException {
      TypeVariable<?>[] typeParams = clazz.getTypeParameters();
      if (typeParams.length != typeArgs.length) {
         throw exc("Class with type params not matching type args");
      }
      Constructor<T> ctor = FieldCommon.nullaryConstructor(clazz);
      java.security.AccessController.doPrivileged(
         (java.security.PrivilegedAction<Void>)() -> {
            ctor.setAccessible(true);
            return null;
         }
      );
      T obj;
      try {
         obj = ctor.newInstance();
      } catch (InstantiationException | IllegalAccessException | IllegalArgumentException exc) {
         // !! We don't like this API - this cannot happen...
         throw new Error(exc);
      } catch (InvocationTargetException exc) {
         Throwable target = exc.getTargetException();
         if (target instanceof Error) {
            throw (Error)target;
         } else if (target instanceof RuntimeException) {
            throw (RuntimeException)target;
         } else {
            // Somebody has been very naughty.
            // !! We don't like this API - we have already checked.
            throw new Error(target);
         }
      }
      // !! We don't do class hierarchies.
      Map<String,Field> nameFields = FieldCommon.serialFields(clazz).stream()
         .collect(Collectors.toMap(f -> f.getName(), f -> f));
      for (;;) {
         String name = in.readUTF();
         if (name.equals(".")) {
            break;
         }
         Field field = nameFields.get(name);
         if (field == null) {
            // Java Serialization just ignores this. (Also the XML way.)
            throw new IOException("field <"+name+"> in stream not in class");
         }
         //Class<?> type = field.getType();
         Type type = field.getGenericType();
         try {
            if (type instanceof Class<?> && ((Class<?>)type).isPrimitive()) {
               String sig = in.readUTF();
               matchType(((Class<?>)type).getName(), sig);
               if (type == boolean.class) {
                  field.setBoolean(obj, in.readBoolean());
               } else if (type == byte.class) {
                  field.setByte(obj, in.readByte());
               } else if (type == char.class) {
                  field.setChar(obj, in.readChar());
               } else if (type == short.class) {
                  field.setShort(obj, in.readShort());
               } else if (type == int.class) {
                  field.setInt(obj, in.readInt());
               } else if (type == long.class) {
                  field.setLong(obj, in.readLong());
               } else if (type == float.class) {
                  field.setFloat(obj, in.readFloat());
               } else if (type == double.class) {
                  field.setDouble(obj, in.readDouble());
               } else if (((Class<?>)type).isArray()) {
                  field.set(obj, array(((Class<?>)type)));
               } else {
                  throw new Error("Unknown primitive type");
               }
            } else {
               if (type instanceof TypeVariable<?>) {
                  typeParam: {
                     for (int i=0; i<typeParams.length; ++i) {
                        if (type.equals(typeParams[i])) {
                           field.set(obj, deserialize(typeArgs[i]));
                           break typeParam;
                        }
                     }
                     throw exc("Field's type variable not found");
                  }
               } else {
                  field.set(obj, deserialize(type));
               }
            }
         } catch (IllegalAccessException exc) {
            // This can't happen.
            // !! We don't like this aspect of the reflection API.
            throw new Error(exc);
         }
      }
      return obj;
   }
   private <T> T array(Class<T> type) throws IOException {
      int len = in.readInt();
      if (type == boolean[].class) {
         boolean[] fieldObj = new boolean[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readBoolean();
         }
         return (T)fieldObj;
      } else if (type == byte[].class) {
         byte[] fieldObj = new byte[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readByte();
         }
         return (T)fieldObj;
      } else if (type == char[].class) {
         char[] fieldObj = new char[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readChar();
         }
         return (T)fieldObj;
      } else if (type == short[].class) {
         short[] fieldObj = new short[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readShort();
         }
         return (T)fieldObj;
      } else if (type == int[].class) {
         int[] fieldObj = new int[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readInt();
         }
         return (T)fieldObj;
      } else if (type == long[].class) {
         long[] fieldObj = new long[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readLong();
         }
         return (T)fieldObj;
      } else if (type == float[].class) {
         float[] fieldObj = new float[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readFloat();
         }
         return (T)fieldObj;
      } else if (type == double[].class) {
         double[] fieldObj = new double[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readDouble();
         }
         return (T)fieldObj;
      } else {
         Class<?> componentType = type.getComponentType();
         Object fieldObj = Array.newInstance(componentType, len);
         for (int i=0; i<len; ++i) {
            Array.set(fieldObj, i, deserialize(componentType));
         }
         return (T)fieldObj;
      }
   }
   private static void matchType(String expected, String actual) throws IOException {
      if (!expected.equals(actual)) {
         throw new IOException("Type mismatch");
      }
   }
   // Actually throws the exception instead of returning it, just in case we forget.
   public static IOException exc(String msg) throws IOException {
      throw new IOException(msg);
   }
}
