package br.davi.narutoair.portal
{
   import flash.events.EventDispatcher;
   import flash.events.StatusEvent;
   import flash.external.ExtensionContext;
   import flash.utils.ByteArray;
   
   public final class PortalMarker extends EventDispatcher
   {
      
      public static const VERSION:String = "1.3.16";
      
      private var context:ExtensionContext;
      
      public function PortalMarker()
      {
         super();
         context = ExtensionContext.createExtensionContext("br.davi.narutoair.portal",null);
         if(!context)
         {
            throw new Error("ExtensionContext retornou null para br.davi.narutoair.portal");
         }
         context.addEventListener("status",onNativeStatus);
      }
      
      public function ping() : Object
      {
         return context.call("ping");
      }
      
      public function status() : Object
      {
         return context.call("status");
      }
      
      public function openPortal(url:String) : Object
      {
         return context.call("open",url);
      }
      
      public function hide() : Object
      {
         return context.call("hide");
      }
      
      public function show() : Object
      {
         return context.call("show");
      }
      
      public function close() : Object
      {
         return context.call("close");
      }
      
      public function report(message:String) : Object
      {
         return context.call("report",message);
      }
      
      public function adaptSwfBytes(bytes:ByteArray):Object { return context.call("adaptSwfBytes",bytes); }
      public function browserConfigure(nonce:String):Object {return context.call("browserConfigure",nonce);}
      public function browserEval(script:String):Object {return context.call("browserEval",script);}

      public function input(action:String,x:String="0",y:String="0",key:String="0"):Object {return context.call("mobileInput",action,x,y,key);}
      public function portalHealth():Object {return context.call("portalHealth");}
      public function resetAccount():Object {return context.call("resetAccount");}
      public function recharge(uid:String,server:String):Object {return context.call("recharge",uid,server);}
      public function graphicsScale(percent:int):Object {return context.call("graphicsScale",String(percent));}
      public function graphicsFPS(choice:Number):Object {return context.call("graphicsFPS",String(choice));}
      public function graphicsFSR(enabled:Boolean,epoch:int):Object {return context.call("graphicsFSR",String(enabled),String(epoch));}
      public function kaguyaMode(enabled:Boolean,style:int):Object {return context.call("kaguyaMode",String(enabled),String(style));}
      public function audioDiagnostic(action:String="snapshot"):Object {return context.call("audioDiagnostic",action);}

      public function dispose() : void
      {
         if(!context)
         {
            return;
         }
         try
         {
            context.removeEventListener("status",onNativeStatus);
         }
         catch(e:Error)
         {
         }
         try
         {
            context.dispose();
         }
         catch(e2:Error)
         {
         }
         context = null;
      }
      
      private function onNativeStatus(e:StatusEvent) : void
      {
         dispatchEvent(new StatusEvent("status",false,false,e.code,e.level));
      }
   }
}
