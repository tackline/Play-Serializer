package tackline.play.serialize;

import java.util.List;
 
interface Exploder {
   List<String> names();
   List<Object> explode(Object obj);
}
