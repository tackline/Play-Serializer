package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

public class FieldSerializer {
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
   /* pp */ FieldSerializer(DataOutput out) {
      this.out = out;
   }
   public static <T> void serialize(DataOutput out, Class<T> clazz, T obj) throws IOException {
      new FieldSerializer(out).serialize(clazz, obj);
   }
   /* pp */ void serialize(Type type, Object obj) throws IOException {
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
      FieldCommon.extractParameters(type, new ParameterExtract<Void,IOException>() {
         public Void class_(Class<?> rawType, Type[] typeArgs) throws IOException {
            object(rawType, typeArgs, obj);
            return null;
         }
         public Void array(Type componentType) throws IOException {
            FieldSerializer.this.array(componentType, obj);
            return null;
         }
      });
   }
   private void backRef(
      Ref backRef, Type type, Object obj
   ) throws IOException {
      if (!backRef.type.equals(type)) {
         throw exc("static type of object changed");
      }
      out.writeLong(backRef.id);
   }
   private void labelForBackRef(
      Type type, Object obj
   ) throws IOException {
      long id = nextId++;
      backRefs.put(obj, new Ref(type, id));
      out.writeLong(id);
   }
   /* pp */ Exploder oldObject(Class<?> clazz) throws IOException {
      List<Field> fields = FieldCommon.serialFields(clazz);
      List<String> names = new ArrayList<>();
      for (Field field : fields) {
         names.add(field.getName());
      }
      List<String> names_ = Collections.unmodifiableList(names); // sigh sigh
      return new Exploder() {
         public List<String> names() {
            return names_;
         }
         public List<Object> explode(Object obj) {
            List<Object> data = new ArrayList<>();
            for (Field field : fields) {
               try {
                  data.add(field.get(obj));
               } catch (IllegalAccessException exc) {
                  // This can't happen.
                  // !! We don't like this aspect of the reflection API.
                  throw new Error(exc);
               }
            }
            return data;
         }
      };
   }
   private void object(
      Class<?> clazz, Type[] typeArgs, Object obj
   ) throws IOException {
      TypeParamMap typeMap = new TypeParamMap(clazz, typeArgs);
      ObjectFormat format = FieldCommon.format(clazz);
      Exploder exploder = oldObject(clazz);
      List<String> names = exploder.names();
      List<Object> data = exploder.explode(obj);
      int num = names.size();
      for (int i=0; i<num; ++i) {
         String name = names.get(i);
         out.writeUTF(name);
         int index = format.names().indexOf(name);
         if (index == -1) {
            // Java Serialization just ignores this.
            //   (Also the XML way.)
            throw new IOException(
               "field <"+name+"> in stream not in class"
            );
         }
         DataFormat dataFormat = format.dataFormats().get(index);
         Type type = format.types().get(index);
         Object value = data.get(i);
//         Type type = field.getGenericType();
         switch (dataFormat) {
            case BOOLEAN: out.writeBoolean((Boolean)  value); break;
            case BYTE   : out.writeByte(   (Byte)     value); break;
            case CHAR   : out.writeChar(   (Character)value); break;
            case SHORT  : out.writeShort(  (Short)    value); break;
            case INT    : out.writeInt(    (Integer)  value); break;
            case LONG   : out.writeLong(   (Long)     value); break;
            case FLOAT  : out.writeFloat(  (Float)    value); break;
            case DOUBLE : out.writeDouble( (Double)   value); break;
            case REF    :
               serialize(typeMap.substitute(type), value);
            break;
            default: throw new Error("???");
         }
      }
      out.writeUTF("."); // End of class indicator.
   }
   private void array(
      Type componentType, Object fieldObj
   ) throws IOException {
      int len = Array.getLength(fieldObj);
      out.writeInt(len);
      if (componentType == boolean.class) {
         for (boolean c : (boolean[])fieldObj) {
            out.writeBoolean(c);
         }
      } else if (componentType == byte.class) {
         for (byte c : (byte[])fieldObj) {
            out.writeByte(c);
         }
      } else if (componentType == char.class) {
         for (char c : (char[])fieldObj) {
            out.writeChar(c);
         }
      } else if (componentType == short.class) {
         for (short c : (short[])fieldObj) {
            out.writeShort(c);
         }
      } else if (componentType == int.class) {
         for (int c : (int[])fieldObj) {
            out.writeInt(c);
         }
      } else if (componentType == long.class) {
         for (long c : (long[])fieldObj) {
            out.writeLong(c);
         }
      } else if (componentType == float.class) {
         for (float c : (float[])fieldObj) {
            out.writeFloat(c);
         }
      } else if (componentType == double.class) {
         for (double c : (double[])fieldObj) {
            out.writeDouble(c);
         }
      } else {
         // !! Should probably check raw component type here for fast fail.
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
