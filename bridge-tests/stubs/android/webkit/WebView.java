package android.webkit;
public class WebView extends android.view.View {
 public String script,url;public boolean stopped,destroyed;public int reloads;public void reload(){reloads++;}public WebViewClient client;public WebSettings settings=new WebSettings();
 public WebView(){}public WebView(android.content.Context c){}public void stopLoading(){stopped=true;}public void loadUrl(String value){url=value;}public void destroy(){destroyed=true;}public WebSettings getSettings(){return settings;}public void setWebViewClient(WebViewClient c){client=c;}
 public ValueCallback<String> reply;public String getUrl(){return url;}
 public void evaluateJavascript(String value,ValueCallback<String> callback){script=value;if(callback!=null)reply=callback;}
}
