package android.webkit;
public class WebStorage {public boolean cleared;private static final WebStorage INSTANCE=new WebStorage();public static WebStorage getInstance(){return INSTANCE;}public void deleteAllData(){cleared=true;}}
