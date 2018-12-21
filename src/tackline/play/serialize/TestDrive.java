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
      
      Object[] cycle = new Object[1];
      cycle[0] = cycle;
      try {
         check(cycle);
         fail("Cycle undetected");
      } catch (IllegalArgumentException exc) {
         // good
      }
      
      Point[] repeat = { new Point(1, 2), null };
      repeat[1] = repeat[0];
      Point[] repeatCopy = copy(repeat);
      asrt(repeatCopy[0] == repeatCopy[1], "Repeated instance not the same instance");
      
      check(new WithField(null));
      check(new WithArray(null));
   }
   private static void check(Object obj){
      Object copy = copy(obj);
      asrt(
         java.util.Objects.deepEquals(obj, copy),
         "expected <"+obj+"> was <"+copy+">"
      );
   }
   private static <T> T copy(T obj){
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      try {
         FieldSerializer.serialize(new DataOutputStream(byteOut), obj);
         return (T)FieldDeserializer.deserialize(
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
   }
   private static void asrt(boolean that, String msg) {
      if (!that) {
         fail(msg);
      }
   }
   private static void fail(String msg) {
      throw new AssertionError(msg);
   }
}
