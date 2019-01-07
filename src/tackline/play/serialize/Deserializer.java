package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

public class Deserializer {
   private static class Ref {
      private final KnownType type;
      private final Object obj;
      public Ref(KnownType type, Object obj) {
         this.type = type;
         this.obj = obj;
      }
   }
   private final Map<Long,Ref> backRefs = new HashMap<>();
   
   private final Reflecter reflecter;
   private final DataInput in;
   /* pp */ Deserializer(Reflecter reflecter, DataInput in) {
      this.reflecter = reflecter;
      this.in = in;
   }
   public static <T> T fieldDeserialize(DataInput in, Class<T> clazz) throws IOException {
      return clazz.cast(new Deserializer(new FieldReflecter(), in).deserialize(KnownType.clazz(clazz)));
   }
   public static <T> T valueDeserialize(DataInput in, Class<T> clazz) throws IOException {
      return clazz.cast(new Deserializer(new FieldReflecter(), in).deserialize(KnownType.clazz(clazz)));
   }
   /* pp */ Object deserialize(KnownType type) throws IOException {
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
   private Object refType(KnownType type) throws IOException {
      return type.extractParameters(new ParameterExtract<Object,IOException>() {
         public Object class_(Class<?> rawType, KnownType[] args) throws IOException {
            return object(type, rawType, args);
         }
         public Object array(KnownType componentType) throws IOException {
            return Deserializer.this.array(componentType);
         }
      });
   }
   private Object backRef(KnownType type) throws IOException {
      long id = in.readLong();
      Ref ref = backRefs.get(id);
      if (ref == null) {
         throw new IOException("Read an non-existent back ref");
      }
      if (!ref.type.equals(type)) {
         throw new IOException("Back ref is of the wrong type");
      }
      return ref.obj;
   }
   private void labelForBackRef(
      KnownType type, Object obj
   ) throws IOException {
      backRefs.put(in.readLong(), new Ref(type, obj));
   }
   private Object object(
      KnownType type, Class<?> clazz, KnownType[] typeArgs
   ) throws IOException {
      TypeParamMap typeMap = new TypeParamMap(clazz, typeArgs);
      ObjectFormat format = FieldCommon.format(clazz);
      
      // !! We don't do class hierarchies.

      Map<String,Object> data = new HashMap<>();
      for (;;) {
         String name = in.readUTF();
         if (name.equals(".")) {
            break;
         }
         int index = format.names().indexOf(name);
         if (index == -1) {
            // Java Serialization just ignores this.
            //   (Also the XML way.)
            throw new IOException(
               "field <"+name+"> in stream not in class"
            );
         }
         DataFormat dataFormat = format.dataFormats().get(index);
         Type fieldType = format.types().get(index);
         switch (dataFormat) {
            case BOOLEAN: data.put(name, in.readBoolean()); break;
            case BYTE   : data.put(name, in.readByte()); break;
            case CHAR   : data.put(name, in.readChar()); break;
            case SHORT  : data.put(name, in.readShort()); break;
            case INT    : data.put(name, in.readInt()); break;
            case LONG   : data.put(name, in.readLong()); break;
            case FLOAT  : data.put(name, in.readFloat()); break;
            case DOUBLE : data.put(name, in.readDouble()); break;
            case REF    : data.put(name,
               deserialize(typeMap.substitute(fieldType))
            ); break;
            default: throw new Error("???");
         }
      }
      
      return reflecter.newObject(type, clazz, typeMap).implode(data);
   }
   private Object array(KnownType component) throws IOException {
      int len = in.readInt();
      if (component.equals(KnownType.clazz(boolean.class))) {
         boolean[] fieldObj = new boolean[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readBoolean();
         }
         return fieldObj;
      } else if (component.equals(KnownType.clazz(byte.class))) {
         byte[] fieldObj = new byte[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readByte();
         }
         return fieldObj;
      } else if (component.equals(KnownType.clazz(char.class))) {
         char[] fieldObj = new char[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readChar();
         }
         return fieldObj;
      } else if (component.equals(KnownType.clazz(short.class))) {
         short[] fieldObj = new short[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readShort();
         }
         return fieldObj;
      } else if (component.equals(KnownType.clazz(int.class))) {
         int[] fieldObj = new int[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readInt();
         }
         return fieldObj;
      } else if (component.equals(KnownType.clazz(long.class))) {
         long[] fieldObj = new long[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readLong();
         }
         return fieldObj;
      } else if (component.equals(KnownType.clazz(float.class))) {
         float[] fieldObj = new float[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readFloat();
         }
         return fieldObj;
      } else if (component.equals(KnownType.clazz(double.class))) {
         double[] fieldObj = new double[len];
         for (int i=0; i<len; ++i) {
            fieldObj[i] = in.readDouble();
         }
         return fieldObj;
      } else {
         Object fieldObj = component.newArray(len);
         for (int i=0; i<len; ++i) {
            Array.set(fieldObj, i, deserialize(component));
         }
         return fieldObj;
      }
   }
   // Actually throws the exception instead of returning it, just in case we forget.
   public static IOException exc(String msg) throws IOException {
      throw new IOException(msg);
   }
}
