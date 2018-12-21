package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

public final class FieldDeserializer {
   private final DataInput in;
   private FieldDeserializer(DataInput in) {
      this.in = in;
   }
   public static <T> T deserialize(DataInput in, Class<T> clazz) throws IOException {
      return new FieldDeserializer(in).deserialize(clazz);
   }
   // !! A bit long.
   public <T> T deserialize(Class<T> clazz) throws IOException {
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
         String sig = in.readUTF();
         matchType(type.getName(), sig);
         try {
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
               throw new IllegalArgumentException("!! We don't do reference types");
            }
         } catch (IllegalAccessException exc) {
            // This can't happen.
            // !! We don't like this aspect of the reflection API.
            throw new Error(exc);
         }
      }
      return obj;
   }
   private Object array(Class<?> type) throws IOException {
      Class<?> componentType = type.getComponentType();
      int len = in.readInt();
      if (componentType == boolean.class) {
         boolean[] fieldObj = new boolean[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readBoolean();
         }
         return fieldObj;
      } else if (componentType == byte.class) {
         byte[] fieldObj = new byte[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readByte();
         }
         return fieldObj;
      } else if (componentType == char.class) {
         char[] fieldObj = new char[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readChar();
         }
         return fieldObj;
      } else if (componentType == short.class) {
         short[] fieldObj = new short[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readShort();
         }
         return fieldObj;
      } else if (componentType == int.class) {
         int[] fieldObj = new int[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readInt();
         }
         return fieldObj;
      } else if (componentType == long.class) {
         long[] fieldObj = new long[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readLong();
         }
         return fieldObj;
      } else if (componentType == float.class) {
         float[] fieldObj = new float[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readFloat();
         }
         return fieldObj;
      } else if (componentType == double.class) {
         double[] fieldObj = new double[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readDouble();
         }
         return fieldObj;
      } else if (componentType.isArray()) {
         Object fieldObj = Array.newInstance(componentType, len);
         for (int i=0; i<len; ++i) {
            Array.set(fieldObj, i, array(componentType));
         }
         return fieldObj;
      } else {
         throw null;
      }
   }
   private static void matchType(String expected, String actual) throws IOException {
      if (!expected.equals(actual)) {
         throw new IOException("Type mismatch");
      }
   }
}
