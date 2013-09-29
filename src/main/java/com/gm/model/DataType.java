package com.gm.model;

import java.util.HashMap;
import java.util.Map;

public enum DataType {
  AUTO(false, null),
  INTEGER(false, "Int", short.class, int.class, long.class, Integer.class, Long.class, Short.class),
  REAL(false, "Double", double.class, float.class, Double.class, Float.class),
  TEXT(true, "String", String.class),
  BLOB(true, "Bytes");
  
  private final Class<?>[] classes;
  private final String className;
  private final boolean nullable;
  
  private DataType(boolean nullable, String className, Class<?>... classes) {
    this.className = className;
    this.classes = classes;
    this.nullable = nullable;
  }
  
  private static final Map<Class<?>, DataType> classToType = 
      new HashMap<Class<?>, DataType>();
  
  static {
    for (DataType type : values()) {
      for (Class<?> c : type.classes) {
        classToType.put(c, type);
      }
    }
  }

  public boolean isNullable() {
    return nullable;
  }

  public String getClassName() {
    return className;
  }
  
  public static DataType fromClass(Class<?> c) {
    return classToType.get(c);
  }
}
