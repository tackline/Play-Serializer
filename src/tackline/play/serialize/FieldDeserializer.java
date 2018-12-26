package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;
import java.util.stream.*;

public class FieldDeserializer {
   private static class Ref {
      private final Type type;
      private final Object obj;
      public Ref(Type type, Object obj) {
         this.type = type;
         this.obj = obj;
      }
   }
   private final Map<Long,Ref> backRefs = new HashMap<>();
   /* pp */ final DataInput in;
   /* pp */ FieldDeserializer(DataInput in) {
      this.in = in;
   }
   public static <T> T deserialize(DataInput in, Class<T> clazz) throws IOException {
      return clazz.cast(new FieldDeserializer(in).deserialize(clazz));
   }
   /* pp */ Object deserialize(Type type) throws IOException {
      String clazzName = in.readUTF(); //!!name
      if (clazzName.equals("%")) {
         // !! Doesn't check type.
         return backRef(type);
      } else if (clazzName.equals("!")) {
         return null;
      } else if (clazzName.equals("=")) {
         Object obj = refType(type);
         labelForBackRef(type, obj);
         return obj;
      } else {
         throw exc("Unkown serialization type");
      }
   }
   private Object refType(Type type) throws IOException {
      return FieldCommon.extractParameters(type, new ParameterExtract<Object,IOException>() {
         public Object class_(Class<?> rawType, Type[] typeArgs) throws IOException {
            return object(rawType, typeArgs);
         }
         public Object array(Type componentType) throws IOException {
            return FieldDeserializer.this.array(componentType);
         }
      });
   }
   private Object backRef(Type type) throws IOException {
      long id = in.readLong();
      Ref ref = backRefs.get(id);
      if (ref == null) {
         throw new IOException("Read an non-existent back ref");
      }
      // !! This isn't going to work for type substitution, is it?
      if (ref.type != type) {
         throw new IOException("Back ref is of the wrong type");
      }
      return ref.obj;
   }
   private void labelForBackRef(Type type, Object obj) throws IOException {
      backRefs.put(in.readLong(), new Ref(type, obj));
   }
   /* pp */ <T> T object(Class<T> clazz, Type[] typeArgs) throws IOException {
      TypeParamMap typeMap = new TypeParamMap(clazz, typeArgs);
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
         throw FieldCommon.throwUnchecked(exc);
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
         Type type = field.getGenericType();
         try {
            if (type instanceof Class<?> && ((Class<?>)type).isPrimitive()) {
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
               } else {
                  throw new Error("Unknown primitive type");
               }
            } else {
               field.set(obj, deserialize(typeMap.substitute(type)));
            }
         } catch (IllegalAccessException exc) {
            // This can't happen.
            // !! We don't like this aspect of the reflection API.
            throw new Error(exc);
         }
      }
      return obj;
   }
   private Object array(Type componentType) throws IOException {
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
      } else {
         final Class<?> rawComponentType;
         if (componentType instanceof Class<?>) {
            rawComponentType = (Class<?>)componentType;
         } else if (componentType instanceof ParameterizedType) {
            Type badlyTypedRawComponentType = ((ParameterizedType)componentType).getRawType();
            if (badlyTypedRawComponentType instanceof Class<?>) {
                rawComponentType = (Class<?>)badlyTypedRawComponentType;
            } else {
               throw exc("WTF: ParameterizedType.getRawType not a Class<?>");
            }
         } else {
            throw exc("Array component type <"+componentType.getClass()+"> not a Class<?>");
         }
         Object fieldObj = Array.newInstance(rawComponentType, len);
         for (int i=0; i<len; ++i) {
            Array.set(fieldObj, i, deserialize(componentType));
         }
         return fieldObj;
      }
   }
   // Actually throws the exception instead of returning it, just in case we forget.
   public static IOException exc(String msg) throws IOException {
      throw new IOException(msg);
   }
}
