package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;

public final class FieldSerializer {
   private final DataOutput out;
   private FieldSerializer(DataOutput out) {
      this.out = out;
   }
   public static void serialize(DataOutput out, Object obj) throws IOException {
      new FieldSerializer(out).serialize(obj.getClass(), obj);
   }
   // !! A bit long.
   private void serialize(Class<?> clazz, Object obj) throws IOException {
      if (clazz.isArray()) {
         array(clazz, obj);
      } else {
         object(clazz, obj);
      }
   }
   private void object(Class<?> clazz, Object obj) throws IOException {
      @SuppressWarnings("unused")
      Constructor<?> ctor = FieldCommon.nullaryConstructor(clazz);
      // !! We don't do class hierarchies.
      for (Field field : FieldCommon.serialFields(clazz)) {
         out.writeUTF(field.getName());
         Class<?> type = field.getType();
         out.writeUTF(type.getName());
         try {
            if (type == boolean.class) {
               out.writeBoolean(field.getBoolean(obj));
            } else if (type == byte.class) {
               out.writeByte(field.getByte(obj));
            } else if (type == char.class) {
               out.writeChar(field.getChar(obj));
            } else if (type == short.class) {
               out.writeShort(field.getShort(obj));
            } else if (type == int.class) {
               out.writeInt(field.getInt(obj));
            } else if (type == long.class) {
               out.writeLong(field.getLong(obj));
            } else if (type == float.class) {
               out.writeFloat(field.getFloat(obj));
            } else if (type == double.class) {
               out.writeDouble(field.getDouble(obj));
            } else if (type.isArray()) {
               array(type, field.get(obj));
            } else {
               serialize(type, field.get(obj));
            }
         } catch (IllegalAccessException exc) {
            // This can't happen.
            // !! We don't like this aspect of the reflection API.
            throw new Error(exc);
         }
      }
      out.writeUTF("."); // End of class indicator.
   }
   private void array(Class<?> type, Object fieldObj) throws IOException {
      int len = Array.getLength(fieldObj);
      out.writeInt(len);
      if (type == boolean[].class) {
         for (boolean c : (boolean[])fieldObj) {
            out.writeBoolean(c);
         }
      } else if (type == byte[].class) {
         for (byte c : (byte[])fieldObj) {
            out.writeByte(c);
         }
      } else if (type == char[].class) {
         for (char c : (char[])fieldObj) {
            out.writeChar(c);
         }
      } else if (type == short[].class) {
         for (short c : (short[])fieldObj) {
            out.writeShort(c);
         }
      } else if (type == int[].class) {
         for (int c : (int[])fieldObj) {
            out.writeInt(c);
         }
      } else if (type == long[].class) {
         for (long c : (long[])fieldObj) {
            out.writeLong(c);
         }
      } else if (type == float[].class) {
         for (float c : (float[])fieldObj) {
            out.writeFloat(c);
         }
      } else if (type == double[].class) {
         for (double c : (double[])fieldObj) {
            out.writeDouble(c);
         }
      } else {
         for (Object c : (Object[])fieldObj) {
            serialize(type.getComponentType(), c);
         }
      }
   }
}
