package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

public final class FieldSerializer {
   private FieldSerializer() {
   }
   // !! A bit long.
   public static void serialize(DataOutput out, Object obj) throws IOException {
      Class<?> clazz = obj.getClass();
      @SuppressWarnings("unused")
      Constructor<?> ctor = FieldCommon.nullaryConstructor(clazz);
      // !! We don't do class hierarchies.
      for (Field field : FieldCommon.serialFields(clazz)) {
         out.writeUTF(field.getName());
         Class<?> type = field.getType();
         try {
            if (type == boolean.class) {
               out.writeUTF("Z");
               out.writeBoolean(field.getBoolean(obj));
            } else if (type == byte.class) {
               out.writeUTF("B");
               out.writeByte(field.getByte(obj));
            } else if (type == char.class) {
               out.writeUTF("C");
               out.writeChar(field.getChar(obj));
            } else if (type == short.class) {
               out.writeUTF("S");
               out.writeShort(field.getShort(obj));
            } else if (type == int.class) {
               out.writeUTF("I");
               out.writeInt(field.getInt(obj));
            } else if (type == long.class) {
               out.writeUTF("J");
               out.writeLong(field.getLong(obj));
            } else if (type == float.class) {
               out.writeUTF("F");
               out.writeFloat(field.getFloat(obj));
            } else if (type == double.class) {
               out.writeUTF("D");
               out.writeDouble(field.getDouble(obj));
            } else {
               throw new IllegalArgumentException("!! We don't do reference types");
            }
         } catch (IllegalAccessException exc) {
            // This can't happen.
            // !! We don't like this aspect of the reflection API.
            throw new Error(exc);
         }
      }
      out.writeUTF("."); // End of class indicator.
   }
}
