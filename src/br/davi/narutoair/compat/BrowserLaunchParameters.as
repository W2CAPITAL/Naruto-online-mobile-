package br.davi.narutoair.compat {
    /** Reproduce URL parameters plus explicit HTML FlashVars for loadBytes. */
    public class BrowserLaunchParameters {
        public static function merge(url:String,flashvars:Object):Object {
            var result:Object={};
            var clean:String=(url || "").split("#")[0];
            var query:int=clean.indexOf("?");
            if(query>=0) {
                var pairs:Array=clean.substr(query+1).split("&");
                for each(var pair:String in pairs) {
                    if(!pair)continue;
                    var equal:int=pair.indexOf("=");
                    try {
                        var name:String=decodeURIComponent((equal<0?pair:pair.substr(0,equal)).replace(/\+/g," "));
                        var value:String=decodeURIComponent((equal<0?"":pair.substr(equal+1)).replace(/\+/g," "));
                        if(name && name!="__proto__" && name!="constructor" && name!="prototype")result[name]=value;
                    } catch(error:Error) { /* Skip only a malformed name/value pair. */ }
                }
            }
            if(flashvars)for(var key:String in flashvars) {
                if(key==="__proto__" || key==="constructor" || key==="prototype")continue;
                result[key]=flashvars[key]==null?"":String(flashvars[key]);
            }
            return result;
        }
    }
}
