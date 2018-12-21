package tackline.play.serialize;

import java.awt.Point;
import java.io.*;

public class TestDrive {
   public static class WithPrimitiveArray {
      private int[][] array;
      public WithPrimitiveArray() { // for serial
      }
      /* pp */ WithPrimitiveArray(int[][] array) { // for us
         this.array = array;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof WithPrimitiveArray &&
            java.util.Objects.deepEquals(((WithPrimitiveArray)other).array, array);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return java.util.Arrays.toString(array);
      }
   }
   public static class WithArray {
      private Point[][] array;
      public WithArray() { // for serial
      }
      /* pp */ WithArray(Point[][] array) { // for us
         this.array = array;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof WithArray &&
            java.util.Objects.deepEquals(((WithArray)other).array, array);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return java.util.Arrays.toString(array);
      }
   }
   
   public static class WithField {
      private Point field;
      public WithField() { // for serial
      }
      /* pp */ WithField(Point field) { // for us
         this.field = field;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof WithField &&
            java.util.Objects.deepEquals(((WithField)other).field, field);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return java.util.Objects.toString(field);
      }
   }
   
   public static void main(String[] args) {
      check(new Point(1, 2));
      check(new WithPrimitiveArray(new int[][] {{ 1, 2 }, { 3, 4 }}));
      check(new WithArray(new Point[][] {{ new Point(1, 2) }, { new Point(3, 4) }}));
      check(new WithField(new Point(1, 2)));
      check(new WithField[] { new WithField(new Point(1, 2)) });
      check(new WithField[] { });
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
      if (!java.util.Objects.deepEquals(obj, copy)) {
         throw new AssertionError("expected <"+obj+"> was <"+copy+">");
      }
   }
}
