package jsr292.cookbook.mdispatch;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

class SmallSelectorMetadata extends SelectorMetadata {
  final MethodHandle[] mhs;
  private final PositionInfo[] positionInfos;
  
  private SmallSelectorMetadata(MethodHandle[] mhs, PositionInfo[] positionInfos) {
    this.mhs = mhs;
    this.positionInfos = positionInfos;
  }
  
  static class PositionInfo extends AbstractPositionInfo {
    final SmallClassBitMap classBitMap;
    
    public PositionInfo(int projectionIndex, boolean mayBoxUnbox, SmallClassBitMap classIntMap) {
      super(projectionIndex, mayBoxUnbox);
      this.classBitMap = classIntMap;
    }
  }
  
  static final MethodHandle FALLBACK;
  static {
    try {
      FALLBACK = MethodHandles.publicLookup().findVirtual(MultiDispatchCallSite.class,
          "fallback", MethodType.methodType(Object.class, Object[].class));
    } catch (ReflectiveOperationException e) {
      throw (AssertionError)new AssertionError().initCause(e);
    }
  }
  
  public class MultiDispatchCallSite extends MutableCallSite {
    private final boolean isStatic; //FIXME
    private final int constBits;
    private final ArrayList<PositionInfo> actualPosInfos;
    private final MethodHandle fallback;
    private int counter;   // data-race to this field are ok 
    
    private static final int MAX_COUNTER = 3;
    
    MultiDispatchCallSite(MethodType type, boolean isStatic, int constBits, ArrayList<PositionInfo> actualPosInfos) {
      super(type);
      this.isStatic = isStatic;
      this.constBits = constBits;
      this.actualPosInfos = actualPosInfos;
      
      MethodHandle fallback = FALLBACK.bindTo(this).asCollector(Object[].class, type.parameterCount()).asType(type);
      setTarget(fallback);
      this.fallback = fallback;
    }
    
    public Object fallback(Object[] args) throws Throwable {
      //System.out.println("fallback "+Arrays.toString(args));
      if (++counter >= MAX_COUNTER) {
        // don't use a cache anymore
        MethodHandle target = getTargetMethodHandle();
        setTarget(target);
        //System.out.println("bit map method target "+target);
        return target.invokeWithArguments(args);
      }
      
      // find target
      int bits = constBits;
      for(PositionInfo positionInfo: actualPosInfos) {
        int index = positionInfo.projectionIndex;
        bits = bits & positionInfo.classBitMap.lookup(args[index].getClass()); 
      }
      MethodHandle target = mhs[Integer.numberOfTrailingZeros(bits)];
      
      // install guards for significant parameters
      MethodHandle guards = target.asType(type());
      for(PositionInfo positionInfo: actualPosInfos) {
        int index = positionInfo.projectionIndex;
        MethodHandle test = RT.CHECK_CLASS.bindTo(args[index].getClass()).
            asType(MethodType.methodType(boolean.class, type().parameterType(index)));
        if (index != 0) {
          test = MethodHandles.dropArguments(test, 0, type().parameterList().subList(0, index));
        }
        guards = MethodHandles.guardWithTest(test, guards, fallback);
      }
      
      setTarget(guards);
      //System.out.println("cache method target "+target);
      return target.invokeWithArguments(args);
    }
    
    private MethodHandle getTargetMethodHandle() {
      MethodType type = type();
      
      if (actualPosInfos.size() == 1) {
        //System.out.println("one actual pos, transfer to a class map");
        
        // we can pre-calculate all method handles and use the exact invoker
        PositionInfo positionInfo = actualPosInfos.get(0);
        int projectionIndex = positionInfo.projectionIndex;
        ClassMHMap classMHMap = positionInfo.classBitMap.transfer(constBits, projectionIndex, mhs, type);
        
        MethodHandle classMHMapLookup = CLASSMHMAP_LOOKUP.bindTo(classMHMap);
        MethodHandle getMH = MethodHandles.filterReturnValue(OBJECT_GET_CLASS, classMHMapLookup);
        
        if (type.parameterCount() != 1) {
          getMH = MethodHandles.permuteArguments(getMH,
              type.changeReturnType(MethodHandle.class).changeParameterType(projectionIndex, Object.class),
              new int[] { projectionIndex });
        } else {
           getMH = getMH.asType(type.changeReturnType(MethodHandle.class).changeParameterType(0, Object.class));
        }
        
        return MethodHandles.foldArguments(
            MethodHandles.exactInvoker(type),
            getMH);
      }
      
      // normalize method handles to the callsite type
      MethodHandle[] array = new MethodHandle[mhs.length];
      for(int i=0; i<array.length; i++) {  
        array[i] = mhs[i].asType(type);  //FIXME, may throw a WMTE
      }
      
      // construct a tree of method handle, prepend static const bits set if necessary
      MethodHandle bitReducer = SmallBitReducer.getReducer(actualPosInfos.size() + ((constBits!=0)? 1: 0));
      bitReducer = bitReducer.bindTo(array);
      if (constBits != 0) {
        bitReducer = bitReducer.bindTo(constBits);
      }
      
      boolean mayBoxUnbox = false;
      MethodHandle[] filters = new MethodHandle[actualPosInfos.size()];
      for(int i=0; i<filters.length; i++) {
        PositionInfo positionInfo = actualPosInfos.get(i);
        MethodHandle classBitMapLookup = SMALLCLASSBITMAP_LOOKUP.bindTo(positionInfo.classBitMap);
        MethodHandle filter = MethodHandles.filterReturnValue(OBJECT_GET_CLASS, classBitMapLookup);
        filters[i] = filter;
        
        mayBoxUnbox |= positionInfo.mayBoxUnbox;
      }
      MethodHandle getMH = MethodHandles.filterArguments(bitReducer, 0, filters);
      
      if (actualPosInfos.size() != type.parameterCount()) {
        getMH = MethodHandles.permuteArguments(getMH,
            //FIXME, don't use generic method here !
            MethodType.genericMethodType(type.parameterCount()).changeReturnType(MethodHandle.class),
            toArray(actualPosInfos));
      }
      getMH = getMH.asType(type.changeReturnType(MethodHandle.class));
      return MethodHandles.foldArguments(
          //FIXME invoker() in jdk7b144 returns a wrong method type, so add asType as a workaround
          (mayBoxUnbox)? MethodHandles.invoker(type).asType(type.insertParameterTypes(0, MethodHandle.class)): MethodHandles.exactInvoker(type),
          getMH);
    }
  }
  
  @Override
  MethodHandle createMethodHandle(MethodType type) {
    MethodHandle[] mhs = this.mhs;
    
    //System.out.println("create method handle "+java.util.Arrays.toString(mhs));
    
    // try to simplify using static information
    int constBits = 0xFFFFFFFF;
    ArrayList<PositionInfo> actualPosInfos = new ArrayList<>();
    for(PositionInfo  positionInfo: positionInfos) {
      int projectionIndex = positionInfo.projectionIndex;
      Class<?> klass = type.parameterType(projectionIndex);
      if (klass.isPrimitive() ||
          Modifier.isFinal(klass.getModifiers())) {  //FIXME add || a hierarchy check 
        int bits = positionInfo.classBitMap.lookup(klass);
        constBits = constBits & bits;
        continue;
      }
      actualPosInfos.add(positionInfo);
    }
    
    if (actualPosInfos.isEmpty()) {  // static call
      //System.out.println("static freezed "+ mhs[Integer.numberOfTrailingZeros(constBits)]);
      return mhs[Integer.numberOfTrailingZeros(constBits)].asType(type);
    }
    
    // try to install a cache
    //System.out.println("cache " + type);
    MultiDispatchCallSite callSite = new MultiDispatchCallSite(type, true/*FIXME*/, constBits, actualPosInfos);
    return callSite.dynamicInvoker();
  } 
  
  static final MethodHandle OBJECT_GET_CLASS;
  static final MethodHandle SMALLCLASSBITMAP_LOOKUP;
  static final MethodHandle CLASSMHMAP_LOOKUP;
  static {
    Lookup lookup = MethodHandles.publicLookup();
    try {
      OBJECT_GET_CLASS = lookup.findVirtual(Object.class, "getClass",
          MethodType.methodType(Class.class));
      SMALLCLASSBITMAP_LOOKUP = lookup.findVirtual(SmallClassBitMap.class, "lookup",
          MethodType.methodType(int.class, Class.class));
      CLASSMHMAP_LOOKUP = lookup.findVirtual(ClassMHMap.class, "lookup",
          MethodType.methodType(MethodHandle.class, Class.class));
    } catch (ReflectiveOperationException e) {
      throw (AssertionError)new AssertionError().initCause(e);
    }
  }

  public static SmallSelectorMetadata create(List<MethodHandle> mhList) {
    assert mhList.size() <= 32;
    
    int length = mhList.get(0).type().parameterCount();
    @SuppressWarnings("unchecked")
        HashSet<Class<?>>[] sets = (HashSet<Class<?>>[])new HashSet<?>[length]; 
    @SuppressWarnings("unchecked")
        HashSet<Class<?>>[] boxUnboxSets = (HashSet<Class<?>>[])new HashSet<?>[length]; 
    for(int i=0; i<length; i++) {
      sets[i] = new HashSet<>();
    }
    
    // find types by position
    for(int i=0; i<mhList.size(); i++) {
      MethodHandle mh = mhList.get(i);
      MethodType type = mh.type();
      
      for(int j=0; j<length; j++) {
        Class<?> klass = type.parameterType(j);
        if (klass.isInterface()) {
          throw new UnsupportedOperationException("interface " + klass.getName() + " aren't currently supported "+mh);
        }
        sets[j].add(klass);
        
        // supplementary primitive/wrapper set used to handle boxing/unboxing
        HashSet<Class<?>> convSet = PRIMITIVE_CONVERSION_MAP.get(klass);
        if (convSet != null) {
          HashSet<Class<?>> boxUnboxSet = boxUnboxSets[j];
          if (boxUnboxSet == null) { // lazy allocation
            boxUnboxSet = boxUnboxSets[j] = new HashSet<>();
          }
          boxUnboxSet.addAll(convSet);
        }
      }
    }
    
    // determine projection
    ArrayList<PositionInfo> positionInfos = new ArrayList<>();
    for(int i=0; i<sets.length; i++) {
      HashSet<Class<?>> set = sets[i];
      if (set.size() == 1) { // only one type
        continue;
      }
      
      // add primitive/wrapper classes
      boolean mayBoxUnbox = boxUnboxSets[i] != null;
      if (mayBoxUnbox) {
        set.addAll(boxUnboxSets[i]);
      }
      
      positionInfos.add(new PositionInfo(i, mayBoxUnbox, new SmallClassBitMap(set.size())));
    }
    
    // topologically sort method handles 
    Lattice lattice = new Lattice(toArray(positionInfos));
    for(MethodHandle mh: mhList) {
      lattice.add(mh);
    }
    MethodHandle[] mhs = lattice.topologicalSort();
    
    //System.out.println("topological sort "+java.util.Arrays.toString(mhs));
    
    // populate SmallClassBitMaps
    int projectionLength = positionInfos.size();
    for(int i=0; i<projectionLength; i++) {
      PositionInfo positionInfo = positionInfos.get(i);
      int index = positionInfo.projectionIndex;
      HashSet<Class<?>> set = sets[index];
      SmallClassBitMap map = positionInfo.classBitMap;
      for(Class<?> type: set) {
        int bits = 0;
        for(int j=0; j<mhs.length; j++) {
          if (isAssignablefrom(mhs[j].type().parameterType(index), type)) {
            bits |= 1 << j;
          }
        }
        map.putNoResize(type, bits);
      }
    }
    
    //System.out.println("dispatch "+mhList);
    //System.out.println("position infos "+positionInfos);
    
    return new SmallSelectorMetadata(mhs,
        positionInfos.toArray(new PositionInfo[positionInfos.size()]));
  }

  private static boolean isAssignablefrom(Class<?> type1, Class<?> type2) {
    if (type1 == type2) {
      return true;
    }
    
    HashSet<Class<?>> conversionSet = PRIMITIVE_CONVERSION_MAP.get(type1);
    if (conversionSet != null) {  // type1 is a primitive, a wrapper, Object or Number
      if (conversionSet.contains(type2)) {
        return true;  // primitive or boxing conversion
      }
    }
    
    return type1.isAssignableFrom(type2);
  }
  
  static int[] toArray(List<PositionInfo> positionInfos) {
    int[] array = new int[positionInfos.size()];
    int i = 0;
    for(PositionInfo positionInfo: positionInfos) {
      array[i++] = positionInfo.projectionIndex;
    }
    return array;
  }
  
  
  
  private static final HashMap<Class<?>, HashSet<Class<?>>> PRIMITIVE_CONVERSION_MAP;
  static {
    Class<?>[][] array = new Class<?>[][] {
        { boolean.class, Boolean.class},
        { byte.class, Byte.class},
        { short.class, Short.class, byte.class, Byte.class},
        { char.class, Character.class, byte.class, Byte.class},
        { int.class, Integer.class, char.class, Character.class, short.class, Short.class, byte.class, Byte.class},
        { long.class, Long.class, int.class, Integer.class, char.class, Character.class, short.class, Short.class, byte.class, Byte.class},
        { float.class, Float.class, long.class, Long.class, int.class, Integer.class, char.class, Character.class, short.class, Short.class, byte.class, Byte.class},
        { double.class, Double.class, float.class, Float.class, long.class, Long.class, int.class, Integer.class, char.class, Character.class, short.class, Short.class, byte.class, Byte.class},
        
        { Boolean.class, boolean.class},
        { Byte.class, byte.class},
        { Short.class, short.class, byte.class},
        { Character.class, char.class, byte.class},
        { Integer.class, int.class, char.class, short.class, byte.class},
        { Long.class, long.class, int.class, char.class, short.class, byte.class},
        { Float.class, float.class, long.class, int.class, char.class, short.class, byte.class},
        { Double.class, double.class, float.class, long.class, int.class, char.class, short.class, byte.class},
    };
    
    HashMap<Class<?>, HashSet<Class<?>>> map = new HashMap<>();
    for(Class<?>[] classes: array) {
      HashSet<Class<?>> conversionSet = new HashSet<>();
      Collections.addAll(conversionSet, classes);
      map.put(classes[0], conversionSet);
    }
    
    HashSet<Class<?>> objectConvSet = new HashSet<>();
    Collections.addAll(objectConvSet, double.class, Double.class, float.class, Float.class, long.class, Long.class, int.class, Integer.class, char.class, Character.class, short.class, Short.class, byte.class, Byte.class);
    map.put(Number.class, new HashSet<>(objectConvSet));
    Collections.addAll(objectConvSet, boolean.class, Boolean.class);
    map.put(Object.class, objectConvSet);
    
    PRIMITIVE_CONVERSION_MAP = map;
  }
}
