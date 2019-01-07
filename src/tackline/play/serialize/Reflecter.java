package tackline.play.serialize;

interface Reflecter {

   Exploder oldObject(Class<?> clazz);

   <T> Imploder newObject(KnownType type, Class<T> clazz, TypeParamMap typeMap);

}