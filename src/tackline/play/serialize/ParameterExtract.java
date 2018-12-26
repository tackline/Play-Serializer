package tackline.play.serialize;

import java.lang.reflect.Type;

interface ParameterExtract<R, EXC extends Throwable> {
   R class_(Class<?> rawType, Type[] typeArgs) throws EXC;
   R array(Type componentType) throws EXC;
}