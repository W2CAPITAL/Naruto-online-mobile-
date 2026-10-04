package br.davi.narutoair.compat {
    import flash.net.Socket;
    import flash.events.Event;
    import flash.events.IOErrorEvent;
    import flash.events.SecurityErrorEvent;
    import flash.events.ProgressEvent;
    import flash.events.OutputProgressEvent;
    import flash.events.TimerEvent;
    import flash.utils.Timer;
    import flash.utils.Dictionary;
    import flash.utils.ByteArray;
    import flash.utils.getTimer;

    /**
     * Native TCP socket compatibility wrapper with non-invasive diagnostics.
     * It never changes host, port, protocol bytes, endian, objectEncoding or timeout.
     * The only transport portability behavior retained from 1.3.1 is flushing bytes
     * already pending in AIR's native write buffer once per frame-sized tick.
     */
    public class BrowserSocket extends Socket {
        private static var report:Function;
        private static var sockets:Dictionary=new Dictionary();
        private static var ticker:Timer;

        private static var requests:uint=0;
        private static var connections:uint=0;
        private static var receivedEvents:uint=0;
        private static var errors:uint=0;
        private static var writeCalls:uint=0;
        private static var bytesQueued:Number=0;
        private static var flushCalls:uint=0;
        private static var autoFlushCalls:uint=0;
        private static var outputEvents:uint=0;
        private static var bytesTransported:Number=0;
        private static var readCalls:uint=0;
        private static var bytesRead:Number=0;
        private static var bytesReceived:Number=0;
        private static var lastWriteAt:int=-1;
        private static var lastReadAt:int=-1;
        private static var lastRxAt:int=-1;
        private static var firstWrite:String="";

        private var id:uint;
        private var waiting:Boolean=false;
        private var started:int;
        private var connectedAt:int=-1;
        private var warned:Boolean=false;
        private var dataLogged:Boolean=false;
        private var outputLogged:Boolean=false;
        private var autoFlushLogged:Boolean=false;
        private var explicitFlushLogged:Boolean=false;
        private var firstWriteLogged:Boolean=false;
        private var lastOutputSent:Number=0;

        public function BrowserSocket(host:String=null,port:int=0) {
            super();
            addEventListener(Event.CONNECT,onConnect,false,100);
            addEventListener(Event.CLOSE,onClose,false,100);
            addEventListener(ProgressEvent.SOCKET_DATA,onData,false,100);
            addEventListener(OutputProgressEvent.OUTPUT_PROGRESS,onOutputProgress,false,100);
            addEventListener(IOErrorEvent.IO_ERROR,onError,false,100);
            addEventListener(SecurityErrorEvent.SECURITY_ERROR,onError,false,100);
            if(host!=null)connect(host,port);
        }

        public static function configure(reporter:Function):void {
            disable();
            report=reporter;
            requests=0;connections=0;receivedEvents=0;errors=0;
            writeCalls=0;bytesQueued=0;flushCalls=0;autoFlushCalls=0;
            outputEvents=0;bytesTransported=0;readCalls=0;bytesRead=0;bytesReceived=0;
            lastWriteAt=-1;lastReadAt=-1;lastRxAt=-1;firstWrite="";
        }

        public static function disable():void {
            if(ticker){ticker.stop();ticker.removeEventListener(TimerEvent.TIMER,pump);ticker=null;}
            var old:Array=[];for(var key:Object in sockets)old.push(key);
            for each(var socket:BrowserSocket in old){try{socket.close();}catch(e:Error){}}
            sockets=new Dictionary();report=null;
        }

        public static function summary():String {
            var now:int=getTimer();
            return "socketPedidos="+requests+
                "; socketConexoes="+connections+
                "; socketWrites="+writeCalls+
                "; socketBytesEnviados="+uint(bytesQueued)+
                "; socketFlushes="+(flushCalls+autoFlushCalls)+
                "; socketAutoFlushes="+autoFlushCalls+
                "; socketOutputEventos="+outputEvents+
                "; socketBytesTransportados="+uint(bytesTransported)+
                "; socketDados="+receivedEvents+
                "; socketBytesRecebidos="+uint(bytesReceived)+
                "; socketReads="+readCalls+
                "; socketBytesLidos="+uint(bytesRead)+
                "; socketErros="+errors+
                "; ultimoWrite="+age(now,lastWriteAt)+
                "; ultimoRead="+age(now,lastReadAt)+
                "; ultimoRx="+age(now,lastRxAt)+
                "; socketEstado="+state();
        }

        public static function detail():String {
            var pending:uint=0;var connectedCount:uint=0;var oldestConnected:int=-1;var now:int=getTimer();
            for(var key:Object in sockets){
                var socket:BrowserSocket=key as BrowserSocket;
                if(!socket)continue;
                try{pending+=socket.bytesPending;}catch(e:Error){}
                if(socket.connected){connectedCount++;if(socket.connectedAt>=0 && (oldestConnected<0 || socket.connectedAt<oldestConnected))oldestConnected=socket.connectedAt;}
            }
            return "socketAtivos="+connectedCount+
                "; socketPendingBytes="+pending+
                "; conectadoHa="+age(now,oldestConnected)+
                "; primeiroWrite="+(firstWrite?firstWrite:"nenhum");
        }

        private static function age(now:int,value:int):String {
            return value<0 ? "nunca" : String(Math.max(0,now-value))+"ms";
        }

        private static function state():String {
            var anyConnected:Boolean=false;var pending:uint=0;var anyWaiting:Boolean=false;
            for(var key:Object in sockets){
                var socket:BrowserSocket=key as BrowserSocket;
                if(!socket)continue;
                anyWaiting=anyWaiting||socket.waiting;
                try{if(socket.connected)anyConnected=true;pending+=socket.bytesPending;}catch(e:Error){}
            }
            if(errors>0 && !anyConnected && !anyWaiting)return "ERROR";
            if(anyWaiting && !anyConnected)return "CONECTANDO";
            if(anyConnected && writeCalls==0)return "CONNECTED_SEM_TX";
            if(anyConnected && pending>0)return "TX_PENDENTE";
            if(anyConnected && writeCalls>0 && receivedEvents==0)return "TX_SEM_RX";
            if(anyConnected && receivedEvents>0)return "RX_ATIVO";
            if(requests>0 && connections==0)return "SEM_CONEXAO";
            return "IDLE";
        }

        private function note(phase:String):void {if(report!=null)report("SOCKET 1.3.13: id="+id+"; "+phase);}

        override public function connect(host:String,port:int):void {
            id=++requests;waiting=true;started=getTimer();connectedAt=-1;warned=false;dataLogged=false;
            outputLogged=false;autoFlushLogged=false;explicitFlushLogged=false;firstWriteLogged=false;lastOutputSent=0;
            sockets[this]=true;
            if(!ticker){ticker=new Timer(16);ticker.addEventListener(TimerEvent.TIMER,pump);}
            ticker.start();note("CONNECT REQUEST; timeout="+timeout+"; endereco omitido");
            try{super.connect(host,port);}catch(e:Error){waiting=false;delete sockets[this];errors++;note("CONNECT ERROR #"+e.errorID);throw e;}
        }

        private static function pump(event:TimerEvent):void {
            for(var key:Object in sockets) {
                var socket:BrowserSocket=key as BrowserSocket;
                try {
                    if(socket.connected && socket.bytesPending>0){
                        var pending:uint=socket.bytesPending;
                        autoFlushCalls++;
                        socket.flushBuffered();
                        if(!socket.autoFlushLogged){socket.autoFlushLogged=true;socket.note("AUTO FLUSH; pendingAntes="+pending);}
                    }
                    if(socket.waiting && !socket.warned && getTimer()-socket.started>15000) {
                        socket.warned=true;socket.note("AGUARDANDO CONEXAO 15s");
                    }
                }catch(e:Error){socket.note("FLUSH ERROR #"+e.errorID);delete sockets[socket];}
            }
            var any:Boolean=false;for(var item:Object in sockets){any=true;break;}if(!any && ticker)ticker.stop();
        }

        private function flushBuffered():void {super.flush();}

        override public function flush():void {
            var pending:uint=0;try{pending=bytesPending;}catch(e:Error){}
            super.flush();flushCalls++;
            if(!explicitFlushLogged){explicitFlushLogged=true;note("EXPLICIT FLUSH; pendingAntes="+pending);}
        }

        private function onConnect(event:Event):void {waiting=false;connectedAt=getTimer();connections++;note("CONNECTED");}
        private function onClose(event:Event):void {waiting=false;delete sockets[this];note("CLOSED; "+localSummary());}

        private function onData(event:ProgressEvent):void {
            if(report==null)return;
            receivedEvents++;lastRxAt=getTimer();
            var amount:Number=event.bytesLoaded;
            if(amount<=0){try{amount=bytesAvailable;}catch(e:Error){amount=0;}}
            if(amount>0)bytesReceived+=amount;
            if(!dataLogged){dataLogged=true;note("FIRST DATA; bytes="+uint(amount)+"; conteudo omitido");}
        }

        private function onOutputProgress(event:OutputProgressEvent):void {
            if(report==null)return;
            outputEvents++;
            var sent:Number=event.bytesTotal-event.bytesPending;
            if(sent<0)sent=0;
            if(sent>=lastOutputSent)bytesTransported+=sent-lastOutputSent;else bytesTransported+=sent;
            lastOutputSent=sent;
            if(!outputLogged){outputLogged=true;note("OUTPUT PROGRESS; total="+uint(event.bytesTotal)+"; pending="+uint(event.bytesPending));}
        }

        private function onError(event:Event):void {waiting=false;errors++;delete sockets[this];note("ERROR "+event.type+"; "+localSummary());}
        override public function close():void {waiting=false;delete sockets[this];super.close();}

        private function localSummary():String {
            var pending:uint=0;try{pending=bytesPending;}catch(e:Error){}
            return "connected="+connected+"; pending="+pending;
        }

        private function afterWrite(method:String,length:uint,signature:String=""):void {
            writeCalls++;bytesQueued+=length;lastWriteAt=getTimer();
            if(!firstWriteLogged){
                firstWriteLogged=true;
                var text:String="seq="+writeCalls+" method="+method+" len="+length+(signature?" sig="+signature:"");
                if(!firstWrite)firstWrite=text;
                note("TX FIRST; "+text+"; payload omitido");
            }
        }

        private function byteArraySignature(bytes:ByteArray,offset:uint,length:uint):String {
            if(bytes==null || offset>=bytes.length || length==0)return "";
            var end:uint=Math.min(bytes.length,offset+Math.min(length,32));
            var hash:uint=2166136261;
            for(var i:uint=offset;i<end;i++){hash^=uint(bytes[i]);hash=uint(hash*16777619);}
            var hex:String=hash.toString(16).toUpperCase();while(hex.length<8)hex="0"+hex;
            return "FNV32-"+hex;
        }

        private function utf8Length(value:String):uint {
            var b:ByteArray=new ByteArray();b.writeUTFBytes(value==null?"":value);return b.length;
        }
        private function multiByteLength(value:String,charSet:String):uint {
            var b:ByteArray=new ByteArray();b.writeMultiByte(value==null?"":value,charSet);return b.length;
        }
        private function pendingDelta(before:uint):uint {
            try{var after:uint=bytesPending;return after>=before?after-before:0;}catch(e:Error){return 0;}
        }

        override public function writeBoolean(value:Boolean):void {if(report==null){super.writeBoolean(value);return;}var b:uint=bytesPending;super.writeBoolean(value);var n:uint=pendingDelta(b);afterWrite("writeBoolean",n?n:1);}
        override public function writeByte(value:int):void {if(report==null){super.writeByte(value);return;}var b:uint=bytesPending;super.writeByte(value);var n:uint=pendingDelta(b);afterWrite("writeByte",n?n:1);}
        override public function writeBytes(bytes:ByteArray,offset:uint=0,length:uint=0):void {if(report==null){super.writeBytes(bytes,offset,length);return;}
            var actual:uint=bytes==null?0:(length==0?bytes.length-offset:length);
            var sig:String=byteArraySignature(bytes,offset,actual);
            super.writeBytes(bytes,offset,length);afterWrite("writeBytes",actual,sig);
        }
        override public function writeDouble(value:Number):void {if(report==null){super.writeDouble(value);return;}var b:uint=bytesPending;super.writeDouble(value);var n:uint=pendingDelta(b);afterWrite("writeDouble",n?n:8);}
        override public function writeFloat(value:Number):void {if(report==null){super.writeFloat(value);return;}var b:uint=bytesPending;super.writeFloat(value);var n:uint=pendingDelta(b);afterWrite("writeFloat",n?n:4);}
        override public function writeInt(value:int):void {if(report==null){super.writeInt(value);return;}var b:uint=bytesPending;super.writeInt(value);var n:uint=pendingDelta(b);afterWrite("writeInt",n?n:4);}
        override public function writeMultiByte(value:String,charSet:String):void {if(report==null){super.writeMultiByte(value,charSet);return;}var n:uint=multiByteLength(value,charSet);super.writeMultiByte(value,charSet);afterWrite("writeMultiByte",n);}
        override public function writeObject(object:*):void {if(report==null){super.writeObject(object);return;}var b:uint=bytesPending;super.writeObject(object);afterWrite("writeObject",pendingDelta(b));}
        override public function writeShort(value:int):void {if(report==null){super.writeShort(value);return;}var b:uint=bytesPending;super.writeShort(value);var n:uint=pendingDelta(b);afterWrite("writeShort",n?n:2);}
        override public function writeUnsignedInt(value:uint):void {if(report==null){super.writeUnsignedInt(value);return;}var b:uint=bytesPending;super.writeUnsignedInt(value);var n:uint=pendingDelta(b);afterWrite("writeUnsignedInt",n?n:4);}
        override public function writeUTF(value:String):void {if(report==null){super.writeUTF(value);return;}var n:uint=utf8Length(value)+2;super.writeUTF(value);afterWrite("writeUTF",n);}
        override public function writeUTFBytes(value:String):void {if(report==null){super.writeUTFBytes(value);return;}var n:uint=utf8Length(value);super.writeUTFBytes(value);afterWrite("writeUTFBytes",n);}

        private function afterRead(method:String,before:uint):void {
            var consumed:uint=0;try{var after:uint=bytesAvailable;consumed=before>=after?before-after:0;}catch(e:Error){}
            readCalls++;bytesRead+=consumed;lastReadAt=getTimer();
            if(readCalls==1)note("READ FIRST; method="+method+"; bytes="+consumed+"; conteudo omitido");
        }
        override public function readBoolean():Boolean {if(report==null)return super.readBoolean();var b:uint=bytesAvailable;var v:Boolean=super.readBoolean();afterRead("readBoolean",b);return v;}
        override public function readByte():int {if(report==null)return super.readByte();var b:uint=bytesAvailable;var v:int=super.readByte();afterRead("readByte",b);return v;}
        override public function readBytes(bytes:ByteArray,offset:uint=0,length:uint=0):void {if(report==null){super.readBytes(bytes,offset,length);return;}var b:uint=bytesAvailable;super.readBytes(bytes,offset,length);afterRead("readBytes",b);}
        override public function readDouble():Number {if(report==null)return super.readDouble();var b:uint=bytesAvailable;var v:Number=super.readDouble();afterRead("readDouble",b);return v;}
        override public function readFloat():Number {if(report==null)return super.readFloat();var b:uint=bytesAvailable;var v:Number=super.readFloat();afterRead("readFloat",b);return v;}
        override public function readInt():int {if(report==null)return super.readInt();var b:uint=bytesAvailable;var v:int=super.readInt();afterRead("readInt",b);return v;}
        override public function readMultiByte(length:uint,charSet:String):String {if(report==null)return super.readMultiByte(length,charSet);var b:uint=bytesAvailable;var v:String=super.readMultiByte(length,charSet);afterRead("readMultiByte",b);return v;}
        override public function readObject():* {if(report==null)return super.readObject();var b:uint=bytesAvailable;var v:* = super.readObject();afterRead("readObject",b);return v;}
        override public function readShort():int {if(report==null)return super.readShort();var b:uint=bytesAvailable;var v:int=super.readShort();afterRead("readShort",b);return v;}
        override public function readUnsignedByte():uint {if(report==null)return super.readUnsignedByte();var b:uint=bytesAvailable;var v:uint=super.readUnsignedByte();afterRead("readUnsignedByte",b);return v;}
        override public function readUnsignedInt():uint {if(report==null)return super.readUnsignedInt();var b:uint=bytesAvailable;var v:uint=super.readUnsignedInt();afterRead("readUnsignedInt",b);return v;}
        override public function readUnsignedShort():uint {if(report==null)return super.readUnsignedShort();var b:uint=bytesAvailable;var v:uint=super.readUnsignedShort();afterRead("readUnsignedShort",b);return v;}
        override public function readUTF():String {if(report==null)return super.readUTF();var b:uint=bytesAvailable;var v:String=super.readUTF();afterRead("readUTF",b);return v;}
        override public function readUTFBytes(length:uint):String {if(report==null)return super.readUTFBytes(length);var b:uint=bytesAvailable;var v:String=super.readUTFBytes(length);afterRead("readUTFBytes",b);return v;}
    }
}
