package ${package_name};
 
import java.util.ArrayList;
import java.util.List;
 
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
 
public class ${dao_name} extends SQLiteOpenHelper {
 
  private static final int DATABASE_VERSION = 1;
  
  private static final String DATABASE_NAME = "${package_name}";

  private static final String TABLE_NAME = "${entity_name}";
 
  public ${dao_name}(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }
 
  @Override
  public void onCreate(SQLiteDatabase db) {
    String CREATE_ENTITY_TABLE = "CREATE TABLE " + TABLE_NAME + "("
        + "${key_property.name} ${key_property.type} PRIMARY KEY"
        <#list properties as property>
        + ", ${property.name} ${property.type}"
        </#list>
        + ")";
    db.execSQL(CREATE_ENTITY_TABLE);
  }
 
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    onCreate(db);
  }

  public void create(${entity_name} entity) {
    SQLiteDatabase db = this.getWritableDatabase();

    ContentValues values = new ContentValues();
    <#list all_properties as property>
    <#if property.type.nullable >
    if (entity.get${property.capName}() != null)
    </#if>
    values.put("${property.name}", entity.get${property.capName}());
    </#list>

    db.insert(TABLE_NAME, null, values);
    db.close();
  }

  public ${entity_name} get(${key_property.className} key) {
    SQLiteDatabase db = this.getReadableDatabase();

    Cursor cursor = db.query(TABLE_NAME, new String[] {
        "${key_property.name}"
        <#list properties as property>
        , "${property.name}"
        </#list>}, "${key_property.name} = ?", new String[] { String.valueOf(key) }, 
        null, null, null, null);
    
    if (!cursor.moveToFirst()) {
      return null;
    }
 
    int i = 0;
    ${entity_name} entity = new ${entity_name}(cursor.get${key_property.classNameForMethod}(i++));
    <#list properties as property>
    entity.set${property.capName}(cursor.get${property.classNameForMethod}(i++));
    </#list>

    return entity;
  }

  public List<${entity_name}> getAll() {
    List<${entity_name}> list = new ArrayList<${entity_name}>();
    String selectQuery = "SELECT ${key_property.name}"
        <#list properties as property>
        + ", ${property.name}"
        </#list>
        + " FROM " + TABLE_NAME;

    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(selectQuery, null);

    if (cursor.moveToFirst()) {
      do {
        int i = 0;
        ${entity_name} entity = new ${entity_name}(cursor.get${key_property.classNameForMethod}(i++));
        <#list properties as property>
        entity.set${property.capName}(cursor.get${property.classNameForMethod}(i++));
        </#list>
        list.add(entity);
      } while (cursor.moveToNext());
    }
 
    return list;
  }
 
  public int update(${entity_name} entity) {
    SQLiteDatabase db = this.getWritableDatabase();
 
    ContentValues values = new ContentValues();
    <#list properties as property>
    values.put("${property.name}", entity.get${property.capName}());
    </#list>

    return db.update(TABLE_NAME, values, "${key_property.name} = ?",
        new String[] { String.valueOf(entity.get${key_property.capName}()) });
  }

  public void delete(${entity_name} entity) {
    delete(entity.get${key_property.capName}());
  }
  
  public void delete(${key_property.className} key) {
    SQLiteDatabase db = this.getWritableDatabase();
    db.delete(TABLE_NAME, "${key_property.name} = ?",
        new String[] { String.valueOf(key) });
    db.close();
  }
 
  public int count() {
    String countQuery = "SELECT * FROM " + TABLE_NAME;
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(countQuery, null);
    cursor.close();
    return cursor.getCount();
  }
}
