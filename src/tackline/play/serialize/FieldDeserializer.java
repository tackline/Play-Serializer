package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

public final class FieldDeserializer {
   private static class Ref {
      private final Class<?> type;
      private final Object obj;
      public <T> Ref(Class<T> type, T obj) {
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
      return new FieldDeserializer(in).deserialize(clazz);
   }
   public <T> T deserialize(Class<T> clazz) throws IOException {
      String clazzName = in.readUTF();
      if (clazzName.equals("%")) {
         return backRef(clazz);
      } else if (clazzName.equals("!")) {
         matchType(clazz.getName(), in.readUTF());
         return null;
      } else {
         matchType(clazz.getName(), clazzName);
         T obj = clazz.isArray() ? array(clazz) : object(clazz);
         labelForBackRef(clazz, obj);
         return obj;
      }
   }
   private <T> T backRef(Class<T> clazz) throws IOException {
      long id = in.readLong();
      Ref ref = backRefs.get(id);
      if (ref == null) {
         throw new IOException("Read an non-existent back ref");
      }
      if (ref.type != clazz) {
         throw new IOException("Back ref is of the wrong type");
      }
      return (T)ref.obj;
   }
   private <T> void labelForBackRef(Class<T> clazz, T obj) throws IOException {
      backRefs.put(in.readLong(), new Ref(clazz, obj));
   }
   private <T> T object(Class<T> clazz) throws IOException {
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
         Class<?> type = field.getType();
         try {
            if (type.isPrimitive()) {
               String sig = in.readUTF();
               matchType(type.getName(), sig);
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
               } else if (type.isArray()) {
                  field.set(obj, array(type));
               } else {
                  throw new Error("Unknown primitive type");
               }
            } else {
               field.set(obj, deserialize(type));
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
}
