package br.davi.narutoair.portal;
import com.adobe.fre.*;
/** Snapshot requested explicitly by the audio help button; no periodic UI traffic. */
public final class AudioDiagnostic {
 public static FREFunction function(){return new FREFunction(){public FREObject call(FREContext c,FREObject[] args){
  try {if(args.length==1 && "reset".equals(args[0].getAsString()))AudioResourceLog.clear();return FREObject.newObject(AudioResourceLog.snapshot());}
  catch(Exception e){return null;}
 }};}
}
