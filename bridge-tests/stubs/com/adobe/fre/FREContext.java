package com.adobe.fre;
import java.util.Map;
public abstract class FREContext {
 public String eventCode,eventLevel;
 public void dispatchStatusEventAsync(String code,String level){eventCode=code;eventLevel=level;}
 public abstract Map<String,FREFunction> getFunctions();
}
