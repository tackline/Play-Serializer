package tackline.play.serialize;

import java.lang.reflect.Type;
import java.util.*;

final class ObjectFormat {
   private final List<String> names;
   private final List<DataFormat> dataFormats;
   private final List<Type> types;
   private ObjectFormat (
      List<String> names,
      List<DataFormat> dataFormats,
      List<Type> types
   ) {
      this.names = FieldCommon.safe(names);
      this.dataFormats = FieldCommon.safe(dataFormats);
      this.types = FieldCommon.safe(types);
   }
   public static ObjectFormat of(
      List<String> names,
      List<DataFormat> dataFormats,
      List<Type> types
   ) {
      return new ObjectFormat(names, dataFormats, types);
   }
   public List<String> names() { return names; }
   public List<DataFormat> dataFormats() { return dataFormats; }
   public List<Type> types() { return types; }
}
