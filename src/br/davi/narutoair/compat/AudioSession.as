package br.davi.narutoair.compat {
    import flash.media.AudioPlaybackMode;
    import flash.media.Sound;
    import flash.media.SoundMixer;
    import flash.media.SoundTransform;
    import flash.net.SharedObject;
    import flash.utils.ByteArray;

    /** App volume only; client sound categories and Android media volume remain independent. */
    public final class AudioSession {
        private static var prefs:SharedObject;
        public static var enabled:Boolean=true;
        public static function initialize():void {
            try {prefs=SharedObject.getLocal("narutoAudio");if(prefs.data.enabled!==undefined)enabled=Boolean(prefs.data.enabled);}catch(error:Error){}
            start();
        }
        public static function start():void {
            SoundMixer.audioPlaybackMode=AudioPlaybackMode.MEDIA;
            SoundMixer.soundTransform=new SoundTransform(enabled?1:0);
        }
        public static function toggle():void {
            enabled=!enabled;
            SoundMixer.soundTransform=new SoundTransform(enabled?1:0);
            try {if(prefs){prefs.data.enabled=enabled;prefs.flush();}}catch(error:Error){}
        }
        public static function stop():void {
            BrowserSound.disable();
            // Includes timeline sounds and native channels from other embedded
            // sound base classes. Prevent music surviving account/server changes.
            SoundMixer.stopAll();
        }
        public static function test():Boolean {
            if(!enabled)return false;
            // Short, quiet native PCM tone; no network, mod or per-frame work.
            var pcm:ByteArray=new ByteArray();
            for(var i:int=0;i<11025;i++) {
                var envelope:Number=Math.min(1,i/441,(11024-i)/441);
                var sample:Number=Math.sin(i*2*Math.PI*660/44100)*.12*envelope;
                pcm.writeFloat(sample);pcm.writeFloat(sample);
            }
            pcm.position=0;
            var sound:Sound=new Sound();
            sound.loadPCMFromByteArray(pcm,11025,"float",true,44100);
            return sound.play()!=null;
        }
    }
}
