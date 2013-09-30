package com.gm.apt;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.JavaFileObject;

import com.gm.model.DataType;
import com.gm.model.Entity;
import com.gm.model.Property;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@SupportedAnnotationTypes({
  "com.gm.model.Property",
  "com.gm.model.Entity" 
})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ModelProcessor extends AbstractProcessor {
  
  private static final Map<String, Class<?>> primitiveClasses = new HashMap<String, Class<?>>();
  static {
    for (Class<?> c : new Class<?>[] {int.class, short.class, long.class}) {
      primitiveClasses.put(c.getName(), c);
    }
  }
  
  private static final Map<Class<?>, String> classToName = new HashMap<Class<?>, String>();
  static {
    classToName.put(int.class, "Integer");
    classToName.put(short.class, "Short");
    classToName.put(long.class, "Long");
    classToName.put(Integer.class, "Integer");
    classToName.put(Short.class, "Short");
    classToName.put(Long.class, "Long");
    classToName.put(String.class, "String");
  }
  
  private static final Map<Class<?>, String> classToNameForMethod = new HashMap<Class<?>, String>();
  static {
    classToNameForMethod.put(int.class, "Int");
    classToNameForMethod.put(short.class, "Short");
    classToNameForMethod.put(long.class, "Long");
    classToNameForMethod.put(Integer.class, "Int");
    classToNameForMethod.put(Short.class, "Short");
    classToNameForMethod.put(Long.class, "Long");
    classToNameForMethod.put(String.class, "String");
  }
  
  public static final class Field {
    private final String name;
    private final DataType type;
    private final boolean isKey;
    private final String capName;
    private final Class<?> clazz;

    public String getName() {
      return name;
    }

    public DataType getType() {
      return type;
    }

    public boolean isKey() {
      return isKey;
    }

    public String getCapName() {
      return capName;
    }
    
    public String getClassName() {
      return classToName.get(clazz);
    }
    
    public String getClassNameForMethod() {
      return classToNameForMethod.get(clazz);
    }

    public Field(String name, Class<?> clazz, boolean isKey) {
      this.name = name;
      this.type = DataType.fromClass(clazz);
      if (type == null) {
        System.err.printf("Unable to determine the data type for %s:%s\n", name, clazz);
        System.exit(-1);
      }
      this.clazz = clazz;
      this.isKey = isKey;
      this.capName = name.substring(0, 1).toUpperCase() + name.substring(1);
    }
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
    Set<? extends Element> elements = env.getElementsAnnotatedWith(Property.class);
    
    Map<String, Set<Field>> typeToFields = new HashMap<String, Set<Field>>();
    Map<String, String> typeToSuper = new HashMap<String, String>();
    Set<String> entities = new HashSet<String>();
        
    for (Element element : elements) {
      TypeElement typeElement = (TypeElement) element.getEnclosingElement();
      String typeName = stripParametricType(typeElement.toString());
      if (!typeToFields.containsKey(typeName)) {
        typeToFields.put(typeName, new HashSet<ModelProcessor.Field>());
        if (typeElement.getAnnotation(Entity.class) != null) {
          entities.add(typeName);
        }
        if (typeElement.getSuperclass().getKind() != TypeKind.NONE) {
          String superTypeName = stripParametricType(typeElement.getSuperclass().toString());
          typeToSuper.put(typeName, superTypeName);
        }
      }
      
      String fieldName = element.toString();
      String fieldTypeName = element.asType().toString();
      Class<?> fieldClass = primitiveClasses.get(fieldTypeName);
      try {
        if (fieldClass == null) {
          fieldClass = Class.forName(fieldTypeName);
        }
      } catch (ClassNotFoundException e) {
        System.err.printf("Unable to load the class for %s:%s in %s\n",
            fieldName, fieldTypeName, typeName);
        return false;
      }

      typeToFields.get(typeName).add(new Field(fieldName, fieldClass, 
          element.getAnnotation(Property.class).isKey()));
    }
    
    Map<String, Set<Field>> entityToFields = new HashMap<String, Set<Field>>();
    
    for (Map.Entry<String, Set<Field>> entry : typeToFields.entrySet()) {
      String typeName = entry.getKey();
      if (entities.contains(typeName)) {
        Set<Field> fields = typeToFields.get(typeName);
        String superTypeName = typeName;
        while ((superTypeName = typeToSuper.get(superTypeName)) != null
            && typeToFields.containsKey(superTypeName)) {
          fields.addAll(typeToFields.get(superTypeName));
        }
        entityToFields.put(typeName, fields);
      }
    }
    
    for (Map.Entry<String, Set<Field>> entry : entityToFields.entrySet()) {
      try {
        generateDao(entry.getKey(), entry.getValue());
      } catch (Exception e) {
        e.printStackTrace();
        System.err.printf("Unable to generate the DAO class for %s\n", entry.getKey());
      }
    }
    
    return true;
  }
  
  private String stripParametricType(String typeName) {
    return typeName.split("<")[0];
  }

  private void generateDao(String entityName, Set<Field> fields) throws IOException, TemplateException {
    System.out.println("-------- generate dao --------");
    System.out.println(entityName);
    for (Field field : fields) {
      System.out.printf("\t%s : %s %s\n", field.name, field.type, field.isKey ? "[key]" : "");
    }
    System.out.println("-------- ------------ --------");

    List<Field> properties = new ArrayList<Field>();
    List<Field> allProperties = new ArrayList<Field>();
    Field keyProperty = null;
    for (Field field : fields) {
      if (field.isKey) {
        keyProperty = field;
      } else {
        properties.add(field);
      }
      allProperties.add(field);
    }
    if (keyProperty == null) {
      System.err.println("Unable to find the key property of model " + entityName);
      System.exit(-1);
    }
    int lastDot = entityName.lastIndexOf(".");
    String packageName = entityName.substring(0, lastDot);
    entityName = entityName.substring(lastDot + 1);
    String daoName = entityName + "Dao";
    
    // Load and init template
    Configuration cfg = new Configuration();
    cfg.setClassForTemplateLoading(DataType.class, "");
    Template template = cfg.getTemplate("dao.ftl");
    
    Map<String, Object> input = new HashMap<String, Object>();
    input.put("key_property", keyProperty);
    input.put("properties", properties);
    input.put("all_properties", allProperties);
    input.put("entity_name", entityName);
    input.put("dao_name", daoName);
    input.put("package_name", packageName);

    // Use template to generate source file
    JavaFileObject jfo = processingEnv.getFiler().createSourceFile(
        packageName + "." + entityName + "Dao");
    Writer writer = jfo.openWriter();
    template.process(input, writer);
    writer.flush();
    writer.close();
  }
}
