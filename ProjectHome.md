A collection of source code showing how to use JSR 292 to implement usual patterns that you can find in dynamic language runtimes.

### Patterns ###
Constants
  * [lazy initialization](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/lazy-init/src/jsr292/cookbook/lazyinit/RT.java) (of a constant array)
  * [almost (volatile) static final field](https://code.google.com/p/jsr292-cookbook/source/browse/trunk/almost-final/src/jsr292/cookbook/almostfinal/)

Method handle interceptors
  * [before](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/interceptors/src/jsr292/cookbook/interceptors/Interceptors.java#10), [after](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/interceptors/src/jsr292/cookbook/interceptors/Interceptors.java#32)
  * [try/finally](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/interceptors/src/jsr292/cookbook/interceptors/Interceptors.java#68)
  * [proxy](https://code.google.com/p/jsr292-cookbook/source/browse/trunk/proxy/src/jsr292/cookbook/proxy/Proxy.java)

Callsite adaptation
  * conversion/boxing/unboxing (not yet implemented)
  * varargs (not yet implemented)
  * named parameters (not yet implemented)

Single-dispatch (one receiver)
  * inlining caches
    * [cascaded inlining cache](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/inlining-cache/src/jsr292/cookbook/icache/RT.java)
    * [bimorphic inlining cache](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/bimorphic-cache/src/jsr292/cookbook/bicache/RT.java)
Double dispatch
  * visitor
    * [visitor](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/visitor/src/jsr292/cookbook/visitor/RT.java)
Multi-dispatch (several receivers)
  * multi-dispatch (bitset based)
    * [multi-dispatch](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/multi-dispatch/src/jsr292/cookbook/mdispatch/RT.java)

Callee adaptation
  * [verified entry point & vtable](VerfiedEntryPoint.md)
  * [memoization](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/memoize/src/jsr292/cookbook/memoize/RT.java)

Mutable metaclass
  * [metaclass & invalidation](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/metaclass/src/jsr292/cookbook/metaclass/RT.java)





