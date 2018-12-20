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

 - Reads/writes primitive fields of an object.
 - Checks types have not changed.
 - !! Checks names.
 - Constructs object through nullary constructor.

Things it does not do

 - Check whether the class actually wants to play.
 - Array fields.
 - Referenced objects.
 - Class hierarchies.
 - Write class info only once.
 - Ensure fields in stream match set of fields in runtime class and are in order with no repeats. (Thanks Sami.)

Things we don't like in reflection API

 - Handling of modifiers.
 - setAccessible rather that is just accessible all the way.
 - Given that there is setAccessible, at least return a type that wont threaten to complain that it can't access. Don't give me IllegalAccessException.
 - Similarly forced checked exception IllegalArgumentException even for a nullary.
 - Also InstantiationException ("not limited to" indeed).
 - InvovationTargetExcpetion and rethrowing even if we've checked checked excpeption clauses.

Anything else.

 - I wrote `name == "."`. Idiot.
 - github is a miserable piece of junk.
