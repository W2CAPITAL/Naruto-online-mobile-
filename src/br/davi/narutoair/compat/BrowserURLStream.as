package br.davi.narutoair.compat {
    import flash.net.URLStream;
    import flash.net.URLRequest;
    import flash.events.Event;
    import flash.events.IOErrorEvent;
    import flash.events.SecurityErrorEvent;
    import flash.events.HTTPStatusEvent;
    import flash.utils.Dictionary;
    public class BrowserURLStream extends URLStream {
        private var requestUrl:String="";
        private var active:Boolean=false;
        private static var pending:Dictionary=new Dictionary(true);
        public static function disable():void {var list:Array=[];for(var key:Object in pending)list.push(key);for each(var stream:BrowserURLStream in list){try{stream.close();}catch(e:Error){}}pending=new Dictionary(true);}
        public function BrowserURLStream() {
            super();
            addEventListener(Event.COMPLETE,onComplete,false,0,true);
            addEventListener(IOErrorEvent.IO_ERROR,onError,false,0,true);
            addEventListener(SecurityErrorEvent.SECURITY_ERROR,onError,false,0,true);
            addEventListener(HTTPStatusEvent.HTTP_STATUS,onStatus,false,0,true);
        }
        override public function load(request:URLRequest):void {
            if(active){active=false;BrowserResources.note("URLStream","CLOSE",requestUrl);}
            var resolved:URLRequest=BrowserResources.request(request);
            requestUrl=resolved.url;active=true;
            pending[this]=true;
            BrowserResources.note("URLStream","REQUEST",requestUrl);
            try {super.load(resolved);}catch(error:Error){active=false;delete pending[this];BrowserResources.note("URLStream","ERROR",requestUrl,error.errorID);throw error;}
        }
        private function onComplete(event:Event):void {delete pending[this];if(active){active=false;BrowserResources.note("URLStream","COMPLETE",requestUrl);}}
        private function onError(event:Event):void {delete pending[this];if(active){active=false;BrowserResources.note("URLStream","ERROR",requestUrl);}}
        private function onStatus(event:HTTPStatusEvent):void {BrowserResources.note("URLStream","HTTP",requestUrl,event.status);}
        override public function close():void {delete pending[this];if(active){active=false;BrowserResources.note("URLStream","CLOSE",requestUrl);}super.close();}
    }
}
