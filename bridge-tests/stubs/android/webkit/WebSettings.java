package android.webkit;
public class WebSettings {public boolean js,dom;public void setJavaScriptEnabled(boolean v){js=v;}public void setDomStorageEnabled(boolean v){dom=v;}public int cacheMode=-1;public void setCacheMode(int value){cacheMode=value;}}
