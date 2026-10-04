package {
    import flash.display.Sprite;
    import flash.media.Sound;
    import flash.media.SoundLoaderContext;
    import flash.media.SoundChannel;
    import flash.media.SoundTransform;
    import flash.net.URLRequest;
    import flash.system.Security;
    import flash.system.SecurityDomain;
    import flash.net.LocalConnection;
    import flash.net.Socket;
    import flash.net.URLLoader;
    import flash.net.URLStream;
    import flash.display.Loader;
    import flash.system.LoaderContext;
    import flash.utils.ByteArray;
    import flash.external.ExternalInterface;
    public class BrowserEntry extends Sprite {
        public var domain:SecurityDomain;
        public function audioFixture():SoundChannel {
            var sound:Sound=new Sound(new URLRequest("assets/sound/music.mp3"),new SoundLoaderContext(3000,false));
            return sound.play(25,2,new SoundTransform(.6,-.2));
        }
        public var marker:String="flash.system::Security";
        public function BrowserEntry() {
            var web:Boolean=ExternalInterface.available;
            if(web)ExternalInterface.call("getUin");
            Security.allowDomain("*");
            Security.allowInsecureDomain("*");
            Security.loadPolicyFile("https://example.invalid/crossdomain.xml");
            var exact:Boolean=Security.exactSettings;
            Security.exactSettings=exact;
            var sandbox:String=Security.sandboxType;
            domain=SecurityDomain.currentDomain;
            new LocalConnection().allowDomain("*");
            var loading:Loader=new Loader();
            var options:LoaderContext=new LoaderContext();
            loading.loadBytes(new ByteArray(),options);
            var socket:Socket=new Socket();
            var data:URLLoader=new URLLoader();
            var stream:URLStream=new URLStream();
            graphics.beginFill(0x55aa55);graphics.drawRect(0,0,100,100);
        }
    }
}
