package jsr292.cookbook.mdispatch;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

public class RT {
  private static final ClassValue<HashMap<Selector,Object>> SELECTOR_MAP_VALUE =
    new ClassValue<HashMap<Selector,Object>>() {
      @Override
      protected HashMap<Selector, Object> computeValue(Class<?> type) {
        Lookup lookup = MethodHandles.publicLookup();
        HashMap<Selector, ArrayList<MethodHandle>> map = new HashMap<>();
        for(Method method: type.getMethods()) {
          if (method.isBridge()) {
            continue;  // skip bridge
          }
          
          boolean isStatic = Modifier.isStatic(method.getModifiers());
          Selector selector = new Selector(method.getName(), method.getParameterTypes().length +
              (isStatic?0: 1));
          ArrayList<MethodHandle> list = map.get(selector);
          if (list == null) {
            list = new ArrayList<>();
            map.put(selector, list);
          }
          try {
            method.setAccessible(true);
            MethodHandle mh = lookup.unreflect(method);
            if (!isStatic) {
              // adapt the receiver type to be the one of current class
              mh = mh.asType(mh.type().changeParameterType(0, type));
            }
            list.add(mh);
          } catch (IllegalAccessException e) {
            throw (LinkageError)new LinkageError().initCause(e);
          }
        }
        
        HashMap<Selector, Object> selectorMap = new HashMap<>();
        for(Entry<Selector, ArrayList<MethodHandle>> entry: map.entrySet()) {
          // only store the method handles, create the metadata later when needed
          ArrayList<MethodHandle> mhs = entry.getValue();
          selectorMap.put(entry.getKey(), mhs.toArray(new MethodHandle[mhs.size()]));
        }
        
        return selectorMap;
      }
    };
    
  static class Selector {
    private final String name;
    private final int parameterCount;
    
    public Selector(String name, int parameterCount) {
      this.name = name;
      this.parameterCount = parameterCount;
    }
    
    @Override
    public int hashCode() {
      return name.hashCode() + parameterCount;
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Selector)) {
        return false;
      }
      Selector selector = (Selector)obj;
      return parameterCount == selector.parameterCount &&
             name.equals(selector.name);
    }
    
    @Override
    public String toString() {
      return name+'/'+parameterCount;
    }
  }
    
  static MethodHandle getMultiDispatchTarget(Lookup lookup, String name, MethodType type, Class<?> dispatchType) {
    try {
      Selector selector = new Selector(name, type.parameterCount());
      HashMap<Selector, Object> selectorMap = SELECTOR_MAP_VALUE.get(dispatchType);
      Object value = selectorMap.get(selector);
      if (value == null) {
        throw new LinkageError("no public method "+selector+" in "+dispatchType.getName());
      }
      
      SelectorMetadata metadata;
      if (value instanceof MethodHandle[]) {
        MethodHandle[] mhs = (MethodHandle[])value;  
        if (mhs.length == 1) {
          // only one method, no multi-dispatch
          //System.out.println("one virtual linking "+dispatchType.getName()+'.'+name+type+" in "+lookup.lookupClass().getName());
          return mhs[0].asType(type);
        }
        
        try {
          if (mhs.length <= 32) {
            metadata = SmallSelectorMetadata.create(mhs);
          } else {
            throw new UnsupportedOperationException("NYI");
            //selectorMetadata = JumboSelectorMetadata.create(mhs);
          }
        } catch(UnsupportedOperationException e) {
          throw new LinkageError("NIY, "+dispatchType+'.'+selector, e);
        }
        
        // entry is already preallocated, so only one variable is changed
        // there is also a data race but because the code acts as a cache, we don't care
        selectorMap.put(selector, metadata);
      } else {
        metadata = (SelectorMetadata)value;
      }
      return metadata.createMethodHandle(type);
      
    } catch(RuntimeException e) {
      throw new BootstrapMethodError(
          "error while linking "+dispatchType.getName()+'.'+name+type+" in "+lookup.lookupClass().getName(),
          e);
    }
  }
  
  public static CallSite invokestatic(Lookup lookup, String name, MethodType type, Class<?> staticType) throws NoSuchMethodException, IllegalAccessException {
    return new ConstantCallSite(getMultiDispatchTarget(lookup, name, type, staticType));
  }
  
  static class BimorphicCacheCallSite extends MutableCallSite {
    final Lookup lookup;
    final String name;
    
    private Class<?> class1;
    private MethodHandle mh1;
    private Class<?> class2;
    private MethodHandle mh2;

    BimorphicCacheCallSite(Lookup lookup, String name, MethodType type) {
      super(type);
      this.lookup = lookup;
      this.name = name;
    }
    
    public synchronized Object fallback(Object[] args) throws Throwable {
      if (class1 != null && class2 != null) {
        // bimorphic cache defeated, use a dispatch table instead
        return fallbackToDispatchTable(args);
      }
      
      MethodType type = type();
      Object receiver = args[0];
      Class<?> receiverClass = receiver.getClass();
      MethodHandle target = getMultiDispatchTarget(lookup, name, type, receiverClass);
      
      MethodHandle test = CHECK_CLASS.bindTo(receiverClass);
      test = test.asType(test.type().changeParameterType(0, type.parameterType(0)));
      
      MethodHandle guard = MethodHandles.guardWithTest(test, target, getTarget());
      if (class1 == null) {
        class1 = receiverClass;
        mh1 = target;
      } else {
        class2 = receiverClass;
        mh2 = target;
      }
      
      setTarget(guard);
      return target.invokeWithArguments(args);
    }
    
    private Object fallbackToDispatchTable(Object[] args) throws Throwable {
      assert Thread.holdsLock(this);
      
      final MethodType type = type();
      DispatchMap dispatchMap = new DispatchMap() {
        @Override
        protected MethodHandle findMethodHandle(Class<?> receiverClass) throws Throwable {
          return getMultiDispatchTarget(lookup, name, type, receiverClass);
        }
      };
      dispatchMap.populate(class1, mh1, class1, mh2);   // pre-populated with known couples
      class1 = class2 = null;  
      mh1 = mh2 = null;  // free for GC
      
      MethodHandle lookupMH = MethodHandles.filterReturnValue(GET_CLASS, LOOKUP_MH.bindTo(dispatchMap));
      lookupMH = lookupMH.asType(MethodType.methodType(MethodHandle.class, type.parameterType(0)));
      MethodHandle target = MethodHandles.foldArguments(MethodHandles.exactInvoker(type), lookupMH);
      setTarget(target);
      return target.invokeWithArguments(args);
    }
  }
  
  public static CallSite invokevirtual(Lookup lookup, String name, MethodType type) {
    BimorphicCacheCallSite callSite = new BimorphicCacheCallSite(lookup, name, type);
    
    MethodHandle fallback = FALLBACK.bindTo(callSite);
    fallback = fallback.asCollector(Object[].class, type.parameterCount());
    fallback = fallback.asType(type);
    
    callSite.setTarget(fallback);
    return callSite;
  }
  
  public static boolean checkClass(Class<?> clazz, Object receiver) {
    return receiver.getClass() == clazz;
  }
  
  static final MethodHandle CHECK_CLASS;
  private static final MethodHandle FALLBACK;
  static final MethodHandle GET_CLASS;
  static final MethodHandle LOOKUP_MH;
  static {
    Lookup lookup = MethodHandles.lookup();
    try {
      CHECK_CLASS = lookup.findStatic(RT.class, "checkClass",
          MethodType.methodType(boolean.class, Class.class, Object.class));
      FALLBACK = lookup.findVirtual(BimorphicCacheCallSite.class, "fallback",
          MethodType.methodType(Object.class, Object[].class));
      GET_CLASS = lookup.findVirtual(Object.class, "getClass",
          MethodType.methodType(Class.class));
      LOOKUP_MH =   lookup.findVirtual(DispatchMap.class, "lookup",
          MethodType.methodType(MethodHandle.class, Class.class));
    } catch (ReflectiveOperationException e) {
      throw (AssertionError)new AssertionError().initCause(e);
    }
  }
}
