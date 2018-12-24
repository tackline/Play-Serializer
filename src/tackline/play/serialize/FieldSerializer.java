package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

public final class FieldSerializer {
   private static class Ref {
      private final Type type;
      private final long id;
      public Ref(Type type, long id) {
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
      new FieldSerializer(out).serialize(clazz, obj);
   }
   private void serialize(Type type, Object obj) throws IOException {
      Ref backRef = backRefs.get(obj);
      if (backRef != null) {
         out.writeUTF("%");
         backRef(backRef, type, obj);
      } else if (obj == null) {
         out.writeUTF("!");
      } else {
         out.writeUTF("=");
         if (!seen.add(obj)) {
            throw exc("Cycle detected");
         }
         refType(type, obj);
         labelForBackRef(type, obj);
      }
   }
   private void refType(Type type, Object obj) throws IOException {
      if (type instanceof Class<?>) {
         classType((Class<?>)type, new Type[0], obj);
      } else if (type instanceof ParameterizedType) {
         ParameterizedType parameterizedType = (ParameterizedType)type;
         Type rawType = parameterizedType.getRawType();
         if (rawType instanceof Class<?>) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            classType((Class<?>)rawType, typeArgs, obj);
         } else {
            throw exc("Don't know what that raw type is supposed to be");
         }
      } else if (type instanceof GenericArrayType) { // !! T in List<T>[]?
         array(type, obj);
      } else {
         throw exc("Type <"+type.getClass()+"> of Type not supported, <"+type+">");
      }
   }
   private void classType(Class<?> clazz, Type[] typeArgs, Object obj) throws IOException {
      if (clazz.isArray()) {
         array(clazz, obj);
      } else {
         object(clazz, typeArgs, obj);
      }
   }
   private void backRef(Ref backRef, Type type, Object obj) throws IOException {
      if (!backRef.type.equals(type)) {
         throw exc("static type of object changed");
      }
      out.writeLong(backRef.id);
   }
   private void labelForBackRef(Type type, Object obj) throws IOException {
      long id = nextId++;
      backRefs.put(obj, new Ref(type, id));
      out.writeLong(id);
   }
   private void object(Class<?> clazz, Type[] typeArgs, Object obj) throws IOException {
      // !! Of course there's no real semantics defined for equals on TypeVariable
      TypeParamMap typeMap = new TypeParamMap(clazz.getTypeParameters(), typeArgs);
      @SuppressWarnings("unused")
      Constructor<?> ctor = FieldCommon.nullaryConstructor(clazz);
      // !! We don't do class hierarchies.
      for (Field field : FieldCommon.serialFields(clazz)) {
         out.writeUTF(field.getName());
         Type type = field.getGenericType();
         try {
            if (type instanceof Class<?> && ((Class<?>)type).isPrimitive()) {
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
               } else {
                  throw new Error("Unknown primitive type");
               }
            } else {
               Object fieldObj = field.get(obj);
               serialize(typeMap.substitute(type), fieldObj);
            }
         } catch (IllegalAccessException exc) {
            // This can't happen.
            // !! We don't like this aspect of the reflection API.
            throw new Error(exc);
         }
      }
      out.writeUTF("."); // End of class indicator.
   }
   private void array(Type type, Object fieldObj) throws IOException {
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
         Type componentType;
         if (type instanceof Class<?>) {
            componentType = ((Class<?>)type).getComponentType();
         } else if (type instanceof GenericArrayType) {
            componentType = ((GenericArrayType)type).getGenericComponentType();
         } else {
            throw exc("Unknown array type type");
         }
         for (Object c : (Object[])fieldObj) {
            serialize(componentType, c);
         }
      }
   }
   // Actually throws the exception instead of returning it, just in case we forget.
   private static IllegalArgumentException exc(String msg) {
      throw new IllegalArgumentException(msg);
   }
}
