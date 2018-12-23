package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

public final class FieldSerializer {
   private static class Ref {
      private final Class<?> type;
      private final long id;
      public Ref(Class<?> type, long id) {
         this.type = type;
         this.id = id;
      }
   }
   private final DataOutput out;
   private final Map<Object,Ref> backRefs = new IdentityHashMap<>();
   private final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
   private long nextId = 1;
   private FieldSerializer(DataOutput out) {
      this.out = out;
   }
   public static <T> void serialize(DataOutput out, Class<T> clazz, T obj) throws IOException {
      new FieldSerializer(out).serialize(obj.getClass(), obj);
   }
   private void serialize(Type type, Object obj) throws IOException {
      if (type instanceof Class<?>) {
         refType((Class<?>)type, new Type[0], obj);
      } else if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)type;
         Type rawType = parameterizedType.getRawType();
         if (rawType instanceof Class<?>) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            refType((Class<?>)rawType, typeArgs, obj);
         } else {
            throw exc("Don't know what that raw type is supposed to be");
         }
      } else {
         throw exc("Type <"+type.getClass()+"> of Type not supported, <"+type+">");
      }
   }
   private void refType(Class<?> clazz, Type[] typeArgs, Object obj) throws IOException {
      Ref backRef = backRefs.get(obj);
      if (backRef != null) {
         out.writeUTF("%");
         backRef(backRef, clazz, obj);
      } else if (obj == null) {
         out.writeUTF("!");
         out.writeUTF(clazz.getName());
      } else {
         if (!seen.add(obj)) {
            throw exc("Cycle detected");
         }
         
         out.writeUTF(clazz.getName());
         if (clazz.isArray()) {
            array(clazz, obj);
         } else {
            object(clazz, typeArgs, obj);
         }
         labelForBackRef(clazz, obj);
      }
   }
   private void backRef(Ref backRef, Class<?> clazz, Object obj) throws IOException {
      if (backRef.type != clazz) {
         throw exc("static type of object changed");
      }
      out.writeLong(backRef.id);
   }
   private void labelForBackRef(Class<?> clazz, Object obj) throws IOException {
      long id = nextId++;
      backRefs.put(obj, new Ref(clazz, id));
      out.writeLong(id);
   }
   private void object(Class<?> clazz, Type[] typeArgs, Object obj) throws IOException {
      TypeVariable<?>[] typeParams = clazz.getTypeParameters();
      if (typeParams.length != typeArgs.length) {
         throw exc("Class with type params not matching type args");
      }
      @SuppressWarnings("unused")
      Constructor<?> ctor = FieldCommon.nullaryConstructor(clazz);
      // !! We don't do class hierarchies.
      for (Field field : FieldCommon.serialFields(clazz)) {
         out.writeUTF(field.getName());
         Type type = field.getGenericType();
         try {
            if (type instanceof Class<?> && ((Class<?>)type).isPrimitive()) {
               out.writeUTF(((Class<?>)type).getName());
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
               //} else if (type.isArray()) {
               //   array(type, field.get(obj));
               } else {
                  throw new Error("Unknown primitive type");
               }
            } else {
               if (type instanceof TypeVariable<?>) {
                  typeParam: {
                     for (int i=0; i<typeParams.length; ++i) {
                        if (type.equals(typeParams[i])) {
                           serialize(typeArgs[i], field.get(obj));
                           break typeParam;
                        }
                     }
                     throw exc("Field's type variable not found");
                  }
               } else {
                  serialize(type, field.get(obj));
               }
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
   // Actually throws the exception instead of returning it, just in case we forget.
   public static IllegalArgumentException exc(String msg) {
      throw new IllegalArgumentException(msg);
   }
}
