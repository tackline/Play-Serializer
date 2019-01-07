package tackline.play.serialize;

import java.lang.reflect.*;
import java.io.*;
import java.util.*;

public class Serializer {
   private static class Ref {
      private final KnownType type;
      private final long id;
      public Ref(KnownType type, long id) {
         this.type = type;
         this.id = id;
      }
   }
   private final Reflecter reflecter;
   private final DataOutput out;
   private final Map<Object,Ref> backRefs = new IdentityHashMap<>();
   private final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
   private long nextId = 1;
   /* pp */ Serializer(Reflecter reflecter, DataOutput out) {
      this.reflecter = reflecter;
      this.out = out;
   }
   public static <T> void fieldSerialize(DataOutput out, Class<T> clazz, T obj) throws IOException {
      new Serializer(new FieldReflecter(), out).serialize(KnownType.clazz(clazz), obj);
   }
   public static <T> void valueSerialize(DataOutput out, Class<T> clazz, T obj) throws IOException {
      new Serializer(new ValueReflecter(), out).serialize(KnownType.clazz(clazz), obj);
   }
   /* pp */ void serialize(KnownType type, Object obj) throws IOException {
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
   private void refType(KnownType type, Object obj) throws IOException {
      type.extractParameters(new ParameterExtract<Void,IOException>() {
         public Void class_(Class<?> rawType, KnownType[] args) throws IOException {
            object(rawType, args, obj);
            return null;
         }
         public Void array(KnownType component) throws IOException {
            Serializer.this.array(component, obj);
            return null;
         }
      });
   }
   private void backRef(
      Ref backRef, KnownType type, Object obj
   ) throws IOException {
      if (!backRef.type.equals(type)) {
         throw exc("static type of object changed");
      }
      out.writeLong(backRef.id);
   }
   private void labelForBackRef(
      KnownType type, Object obj
   ) throws IOException {
      long id = nextId++;
      backRefs.put(obj, new Ref(type, id));
      out.writeLong(id);
   }
   private void object(
      Class<?> clazz, KnownType[] typeArgs, Object obj
   ) throws IOException {
      TypeParamMap typeMap = new TypeParamMap(clazz, typeArgs);
      ObjectFormat format = FieldCommon.format(clazz);
      Exploder exploder = reflecter.oldObject(clazz);
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
      KnownType component, Object fieldObj
   ) throws IOException {
      int len = Array.getLength(fieldObj);
      out.writeInt(len);
      if (component.equals(KnownType.clazz(boolean.class))) {
         for (boolean c : (boolean[])fieldObj) {
            out.writeBoolean(c);
         }
      } else if (component.equals(KnownType.clazz(byte.class))) {
         for (byte c : (byte[])fieldObj) {
            out.writeByte(c);
         }
      } else if (component.equals(KnownType.clazz(char.class))) {
         for (char c : (char[])fieldObj) {
            out.writeChar(c);
         }
      } else if (component.equals(KnownType.clazz(short.class))) {
         for (short c : (short[])fieldObj) {
            out.writeShort(c);
         }
      } else if (component.equals(KnownType.clazz(int.class))) {
         for (int c : (int[])fieldObj) {
            out.writeInt(c);
         }
      } else if (component.equals(KnownType.clazz(long.class))) {
         for (long c : (long[])fieldObj) {
            out.writeLong(c);
         }
      } else if (component.equals(KnownType.clazz(float.class))) {
         for (float c : (float[])fieldObj) {
            out.writeFloat(c);
         }
      } else if (component.equals(KnownType.clazz(double.class))) {
         for (double c : (double[])fieldObj) {
            out.writeDouble(c);
         }
      } else {
         // !! Should probably check raw component type here for fast fail.
         for (Object c : (Object[])fieldObj) {
            serialize(component, c);
         }
      }
   }
   // Actually throws the exception instead of returning it, just in case we forget.
   private static IllegalArgumentException exc(String msg) {
      throw new IllegalArgumentException(msg);
   }
}
