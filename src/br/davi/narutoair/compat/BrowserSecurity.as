package br.davi.narutoair.compat {
    import flash.system.Security;
    /** Browser compatibility for the explicitly imported official entry only. */
    public final class BrowserSecurity {
        public static const APPLICATION:String = Security.APPLICATION;
        public static const LOCAL_TRUSTED:String = Security.LOCAL_TRUSTED;
        public static const LOCAL_WITH_FILE:String = Security.LOCAL_WITH_FILE;
        public static const LOCAL_WITH_NETWORK:String = Security.LOCAL_WITH_NETWORK;
        public static const REMOTE:String = Security.REMOTE;
        private static var reporter:Function;
        private static var reported:Object = {};
        public static function configure(callback:Function):void { reporter=callback;reported={}; }
        public static function allowDomain(...domains):void { browserPermission("allowDomain"); }
        public static function allowInsecureDomain(...domains):void { browserPermission("allowInsecureDomain"); }
        private static function browserPermission(method:String):void {
            // The application sandbox cannot grant access to remote sandboxes.
            // Leaving that browser grant absent preserves AIR's isolation.
            if(Security.sandboxType != Security.APPLICATION) throw new SecurityError("Adapter requires AIR application sandbox.");
            if(!reported[method]) {
                reported[method]=true;
                if(reporter!=null) reporter("AIR COMPAT 1.3.13: Security."+method+" do navegador omitido no sandbox application.");
            }
        }
        public static function get sandboxType():String { return Security.sandboxType; }
        public static function get pageDomain():String { return Security.pageDomain; }
        public static function get exactSettings():Boolean { return Security.exactSettings; }
        public static function set exactSettings(value:Boolean):void { Security.exactSettings=value; }
        public static function loadPolicyFile(url:String):void { Security.loadPolicyFile(url); }
        public static function showSettings(panel:String="default"):void { Security.showSettings(panel); }
    }
}
