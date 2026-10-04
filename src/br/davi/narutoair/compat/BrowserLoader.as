package br.davi.narutoair.compat {
    import flash.display.Loader;
    import flash.system.LoaderContext;
    import flash.system.Security;
    import flash.system.ApplicationDomain;
    import flash.utils.ByteArray;
    import flash.utils.Dictionary;
    import flash.net.URLRequest;
    import flash.net.URLStream;
    import flash.net.URLVariables;
    import flash.events.Event;
    import flash.events.ProgressEvent;
    import flash.events.HTTPStatusEvent;
    import flash.events.IOErrorEvent;
    import flash.events.SecurityErrorEvent;
    import flash.utils.getQualifiedClassName;
    /** Used only by the explicitly imported official client's Loader references. */
    public class BrowserLoader extends Loader {
        private static var adapter:Function;
        private static var reporter:Function;
        private static var failure:Function;
        private static var sequence:uint=0;
        private static var clientDomain:ApplicationDomain;
        private static var transfers:Dictionary=new Dictionary(true);
        private static var clients:Dictionary=new Dictionary(true);
        private var stream:URLStream;
        private var buffer:ByteArray;
        private var pendingContext:LoaderContext;
        private var httpStatus:int=0;
        private var transferPath:String="";
        private var progressBucket:int=-1;
        private var clientImport:Boolean=false;
        private var resourceActive:Boolean=false;
        private var internalLoadBytes:Boolean=false;
        private var sourceUrl:String="bytes://client";
        private static const MAX_SWF_BYTES:uint=32*1024*1024;
        public function BrowserLoader() {
            super();
            clients[this]=true;
            contentLoaderInfo.addEventListener(Event.INIT,onClientInit,false,0,true);
            contentLoaderInfo.addEventListener(Event.COMPLETE,onClientComplete,false,0,true);
            contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR,onClientError,false,0,true);
            contentLoaderInfo.addEventListener(SecurityErrorEvent.SECURITY_ERROR,onClientError,false,0,true);
        }
        public static function configure(adapt:Function,report:Function,domain:ApplicationDomain,fail:Function=null,stage:flash.display.Stage=null):void {
            adapter=adapt;reporter=report;clientDomain=domain;failure=fail;sequence=0;
            ClientLoadScheduler.configure(stage);
        }
        public static function disable():void {
            ClientLoadScheduler.disable();
            var pending:Array=[];
            for(var key:Object in clients) pending.push(key);
            for each(var loader:BrowserLoader in pending) {try{loader.unloadAndStop(true);}catch(error:Error){loader.releaseStream();}}
            clients=new Dictionary(true);
            adapter=null;reporter=null;clientDomain=null;failure=null;
        }
        override public function load(request:URLRequest,context:LoaderContext=null):void {
            finishResource("CLOSE");
            releaseStream();
            clientImport=false;
            var resolved:URLRequest=BrowserResources.request(request);
            sourceUrl=resolved.url;
            BrowserResources.note("Loader","REQUEST",sourceUrl);resourceActive=true;
            if(!BrowserResources.isClientSwf(resolved.url)) {super.load(resolved,context);return;}
            if(adapter==null || Security.sandboxType!=Security.APPLICATION) throw new SecurityError("Official client loader adapter is inactive.");
            if(resolved.method!="GET") throw new ArgumentError("Client SWF requires GET.");
            resolved=BrowserResources.request(resolved,true);
            resolved.followRedirects=false;
            if(resolved.idleTimeout<=0) resolved.idleTimeout=30000;
            pendingContext=context || new LoaderContext();
            // loadBytes has no URL parameters; reproduce the original URL's name/value pairs.
            var query:int=resolved.url.indexOf("?");
            if(pendingContext.parameters==null && query>=0)pendingContext.parameters=BrowserLaunchParameters.merge(resolved.url,null);
            buffer=new ByteArray();httpStatus=0;progressBucket=-1;
            transferPath=resolved.url.replace(/^https?:\/\/[^/]+/,"").split(/[?#]/)[0];
            stream=new URLStream();transfers[this]=true;
            stream.addEventListener(ProgressEvent.PROGRESS,onProgress);
            stream.addEventListener(HTTPStatusEvent.HTTP_RESPONSE_STATUS,onResponseStatus);
            stream.addEventListener(Event.COMPLETE,onDownloaded);
            stream.addEventListener(IOErrorEvent.IO_ERROR,onTransferError);
            stream.addEventListener(SecurityErrorEvent.SECURITY_ERROR,onTransferError);
            if(reporter!=null)reporter("CLIENT URL 1.3.13: solicitando "+transferPath);
            ClientLoadScheduler.download(this,function():void {
                try {if(stream)stream.load(resolved);}catch(err:Error){transferFailed(err.message);}
            });
        }
        override public function loadBytes(bytes:ByteArray,context:LoaderContext=null):void {
            if(!internalLoadBytes)finishResource("CLOSE");
            releaseStream();
            clientImport=isSwf(bytes);
            if(!clientImport) {super.loadBytes(bytes,context);return;}
            if(adapter==null || Security.sandboxType!=Security.APPLICATION) throw new SecurityError("Official client loader adapter is inactive.");
            var result:Object=adapter(bytes);
            if(result==null || result.error || !(result.bytes is ByteArray)) {
                throw new Error(result && result.error ? String(result.error) : "Client SWF adapter returned invalid data.");
            }
            var importContext:LoaderContext=context || new LoaderContext();
            if(importContext.applicationDomain==null) importContext.applicationDomain=new ApplicationDomain(clientDomain);
            var promoted:Boolean=!importContext.allowCodeImport;
            importContext.allowCodeImport=true;
            sequence++;
            if(reporter!=null) reporter("CLIENT BYTES 1.3.13: carga="+sequence+"; bytes="+ByteArray(result.bytes).length+
                "; Security="+result.security+"; Loader="+result.loaders+"; Resources="+result.resources+"; Page="+result.page+"; Menu="+result.menus+"; Socket="+result.sockets+"; Sound="+result.sounds+
                "; allowCodeImport=true; contexto="+(promoted?"adaptado":"existente"));
            // Preserve the supplied applicationDomain, parameters and other options.
            super.loadBytes(ByteArray(result.bytes),importContext);
        }
        private function onClientInit(event:Event):void {
            GraphicsProfile.enforceTarget();
            if(clientImport && reporter!=null)reporter("CLIENT INIT 1.3.13: SWF executando; classe="+getQualifiedClassName(content)+"; bytes="+contentLoaderInfo.bytesTotal+"; SWF="+contentLoaderInfo.swfVersion+"; Stage="+(content && content.stage ? "SIM":"aguardando montagem do cliente"));
        }
        private function onClientComplete(event:Event):void {
            if(clientImport && reporter!=null)reporter("CLIENT COMPLETE 1.3.13: modulo SWF carregado.");
            finishResource("COMPLETE");
        }
        private function onClientError(event:Event):void {
            if(reporter!=null)reporter("CLIENT LOAD ERROR 1.3.13: "+event.toString());
            finishResource("ERROR");
        }
        private function onResponseStatus(event:HTTPStatusEvent):void {
            httpStatus=event.status;
            if(reporter!=null)reporter("CLIENT HTTP 1.3.13: "+event.status+" "+transferPath);
            if(stream && event.status!=200) transferFailed("HTTP "+event.status+" em "+transferPath);
        }
        private function drain():void {
            var available:uint=stream.bytesAvailable;
            if(available>MAX_SWF_BYTES-buffer.length) throw new Error("Client SWF exceeds 32 MB.");
            if(available)stream.readBytes(buffer,buffer.length,available);
        }
        private function onProgress(event:ProgressEvent):void {
            if(!stream) return;
            try {
                if(event.bytesTotal>MAX_SWF_BYTES)throw new Error("Client SWF exceeds 32 MB.");
                drain();
                if(event.bytesTotal>0) {
                    var bucket:int=int(event.bytesLoaded*5/event.bytesTotal);
                    if(bucket!=progressBucket) {
                        progressBucket=bucket;
                        if(reporter!=null)reporter("CLIENT URL 1.3.13: "+transferPath+"; download="+int(event.bytesLoaded*100/event.bytesTotal)+"%");
                    }
                }
                // LoaderInfo forbids synthetic dispatchEvent. Native decoding will
                // emit its own progress/INIT/COMPLETE after the download as data.
            }catch(err:Error){transferFailed(err.message);}
        }
        private function onDownloaded(event:Event):void {
            if(!stream) return;
            try {
                drain();
                if(httpStatus!=200 || !isSwf(buffer)) throw new Error("Invalid client SWF response: HTTP "+httpStatus);
                var bytes:ByteArray=buffer;var context:LoaderContext=pendingContext;
                bytes.position=0;
                releaseStream(false);
                // INIT/COMPLETE and decoding events are supplied by the native Loader.
                ClientLoadScheduler.downloaded(this,bytes.length,function():void {
                    internalLoadBytes=true;
                    try {loadBytes(bytes,context);}catch(err:Error){transferFailed(err.message);}
                    finally {internalLoadBytes=false;}
                });
            }catch(err:Error){transferFailed(err.message);}
        }
        private function onTransferError(event:Event):void {
            var message:String=event is IOErrorEvent ? IOErrorEvent(event).text : SecurityErrorEvent(event).text;
            transferFailed(message);
        }
        private function transferFailed(message:String):void {
            finishResource("ERROR");
            releaseStream();
            if(reporter!=null)reporter("CLIENT URL ERROR 1.3.13: "+message);
            // A failed client-code import returns to the portal with a diagnostic;
            // no fabricated COMPLETE and no attempt to dispatch on LoaderInfo.
            if(failure!=null)failure("Falha ao carregar modulo do jogo: "+message);
        }
        private function finishResource(phase:String):void {
            if(resourceActive && sourceUrl!="bytes://client") {
                resourceActive=false;
                BrowserResources.note("Loader",phase,sourceUrl);
            }
        }
        private function releaseStream(cancel:Boolean=true):void {
            if(cancel)ClientLoadScheduler.cancel(this);
            if(stream) {
                stream.removeEventListener(ProgressEvent.PROGRESS,onProgress);
                stream.removeEventListener(HTTPStatusEvent.HTTP_RESPONSE_STATUS,onResponseStatus);
                stream.removeEventListener(Event.COMPLETE,onDownloaded);
                stream.removeEventListener(IOErrorEvent.IO_ERROR,onTransferError);
                stream.removeEventListener(SecurityErrorEvent.SECURITY_ERROR,onTransferError);
                try{stream.close();}catch(err:Error){}
                stream=null;
            }
            buffer=null;pendingContext=null;delete transfers[this];
        }
        override public function close():void {finishResource("CLOSE");if(stream)releaseStream();else super.close();}
        override public function unload():void {finishResource("CLOSE");releaseStream();super.unload();}
        override public function unloadAndStop(gc:Boolean=true):void {finishResource("CLOSE");releaseStream();super.unloadAndStop(gc);}
        private static function isSwf(bytes:ByteArray):Boolean {
            return bytes!=null && bytes.length>=3 && (bytes[0]==70 || bytes[0]==67 || bytes[0]==90) && bytes[1]==87 && bytes[2]==83;
        }
    }
}
