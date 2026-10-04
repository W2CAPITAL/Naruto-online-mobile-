package br.davi.narutoair.compat {
    import flash.media.Sound;
    import flash.media.SoundChannel;
    import flash.media.SoundLoaderContext;
    import flash.media.SoundTransform;
    import flash.net.URLRequest;
    import flash.events.Event;
    import flash.events.IOErrorEvent;
    import flash.events.SecurityErrorEvent;
    import flash.utils.Dictionary;

    /** Native Sound subclass: retain decoding, embedded symbols, loops and events.
     * Only external asset URLs pass through the same resolver as the client.
     */
    public class BrowserSound extends Sound {
        private static var loading:Dictionary=new Dictionary();
        private static var channels:Dictionary=new Dictionary(true);
        private var requestUrl:String="";
        private var active:Boolean=false;

        public function BrowserSound(stream:URLRequest=null,context:SoundLoaderContext=null) {
            super();
            addEventListener(Event.COMPLETE,onComplete,false,0,true);
            addEventListener(IOErrorEvent.IO_ERROR,onError,false,0,true);
            addEventListener(SecurityErrorEvent.SECURITY_ERROR,onError,false,0,true);
            if(stream!=null)load(stream,context);
        }
        override public function load(stream:URLRequest,context:SoundLoaderContext=null):void {
            var resolved:URLRequest=BrowserResources.request(stream);
            // Sound can only be loaded once; failed repeat calls must not erase
            // the tracking of the first native transfer.
            super.load(resolved,context);
            requestUrl=resolved.url;active=true;loading[this]=true;
            BrowserResources.note("Sound","REQUEST",requestUrl);
        }
        private function onComplete(event:Event):void {finish("COMPLETE");}
        private function onError(event:Event):void {finish("ERROR");}
        private function finish(phase:String):void {
            if(!active)return;
            active=false;delete loading[this];BrowserResources.note("Sound",phase,requestUrl);
        }
        override public function close():void {
            super.close();finish("CLOSE");
        }
        override public function play(startTime:Number=0,loops:int=0,sndTransform:SoundTransform=null):SoundChannel {
            var channel:SoundChannel=super.play(startTime,loops,sndTransform);
            if(channel!=null) {
                channels[channel]=true;
                channel.addEventListener(Event.SOUND_COMPLETE,channelComplete,false,0,true);
            }
            return channel;
        }
        private static function channelComplete(event:Event):void {
            var channel:SoundChannel=event.currentTarget as SoundChannel;
            if(channel){channel.removeEventListener(Event.SOUND_COMPLETE,channelComplete);delete channels[channel];}
        }
        public static function disable():void {
            var pending:Array=[];
            for(var sound:Object in loading)pending.push(sound);
            for each(var item:BrowserSound in pending) {
                try{item.close();}catch(error:Error){item.finish("CANCEL");}
            }
            loading=new Dictionary();
            for(var playing:Object in channels) {
                SoundChannel(playing).removeEventListener(Event.SOUND_COMPLETE,channelComplete);
                SoundChannel(playing).stop();
            }
            channels=new Dictionary(true);
        }
    }
}
