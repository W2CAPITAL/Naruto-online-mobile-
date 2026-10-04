package com.adobe.fre;
import java.util.*;
/** Test-only FRE lifetime model. Never packaged. */
public class FREObject {
    public Object value;
    public static int acquired;
    public FREObject(Object value){this.value=value;}
    private static FREObject create(Object value){if(acquired!=0)throw new IllegalStateException("AS allocation while ByteArray acquired");return new FREObject(value);}
    public static FREObject newObject(int value){return create(value);}
    public static FREObject newObject(boolean value){return create(value);}
    public String getAsString(){return (String)value;}
    public static FREObject newObject(String value){return create(value);}
    public static FREObject newObject(String type,FREObject[] args){if(!type.equals("Object") || args.length!=0)throw new IllegalArgumentException();return create(new HashMap<String,FREObject>());}
    @SuppressWarnings("unchecked") public void setProperty(String name,FREObject value){if(acquired!=0)throw new IllegalStateException("AS property while acquired");((Map<String,FREObject>)this.value).put(name,value);}
    @SuppressWarnings("unchecked") public FREObject getProperty(String name){return ((Map<String,FREObject>)this.value).get(name);}
}
