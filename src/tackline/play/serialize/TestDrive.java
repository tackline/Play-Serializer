package tackline.play.serialize;

import java.awt.Point;
import java.io.*;

// Smoke test. We don't claim this is thorough testing. 
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
   public static class Var<T> {
      private T value;
      public Var() {
      }
      public Var(T value) {
         this.value = value;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof Var &&
            java.util.Objects.deepEquals(((Var<?>)other).value, value);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return "Var<"+java.util.Objects.toString(value)+">";
      }
   }
   public static class VarArray<T> {
      private T[] value;
      public VarArray() {
      }
      public VarArray(T[] value) {
         this.value = value;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof VarArray &&
            java.util.Arrays.deepEquals(((VarArray<?>)other).value, value);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return "VarArray<"+java.util.Objects.toString(value)+">";
      }
   }
   public static class WithVar {
      private Var<Point> valuePoint;
      public WithVar() {
      }
      public WithVar(Var<Point> valuePoint) {
         this.valuePoint = valuePoint;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof WithVar &&
            java.util.Objects.deepEquals(((WithVar)other).valuePoint, valuePoint);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return "WithVar<"+java.util.Objects.toString(valuePoint)+">";
      }
   }
   public static class WithVarVar {
      private Var<Var<Point>> valuePoint;
      public WithVarVar() {
      }
      public WithVarVar(Var<Var<Point>> valuePoint) {
         this.valuePoint = valuePoint;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof WithVarVar &&
            java.util.Objects.deepEquals(((WithVarVar)other).valuePoint, valuePoint);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return "WithVarVar<"+java.util.Objects.toString(valuePoint)+">";
      }
   }
   public static class WithVarArray {
      private VarArray<Point> valuePoint;
      public WithVarArray() {
      }
      public WithVarArray(VarArray<Point> valuePoint) {
         this.valuePoint = valuePoint;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof WithVarArray &&
            java.util.Objects.deepEquals(((WithVarArray)other).valuePoint, valuePoint);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return "WithVarArray<"+java.util.Objects.toString(valuePoint)+">";
      }
   }
   public static void main(String[] args) {
      check(Point.class, new Point(1, 2));
      check(WithPrimitiveArray.class, new WithPrimitiveArray(new int[][] {{ 1, 2 }, { 3, 4 }}));
      check(WithArray.class, new WithArray(new Point[][] {{ new Point(1, 2) }, { new Point(3, 4) }}));
      check(WithField.class, new WithField(new Point(1, 2)));
      check(WithField[].class, new WithField[] { new WithField(new Point(1, 2)) });
      check(WithField[].class, new WithField[] { });
      
      // Cyclic structures
      Object[] cycle = new Object[1];
      cycle[0] = cycle;
      try {
         check(Object[].class, cycle);
         fail("Cycle undetected");
      } catch (IllegalArgumentException exc) {
         // good
      }
      
      // Back reference
      Point[] repeat = { new Point(1, 2), null };
      repeat[1] = repeat[0];
      Point[] repeatCopy = copy(Point[].class, repeat);
      asrt(repeatCopy[0] == repeatCopy[1], "Repeated instance not the same instance");
      
      // nulls
      check(WithField.class, new WithField(null));
      check(WithArray.class, new WithArray(new Point[][] { null }));

      // Generics
      check(WithVar.class, new WithVar(new Var<>(new Point(12, 13))));
      check(WithVarVar.class, new WithVarVar(new Var<>(new Var<>(new Point(14, 15)))));
      check(WithVarArray.class, new WithVarArray(new VarArray<>(new Point[] { new Point(15, 16) })));
      
   }
   private static <T> void check(Class<T> clazz, T obj){
      Object copy = copy(clazz, obj);
      asrt(
         java.util.Objects.deepEquals(obj, copy),
         "expected <"+obj+"> was <"+copy+">"
      );
   }
   private static <T> T copy(Class<T> clazz, T obj){
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      try {
         FieldSerializer.serialize(new DataOutputStream(byteOut), clazz, obj);
         return FieldDeserializer.deserialize(
            new DataInputStream(
               new ByteArrayInputStream(
                  byteOut.toByteArray()
               )
            ),
            obj.getClass().asSubclass(clazz)
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
