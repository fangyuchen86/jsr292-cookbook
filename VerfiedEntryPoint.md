# Verified Entry Point #

This pattern doesn't speed up anything but help to reduce
the memory consumption by sharing an inlining cache with one target
for all callsite that calls the same method implementation.

Instead of creating one inlining cache by callsite,
the inlining cache is constructed once by element of the vtable
and shared by several callsites.

At callsite, the current [CallSite](http://download.java.net/jdk7/docs/api/java/lang/invoke/CallSite.html) object is passed as first parameter
of the call. If the inlining cache fails because the receiver class is not
equals to the class, the current implementation fallback to a Java vtable call.
One can also installs the inlining cache of the receiver's class.

### Code ###

[jsr292/cookbook/vep/RT.java](http://code.google.com/p/jsr292-cookbook/source/browse/trunk/verified-entrypoint/src/jsr292/cookbook/vep/RT.java)

### References ###

[Hotspot Wiki - Virtual Calls](http://wikis.sun.com/display/HotSpotInternals/VirtualCalls)