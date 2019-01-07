package tackline.play.serialize;

import java.util.*;
 
/**
 * Property names and properties from a value object.
 */
interface Exploder {
   List<String> names();
   List<Object> explode(Object obj);
}
