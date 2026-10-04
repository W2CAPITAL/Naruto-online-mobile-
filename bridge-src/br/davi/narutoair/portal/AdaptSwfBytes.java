package br.davi.narutoair.portal;
import com.adobe.fre.*;
/** No network or account parameters: only bounded SWF data in/out. */
public final class AdaptSwfBytes implements FREFunction {
    public FREObject call(FREContext context,FREObject[] arguments) {
        try {
            if(arguments.length!=1 || !(arguments[0] instanceof FREByteArray))throw new IllegalArgumentException("Expected SWF ByteArray");
            FREByteArray source=(FREByteArray)arguments[0];byte[] bytes;
            source.acquire();
            try {
                long size=source.getLength();
                if(size<9 || size>32*1024*1024)throw new IllegalArgumentException("Client SWF size limit");
                bytes=new byte[(int)size];source.getBytes().duplicate().get(bytes);
            } finally {source.release();}
            EntryCompatibility.Result adapted=EntryCompatibility.adapt(bytes);
            FREByteArray target=source;
            if(adapted.references>0) {
                target=FREByteArray.newByteArray(adapted.bytes.length);target.acquire();
                try {target.getBytes().duplicate().put(adapted.bytes);}finally{target.release();}
            }
            FREObject result=FREObject.newObject("Object",new FREObject[0]);
            result.setProperty("bytes",target);
            result.setProperty("security",FREObject.newObject(adapted.securityReferences));
            result.setProperty("loaders",FREObject.newObject(adapted.loaderReferences));
            result.setProperty("resources",FREObject.newObject(adapted.resourceReferences));
            result.setProperty("page",FREObject.newObject(adapted.pageReferences));
            result.setProperty("sounds",FREObject.newObject(adapted.soundReferences));
            result.setProperty("sockets",FREObject.newObject(adapted.socketReferences));
            result.setProperty("menus",FREObject.newObject(adapted.menuGuards));
            return result;
        } catch(Exception ex) {
            try {
                FREObject error=FREObject.newObject("Object",new FREObject[0]);
                error.setProperty("error",FREObject.newObject("Client adaptation: "+ex.getClass().getSimpleName()+": "+ex.getMessage()));
                return error;
            } catch(Exception ignored) {return null;}
        }
    }
}
