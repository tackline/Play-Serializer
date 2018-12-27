package tackline.play.serialize;

import java.io.*;
import java.lang.reflect.*;

// Smoke test. We don't claim this is thorough testing. 
public class TestDrive {
   public static class Point {
      int x;
      int y;
      public int x() {
         return x;
      }
      public int y() {
         return y;
      }
      public static Point of(int x, int y) {
         return new Point(x, y);
      }
      public Point() {
      }
      public Point(int x, int y) {
         this.x = x;
         this.y = y;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof Point &&
            ((Point)other).x == x &&
            ((Point)other).y == y;
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return "Point("+x+", "+y+")";
      }
   }
      
   public static class WithPrimitiveArray {
      private int[][] array;
      public int[][] array() {
        return arrayClone(array);
      }
      public static WithPrimitiveArray of(int[][] array) {
         return new WithPrimitiveArray(array);
      }
      public WithPrimitiveArray() { // for serial
      }
      /* pp */ WithPrimitiveArray(int[][] array) { // for us
         this.array = arrayClone(array);
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
      public Point[][] array() {
         return arrayClone(array); // !! Should clone Point to, but Point sucks...
      }
      public static WithArray of(Point[][] array) {
         return new WithArray(arrayClone(array));
      }
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
      public Point field() {
         return field;
      }
      public static WithField of(Point field) {
         return new WithField(field);
      }
      public WithField() { // for serial
      }
      /* pp */ WithField(Point field) { // for us
         this.field = field==null ? null : Point.of(field.x, field.y);
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
      public T value() {
         return value; // !! How do we make this safe?
      }
      public static <T> Var<T> of(T value) {
         return new Var<T>(value);
      }
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
      public T[] value() {
         return arrayClone(value);
      }
      public static <T> VarArray<T> of(T[] value) {
         return new VarArray<T>(value);
      }
      public VarArray() {
      }
      public VarArray(T[] value) {
         this.value = arrayClone(value);
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
   public static class VarVarArray<S> {
      private Var<S[]> value;
      public Var<S[]> value() {
         return value;
      }
      public static <S> VarVarArray<S> of(Var<S[]> value) {
         return new VarVarArray<S>(value);
      }
      public VarVarArray() {
      }
      public VarVarArray(Var<S[]> value) {
         this.value = value;  // !!clone?
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof VarVarArray &&
            java.util.Arrays.deepEquals(((VarVarArray<?>)other).value.value, value.value);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return "VarVarArray<"+java.util.Objects.toString(value)+">";
      }
   }
   public static class WithVar {
      private Var<Point> valuePoint;
      public Var<Point> valuePoint() {
         return valuePoint;
      }
      public static WithVar of(Var<Point> valuePoint) {
         return new WithVar(valuePoint);
      }
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
   public static class WithVarVarT<T> {
      private Var<Var<T>> value;
      public Var<Var<T>> value() {
         return value;
      }
      public static <T> WithVarVarT<T> of(Var<Var<T>> value) {
         return new WithVarVarT<T>(value);
      }
      public WithVarVarT() {
      }
      public WithVarVarT(Var<Var<T>> value) {
         this.value = value;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof WithVarVarT &&
            java.util.Objects.deepEquals(((WithVarVarT)other).value, value);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return "WithVarVarT<"+java.util.Objects.toString(value)+">";
      }
   }
   public static class WithVarVar {
      private Var<Var<Point>> valuePoint;
      public Var<Var<Point>> valuePoint() {
         return valuePoint;
      }
      public static WithVarVar of(Var<Var<Point>> valuePoint) {
         return new WithVarVar(valuePoint);
      }
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
      public VarArray<Point> valuePoint() {
         return valuePoint;
      }
      public static WithVarArray of(VarArray<Point> valuePoint) {
         return new WithVarArray(valuePoint);
      }
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
   public static class WithVarVarArray {
      private VarVarArray<Point> valuePoint;
      public VarVarArray<Point> valuePoint() {
         return valuePoint;
      }
      public static WithVarVarArray of(VarVarArray<Point> valuePoint) {
         return new WithVarVarArray(valuePoint);
      }
      public WithVarVarArray() {
      }
      public WithVarVarArray(VarVarArray<Point> valuePoint) {
         this.valuePoint = valuePoint;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof WithVarVarArray &&
            java.util.Objects.deepEquals(((WithVarVarArray)other).valuePoint, valuePoint);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return "WithVarVarArray<"+java.util.Objects.toString(valuePoint)+">";
      }
   }
   public static class WithWithVarVarT {
      private WithVarVarT<Point> valuePoint;
      public WithVarVarT<Point> valuePoint() {
         return valuePoint;
      }
      public static WithWithVarVarT of(WithVarVarT<Point> valuePoint) {
         return new WithWithVarVarT(valuePoint);
      }
      public WithWithVarVarT() {
      }
      public WithWithVarVarT(WithVarVarT<Point> valuePoint) {
         this.valuePoint = valuePoint;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof WithWithVarVarT &&
            java.util.Objects.deepEquals(((WithWithVarVarT)other).valuePoint, valuePoint);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return "WithWithVarVarT<"+java.util.Objects.toString(valuePoint)+">";
      }
   }
   public static class WithGenericArray {
      private Var<Point>[] array;
      public Var<Point>[] array() {
         return arrayClone(array);
      }
      public static WithGenericArray of(Var<Point>[] array) {
         return new WithGenericArray(array);
      }
      public WithGenericArray() { // for serial
      }
      /* pp */ WithGenericArray(Var<Point>[] array) { // for us
         this.array = array;
      }
      @Override public boolean equals(Object other) {
         return
            other instanceof WithGenericArray &&
            java.util.Objects.deepEquals(((WithGenericArray)other).array, array);
      }
      @Override public int hashCode() {
         return 1; // Correctness over efficiency!
      }
      @Override public String toString() {
         return java.util.Arrays.toString(array);
      }
   }
   public static void main(String[] args) throws IOException {
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
      Point[] repeatFieldCopy = fieldCopy(Point[].class, repeat);
      asrt(repeatFieldCopy[0] == repeatFieldCopy[1], "Repeated instance not the same instance");
      Point[] repeatValueFieldCopy = valueFieldCopy(Point[].class, repeat);
      asrt(repeatValueFieldCopy[0] == repeatValueFieldCopy[1], "Repeated instance not the same instance");
      
      // nulls
      check(WithField.class, new WithField(null));
      check(WithArray.class, new WithArray(new Point[][] { null }));

      // Generics
      check(WithVar.class, new WithVar(new Var<>(new Point(12, 13)))); // !! return type genesis check
      check(WithVarVar.class, new WithVarVar(new Var<>(new Var<>(new Point(14, 15)))));
      check(WithVarArray.class, new WithVarArray(new VarArray<>(new Point[] { new Point(15, 16) })));
      check(WithVarVarArray.class, new WithVarVarArray(new VarVarArray<>(new Var<>(new Point[] { new Point(15, 16) }))));
      check(WithWithVarVarT.class, new WithWithVarVarT(new WithVarVarT<>(new Var<>(new Var<>(new Point(15, 16))))));
      check(WithGenericArray.class, new WithGenericArray((Var<Point>[])new Var<?>[] { new Var<>(new Point(20, 21))}));
   }
   private static <T> void check(Class<T> clazz, T obj) throws IOException {
      assertEquals("Fields", obj, fieldCopy(clazz, obj));
      assertEquals("Fields", obj, valueFieldCopy(clazz, obj));
      assertEquals("Fields", obj, valueCopy(clazz, obj));
   }
   private static void assertEquals(String msg, Object expected, Object actual) {
      asrt(
         java.util.Objects.deepEquals(expected, actual),
         msg+": expected <"+expected+"> was <"+actual+">"
      );
   }
   private static <T> T valueCopy(Class<T> clazz, T obj) throws IOException {
      return valueFromBytes(valueToBytes(clazz, obj), clazz);
   }
   private static <T> T fieldCopy(Class<T> clazz, T obj) throws IOException {
      return fieldFromBytes(fieldToBytes(clazz, obj), clazz);
   }
   private static <T> T valueFieldCopy(Class<T> clazz, T obj) throws IOException {
      return fieldFromBytes(valueToBytes(clazz, obj), clazz);
   }
   private static <T> T fieldFromBytes(byte[] bytes, Class<T> clazz) throws IOException {
      return FieldDeserializer.deserialize(
         fromBytes(bytes), clazz
      );
   }
   private static <T> T valueFromBytes(byte[] bytes, Class<T> clazz) throws IOException {
      return ValueDeserializer.deserialize(
         fromBytes(bytes), clazz
      );
   }
   private static DataInput fromBytes(byte[] bytes) {
      return new DataInputStream(new ByteArrayInputStream(bytes));
   }
   private static <T> byte[] valueToBytes(Class<T> clazz, T obj) throws IOException {
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      ValueSerializer.serialize(new DataOutputStream(byteOut), clazz, obj);
      return byteOut.toByteArray();
   }
   private static <T> byte[] fieldToBytes(Class<T> clazz, T obj) throws IOException {
      ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
      FieldSerializer.serialize(new DataOutputStream(byteOut), clazz, obj);
      return byteOut.toByteArray();
   }
   private static void asrt(boolean that, String msg) {
      if (!that) {
         fail(msg);
      }
   }
   private static void fail(String msg) {
      throw new AssertionError(msg);
   }
   private static <T> T arrayClone(T obj) {
      if (!obj.getClass().isArray()) {
         throw new IllegalArgumentException("Must be an array.");
      }
      return maybeArrayClone(obj);
   }
   private static <T> T maybeArrayClone(T obj) {
      if (obj == null) {
         return null;
      }
      Class<?> clazz = obj.getClass();
      if (clazz.isArray()) {
         int len = Array.getLength(obj);
         Object newArray = Array.newInstance(clazz.getComponentType(), len);
         for (int i=0; i<len; ++i) {
            Array.set(newArray, i, maybeArrayClone(Array.get(obj, i)));
         }
         return (T)clazz.cast(newArray);
      } else {
         return obj;
      }
   }
}
