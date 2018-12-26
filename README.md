DO NOT USE THIS. It's Christmas time. I haven't written a serializer. There's loads of bad ones about. Seems easy.

NB: This code as absolutely not safe. You knew that. It reads and writes private fields with no opt-in check.

Goals

 - See how easy it is to write a serializer.
 - Explore why some of the decisions are made.
 - Note how unhelpful the libraries we are using are.
 - Start with a field serializer (because that seems easy) and move on to value classes with proper interfaces.
 - Have a bit of fun.

Non-goals

 - Create any useful code.
 - Bother testing.
 - Writting documentation for useless code.
 - Nice errors.
 - Efficiency.
 - Worry too much about the likes of long functions.
 - Do any dodgy `Class.forName`ing from stream data - static typing all the way, cafebabe!

Current state

 - Writes a dag according to static type information.
 - Checks for cycles.
 - Reads/writes fields of an object.
 - Reads/writes arrays elements.
 - Checks field names and reorders.
 - Constructs object through nullary constructor.
 - Constructs arrays.
 - Detects same object referred to by references of different static types.
 - Handle null.
 - Handles classes with type parameter/fields with type argument.
 - Generic arrays.
 - Classes parameterised with generic arrays, or something.
 - Primitive type argument substitution hack - the java.lang.reflect is not sufficiently specified to allow this to behave correctly(!).
 - Looks up type variables using name rather than assuming undocumented equals/hashCode works sensibly.
 - Arrays of parameterized types.

Things it does not do

 - Check whether the class actually wants to play.
 - Special case those classes not playing.
 - Check type bounds.
 - Class hierarchies - I'm all about the base^Wstatic type..
 - DRY class info.
 - Ensure fields in stream match set of fields in runtime class and are in order with no repeats. (Thanks Sami.) 
 - Handle cycles.

Things we don't like in reflection API

 - Handling of modifiers.
 - setAccessible rather that is just accessible all the way.
 - Given that there is setAccessible, at least return a type that wont threaten to complain that it can't access. Don't give me IllegalAccessException.
 - Similarly forced checked exception IllegalArgumentException even for a nullary.
 - Also InstantiationException ("not limited to" indeed).
 - InvovationTargetExcpetion and rethrowing even if we've checked checked excpeption clauses.
 - Test of type separate from safe usage, IYSWIM. Notably arrays.
 - Caller sensitive methods.
 - Not being object capability based. 
 - Type - Casting to Class in order to call isPrimitive - one or the other!
 - Mostly undefined equals/hashCode of Types together with the inability of deriving types.
 - Why does ParameterizedType.getRawType not return Class<?>? (Possibly others.)

Anything else.

 - I wrote `name == "."`. Idiot.
 - github is a miserable piece of junk.
 - Serialization is really for value type, which should be immutable, so shouldn't be going around with non-final fields and no-arg constructors.
 - Writting the serializer/deserializer as an object and recrusing within same instance is cool, but don't expose that to the client or more particularly to the serialized/deserialized objects.
 - Primitives - bleurgh.
 - Given that there are primitives, more utility methods? Or not.
 - I'm really thinking about serializing through public interfaces here, without downcasting nastiness.
 - Fun thing: If you try to read junk, that's an IOException; try to write junk and that's an IllegalArgumentException.
 - I used a generic constructor! (though not for anything particularly useuful)
 - Generics for handling return and exception types are terrible.
 - Keywords considered a bad idea (context sensitive keywords moreso).
 - Why am I bothering with arrays of reference types - they suck. (A. to get a grip on what is going on in the reflection API.)
