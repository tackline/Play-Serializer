package tackline.play.serialize;

import java.io.*;

public class TestDrive {

   public static void main(String[] args) {
      check(new java.awt.Point(1, 2));
   }
   private static void check(Object obj){
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      Object copy;
      try {
         FieldSerializer.serialize(new DataOutputStream(byteOut), obj);
         copy = FieldDeserializer.deserialize(
            new DataInputStream(
               new ByteArrayInputStream(
                  byteOut.toByteArray()
               )
            ),
            obj.getClass()
         );
      } catch (IOException exc) {
         throw new Error(exc);
      }
      if (!obj.equals(copy)) {
         throw new AssertionError("expected <"+obj+"> was <"+copy+">");
      }
   }
}
