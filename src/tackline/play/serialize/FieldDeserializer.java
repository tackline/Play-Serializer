package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

public final class FieldDeserializer {
   private FieldDeserializer() {
   }
   // !! A bit long.
   public static <T> T deserialize(DataInput in, Class<T> clazz) throws IOException {
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
         try {
            if (type == boolean.class) {
               matchType("Z", sig);
               field.setBoolean(obj, in.readBoolean());
            } else if (type == byte.class) {
               matchType("B", sig);
               field.setByte(obj, in.readByte());
            } else if (type == char.class) {
               matchType("C", sig);
               field.setChar(obj, in.readChar());
            } else if (type == short.class) {
               matchType("S", sig);
               field.setShort(obj, in.readShort());
            } else if (type == int.class) {
               matchType("I", sig);
               field.setInt(obj, in.readInt());
            } else if (type == long.class) {
               matchType("J", sig);
               field.setLong(obj, in.readLong());
            } else if (type == float.class) {
               matchType("F", sig);
               field.setFloat(obj, in.readFloat());
            } else if (type == double.class) {
               matchType("D", sig);
               field.setDouble(obj, in.readDouble());
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
   private static void matchType(String expected, String actual) throws IOException {
      if (!expected.equals(actual)) {
         throw new IOException("Type mismatch");
      }
   }
}
