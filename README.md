DO NOT USE THIS. It's Christmas time. I haven't written a serializer. There's loads of bad ones about. Seems easy.

Goals

 - See how easy it is to write a serializer.
 - Explore why some of the decisions are made.
 - Note how unhelpful the libraries we are using are.
 - Have a bit of fun.

Non-goals

 - Create any useful code.
 - Bother testing.
 - Writting documentation for useless code.
 - Nice errors.
 - Efficiency.
 - Worry too much about the likes of long functions.

Current state

 - Reads/writes fields of an object.
 - Reads/writes arrays elements.
 - Checks types have not changed.
 - Checks names.
 - Constructs object through nullary constructor.
 - Constructs arrays.

Things it does not do

 - Check whether the class actually wants to play.
 - Special case those classes not playing.
 - Check type parameters.
 - Class hierarchies.
 - Write class info only once.
 - Ensure fields in stream match set of fields in runtime class and are in order with no repeats. (Thanks Sami.)
 - Handle same object used multiple times.
 - Hnalde cycles.

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

Anything else.

 - I wrote `name == "."`. Idiot.
 - github is a miserable piece of junk.
 - Writting the serializer/deserializer as an object and recrusing within same instance is cool, but don't expose that to the client or more particularly to the serialized/deserialized objects.
 - Primitives - bleurgh.
 - Given that there are primitives, more utility methods? Or not.
