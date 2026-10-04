package br.davi.narutoair.portal;
import com.adobe.fre.*;
public final class PortalExtension implements FREExtension {
    public FREContext createContext(String type) {return new MobilePortalContext();}
    public void initialize() {}
    public void dispose() {}
}
