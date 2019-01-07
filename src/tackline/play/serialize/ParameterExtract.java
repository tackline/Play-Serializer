package tackline.play.serialize;

interface ParameterExtract<R, EXC extends Throwable> {
   R class_(Class<?> rawType, KnownType[] args) throws EXC;
   R array(KnownType component) throws EXC;
}