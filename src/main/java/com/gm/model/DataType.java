package com.gm.model;

import java.util.HashMap;
import java.util.Map;

public enum DataType {
  AUTO(false),
  INTEGER(false, short.class, int.class, long.class, Integer.class, Long.class, Short.class),
  REAL(false, double.class, float.class, Double.class, Float.class),
  TEXT(true, String.class),
  BLOB(true);
  
  private final Class<?>[] classes;
  private final boolean nullable;
  
  private DataType(boolean nullable, Class<?>... classes) {
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

  public static DataType fromClass(Class<?> c) {
    return classToType.get(c);
  }
}
