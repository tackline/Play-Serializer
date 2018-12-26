package tackline.play.serialize;

import java.io.DataOutput;
import java.io.IOException;

public class ValueSerializer extends FieldSerializer {
   /* pp */ ValueSerializer(DataOutput out) {
      super(out);
   }
   public static <T> void serialize(DataOutput out, Class<T> clazz, T obj) throws IOException {
      new ValueSerializer(out).serialize(clazz, obj);
   }
}
