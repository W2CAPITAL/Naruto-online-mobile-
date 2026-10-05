package br.davi.narutoair.compat {
    import flash.net.URLRequest;
    import flash.utils.getTimer;

    /** Resolve only the asset origin captured from the validated official entry. */
    public class BrowserResources {
        private static var officialRoot:String="";
        private static var runtimeRoot:String="";
        private static var folder:String="";
        private static var reporter:Function;
        private static var requests:uint=0,completed:uint=0,errors:uint=0,closed:uint=0,lines:uint=0;
        private static var pending:Array=[];
        private static var audioLines:Array=[];

        public static function configure(original:String,runtime:String):void {
            disable();
            var a:Array=/^(https:\/\/cdnnaruto-pt\.oasgames\.com)(\/PT_NarutoAlpha[0-9.]+Build[0-9]+\/)entry\.swf(?:\?[^#]*)?$/.exec(original);
            var b:Array=/^(http:\/\/127\.0\.0\.1:[0-9]{1,5}|https:\/\/cdnnaruto-pt\.oasgames\.com)(\/PT_NarutoAlpha[0-9.]+Build[0-9]+\/)entry\.swf(?:\?[^#]*)?$/.exec(runtime);
            if(!a || !b || a[2]!=b[2]) throw new Error("Base de recursos oficial invalida.");
            officialRoot=a[1];runtimeRoot=b[1];folder=a[2];
        }

        public static function disable():void {
            officialRoot="";runtimeRoot="";folder="";reporter=null;
            requests=0;completed=0;errors=0;closed=0;lines=0;pending=[];audioLines=[];
        }

        public static function resolve(value:String):String {
            if(!folder || !value || /[\x00-\x20\\]/.test(value)) return value;
            var path:String=value;
            // Protocol-relative CDN URLs have no browser document scheme in AIR.
            // Resolve only the exact validated CDN; foreign authorities stay intact.
            var cdnAuthority:String="//"+officialRoot.substr(8)+"/";
            if(path.indexOf(cdnAuthority)==0) path="https:"+path;
            if(path.indexOf(officialRoot+"/")==0) path=path.substr(officialRoot.length);
            else if(path.indexOf("http://"+officialRoot.substr(8)+"/")==0) path=path.substr(officialRoot.length-1);
            else if(path.indexOf(runtimeRoot+"/")==0) path=path.substr(runtimeRoot.length);
            // loadBytes gives the browser client an app:/ base in AIR. These
            // wrappers belong to the imported client, not to packaged app UI.
            else if(/^app:\/(?!\/)/i.test(path)) path=path.substr(5);
            else if(path.indexOf("//")==0 || /^[a-z][a-z0-9+.-]*:/i.test(path)) return value;
            // Normalize dot segments without changing query bytes or encoded filenames.
            var suffix:String="";
            var cut:int=path.search(/[?#]/);
            if(cut>=0) {suffix=path.substr(cut);path=path.substring(0,cut);}
            if(path.charAt(0)!="/") {
                if(/^PT_NarutoAlpha[0-9.]+Build[0-9]+\//.test(path)) path="/"+path;
                else path=folder+path;
            }
            var segments:Array=path.split("/");var normalized:Array=[];
            for each(var part:String in segments) {
                if(part=="." || part=="") continue;
                if(part=="..") {if(normalized.length==0)return value;normalized.pop();}
                else normalized.push(part);
            }
            path="/"+normalized.join("/");
            return runtimeRoot+path+suffix;
        }

        public static function isClientSwf(url:String):Boolean {
            if(!folder || url.indexOf(runtimeRoot+"/")!=0) return false;
            var path:String=url.substr(runtimeRoot.length).split(/[?#]/)[0];
            if(/%2e|%2f|%5c/i.test(path)) return false;
            if(/(?:^|\/)\.\.?\//.test(path)) return false;
            // The version table can select older builds for individual modules.
            return /^\/(?:PT_NarutoAlpha[0-9.]+Build[0-9]+\/|flash\/)\S+\.swf$/i.test(path);
        }

        public static function request(source:URLRequest,forceCopy:Boolean=false):URLRequest {
            if(source==null) throw new ArgumentError("URLRequest ausente.");
            var url:String=resolve(source.url);
            if(url==source.url && !forceCopy) return source;
            var copy:URLRequest=new URLRequest(url);
            copy.method=source.method;copy.data=source.data;copy.contentType=source.contentType;
            copy.requestHeaders=source.requestHeaders ? source.requestHeaders.concat() : [];copy.digest=source.digest;
            copy.authenticate=source.authenticate;copy.cacheResponse=source.cacheResponse;
            copy.followRedirects=source.followRedirects;copy.idleTimeout=source.idleTimeout;
            copy.manageCookies=source.manageCookies;copy.useCache=source.useCache;copy.userAgent=source.userAgent;
            return copy;
        }

        public static function setReporter(value:Function):void {reporter=value;}

        private static function safePath(url:String):String {
            return (url || "").split(/[?#]/)[0].replace(/^(?:[a-z][a-z0-9+.-]*:)?\/\/[^\/]*/i,"").substr(0,220);
        }

        private static function addPending(kind:String,url:String):void {
            pending.push({kind:kind,path:safePath(url),started:getTimer()});
        }

        private static function removePending(kind:String,url:String):Boolean {
            var path:String=safePath(url);
            for(var i:int=0;i<pending.length;i++) {
                var item:Object=pending[i];
                if(item.kind==kind && item.path==path){pending.splice(i,1);return true;}
            }
            return false;
        }

        public static function note(kind:String,phase:String,url:String,status:int=0):void {
            if(kind=="Sound") {
                if(audioLines.length>=64)audioLines.shift();
                audioLines.push(phase+" "+safePath(url)+(status?"; code="+status:""));
            }
            if(reporter==null)return;
            var path:String=safePath(url);
            if(phase=="REQUEST"){requests++;addPending(kind,url);}
            else if(phase=="COMPLETE"){completed++;removePending(kind,url);}
            else if(phase=="ERROR"){errors++;removePending(kind,url);}
            else if(phase=="CLOSE" || phase=="CANCEL"){closed++;removePending(kind,url);}
            if(reporter==null)return;
            // Never log a query, credentials, request body or response data.
            if(lines<180 || ((phase=="ERROR" || phase=="CLOSE") && errors+closed<=24)) {
                lines++;
                reporter("RESOURCE 1.3.13: "+kind+" "+phase+" "+path+(status?"; code="+status:""));
            }
        }

        public static function audioSummary():String {
            return audioLines.length ? audioLines.join("\n") : "Nenhuma chamada de Sound externo capturada pelo adaptador.";
        }

        public static function summary():String {
            return "pedidos="+requests+"; concluidos="+completed+"; erros="+errors+"; fechados="+closed+"; pendentes="+pending.length;
        }

        public static function pendingSummary():String {
            if(pending.length==0)return "pendingResource=nenhum";
            var now:int=getTimer();var oldest:Object=pending[0];
            for each(var item:Object in pending)if(int(item.started)<int(oldest.started))oldest=item;
            return "pendingResource="+oldest.kind+":"+oldest.path+"; pendingMs="+Math.max(0,now-int(oldest.started));
        }
    }
}
