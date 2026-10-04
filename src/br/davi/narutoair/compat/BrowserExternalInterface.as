package br.davi.narutoair.compat {
    /** Client-only page adapter; native AIR ExternalInterface remains unchanged. */
    public final class BrowserExternalInterface {
        private static var state:Object;
        private static var evaluate:Function;
        private static var configureNative:Function;
        private static var callbacks:Object={};
        public static var marshallExceptions:Boolean=false;
        private static var report:Function;
        private static var sessionReadLogged:Boolean=false;
        public static function configure(value:Object,send:Function,configure:Function,reporter:Function=null):void {
            disable();
            if(!value || !value.ready || !/^\d{1,10}$/.test(String(value.uin)) || Number(value.uin)<=0 ||
               Number(value.uin)>4294967295 || !/^https?:\/\/naruto-pt\.oasgames\.com$/.test(String(value.origin)) ||
               !/^[A-Za-z0-9:.]{1,128}$/.test(String(value.nonce))) throw new Error("A pagina oficial nao forneceu a identidade da sessao para a ponte do jogo.");
            if(!value.sessionCookies || !(value.sessionCookies.skey is String) ||
               value.sessionCookies.skey.length==0 || value.sessionCookies.skey.length>16384)
                throw new Error("A pagina oficial ainda nao forneceu a chave da sessao. Reabra o servidor pelo portal.");
            if(configure(String(value.nonce))!==true)throw new Error("Ponte da pagina oficial indisponivel.");
            state=value;evaluate=send;configureNative=configure;report=reporter;sessionReadLogged=false;
        }
        public static function disable():void {
            if(configureNative!=null)configureNative("");
            state=null;evaluate=null;configureNative=null;callbacks={};report=null;sessionReadLogged=false;
        }
        public static function get available():Boolean {return state!=null && evaluate!=null;}
        public static function get objectID():String {return available?String(state.objectID || ""):null;}
        private static function send(action:String,name:String,args:Array,remove:Boolean=false,returnValue:*=undefined):void {
            if(!available)throw new Error("Ponte da pagina nao inicializada.");
            if(!/^[A-Za-z_][A-Za-z0-9_]{0,63}$/.test(name) || /^(?:constructor|prototype|__proto__)$/.test(name))
                throw new ArgumentError("Nome de funcao da pagina invalido.");
            var packet:String=JSON.stringify({type:"naruto-air-browser",nonce:state.nonce,action:action,name:name,args:args,remove:remove,returnValue:returnValue});
            var script:String="(function(){var p="+packet+";try{if(window.__naBrowserDispatch && window.__naBrowserDispatch(p)===true)return;}catch(e){}var frames=document.querySelectorAll('iframe');for(var i=0;i<frames.length;i++){try{var url=new URL(frames[i].src,location.href);if((url.protocol==='https:' || url.protocol==='http:') && url.hostname==='naruto-pt.oasgames.com' && frames[i].contentWindow)frames[i].contentWindow.postMessage(p,"+JSON.stringify(String(state.origin))+");}catch(e){}}})();";
            if(evaluate(script)!==true)throw new Error("A pagina oficial recusou a chamada do cliente.");
        }
        public static function call(name:String,...args):* {
            if(!available)throw new Error("Ponte da pagina nao inicializada.");
            if(name=="getUin" && args.length==0)return String(state.uin);
            if(name=="getCookie" && args.length==1 && (args[0]=="uin" || args[0]=="skey"))return sessionCookie(String(args[0]));
            if(name=="eval" && args.length==1 && args[0]=="navigator.userAgent")return String(state.userAgent);
            // Pure accessors used by the official page can be returned synchronously
            // without blocking the AIR thread on the Android WebView.
            var expression:String=name=="eval" && args.length==1 ? String(args[0]) : args.length==0 ? name : "";
            var wrapped:Array=/^\s*function\s*\(\s*\)\s*\{\s*return\s+([^{};]+);?\s*\}\s*$/.exec(expression);
            if(wrapped)expression=String(wrapped[1]);
            if(/^\s*(?:window\.)?getUin\(\s*\)\s*;?\s*$/.test(expression))return String(state.uin);
            var accessor:Array=/^\s*(?:window\.)?getCookie\(\s*(['"])(uin|skey)\1\s*\)\s*;?\s*$/.exec(expression);
            if(accessor)return sessionCookie(String(accessor[2]));
            send("call",name,args);return null;
        }
        private static function sessionCookie(key:String):* {
            if(key=="skey" && !sessionReadLogged) {
                sessionReadLogged=true;
                if(report!=null)report("PAGE 1.3.13: chave de sessao real fornecida ao cliente; valor omitido.");
            }
            return state.sessionCookies[key];
        }
        public static function addCallback(name:String,closure:Function):void {
            // The original Application.flashDetect is a constant Boolean getter.
            // Supply its actual registered result to the page's synchronous probe.
            var reply:*=name=="flashDetect" && closure!=null ? closure() : undefined;
            send("callback",name,[],closure==null,reply);
            if(closure==null)delete callbacks[name];else callbacks[name]=closure;
        }
        public static function receive(json:String):void {
            if(!available || json.length>65536)return;
            var event:Object=JSON.parse(json);
            if(!(event.args is Array) || !callbacks.hasOwnProperty(event.name))return;
            var closure:Function=callbacks[event.name] as Function;
            if(closure!=null)closure.apply(null,event.args);
        }
    }
}
