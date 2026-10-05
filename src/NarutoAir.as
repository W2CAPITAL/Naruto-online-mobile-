package
{
   import flash.display.Loader;
   import flash.display.Sprite;
   import flash.display.DisplayObjectContainer;
   import flash.desktop.Clipboard;
   import flash.desktop.ClipboardFormats;
   import flash.events.MouseEvent;
   import flash.system.Capabilities;
   import flash.ui.ContextMenu;
   import flash.external.ExternalInterface;
   import flash.utils.getTimer;
   import flash.utils.getQualifiedClassName;
   import flash.events.ErrorEvent;
   import flash.events.Event;
   import flash.events.HTTPStatusEvent;
   import flash.events.IOErrorEvent;
   import flash.events.ProgressEvent;
   import flash.events.SecurityErrorEvent;
   import flash.events.StatusEvent;
   import flash.events.TimerEvent;
   import flash.events.UncaughtErrorEvent;
   import flash.net.URLRequest;
   import flash.net.URLStream;
   import flash.net.FileReference;
   import flash.utils.ByteArray;
   import flash.utils.Endian;
   import flash.net.URLRequestHeader;
   import flash.system.ApplicationDomain;
   import flash.system.LoaderContext;
   import flash.system.Security;
   import flash.text.TextField;
   import flash.text.TextFormat;
   import flash.ui.Multitouch;
   import flash.utils.Timer;
   import flash.utils.getDefinitionByName;
   import br.davi.narutoair.compat.MobileControls;
   import br.davi.narutoair.compat.FloatingButton;
   import br.davi.narutoair.compat.GraphicsProfile;
   import br.davi.narutoair.compat.BrowserSocket;
   import br.davi.narutoair.compat.BrowserSound;
   import br.davi.narutoair.compat.AudioSession;
   import br.davi.narutoair.compat.BrowserSecurity;
   import br.davi.narutoair.compat.BrowserLoader;
   import br.davi.narutoair.compat.BrowserResources;
   import br.davi.narutoair.compat.BrowserURLLoader;
   import br.davi.narutoair.compat.BrowserURLStream;
   import br.davi.narutoair.compat.BrowserLaunchParameters;
   import br.davi.narutoair.compat.BrowserExternalInterface;
   
   [SWF(width="500", height="375", backgroundColor="#ffffff", frameRate="60")]
   public class NarutoAir extends Sprite
   {
      
      private var bridge:*;
      private var controls:MobileControls;
      private var diagnosticFloating:FloatingButton;
      private var stageBaseline:Array=[];
      private var graphicsProfile:GraphicsProfile;
      private static const SERVER_LIST:String="https://naruto.narutowebgame.com/pt/serverlist/";
      private var portalHealthTicks:int=0;
      private var rechargeUid:String="",rechargeServer:String="";
      
      private var loader:Loader;
      
      private var logField:TextField;
      
      private var launched:Boolean = false;
      
      private var pollTimer:Timer;
      
      private var loadTimeout:Timer;
      
      private var lastNativeStatus:String = "";
      
      private var swfStarted:Boolean = false;
      
      private var lastProgress:int = -1;
      
      private var runtimeBase:String = "";
      private var download:URLStream;
      private var entryBytes:ByteArray;
      private var entryContext:LoaderContext;
      private var entryHttpStatus:int = 0;
      private var failed:Boolean = false;
      private var diagnosticLines:Array=[];
      private var diagnosticButton:TextField;
      private var portalButton:TextField;
      private var saveEntryButton:TextField;
      private var entrySnapshot:ByteArray;
      private var entrySave:FileReference;
      private var diagnosticTimer:Timer;
      private var importedParams:int=0;
      private static const MAX_ENTRY_BYTES:uint = 4 * 1024 * 1024;
      
      public function NarutoAir()
      {
         super();
         addEventListener("addedToStage",init);
      }
      
      private function init(param1:Event) : void
      {
         var _loc2_:Class = null;
         removeEventListener("addedToStage",init);
         stage.scaleMode = "noScale";
         stage.align = "TL";
         stage.frameRate = 60;
         try
         {
            Multitouch.inputMode = "none";
         }
         catch(touchErr:Error)
         {
         }
         graphics.beginFill(0);
         graphics.drawRect(0,0,stage.stageWidth,stage.stageHeight);
         graphics.endFill();
         createLog();
         createDiagnosticButton();
         AudioSession.initialize();
         BrowserSecurity.configure(null);
         // Keep all dynamically retargeted client class definitions in the parent domain.
         var resourceClasses:Array=[BrowserURLLoader,BrowserURLStream,BrowserSocket,BrowserSound];
         log("Naruto AIR C71 1.3.18 - carregamento dos modulos no AIR");
         try
         {
            _loc2_ = getDefinitionByName("br.davi.narutoair.portal.PortalMarker") as Class;
            if(!_loc2_)
            {
               log("ERRO: PortalMarker nao encontrado.");
               return;
            }
            bridge = new _loc2_();
            log("ANE wrapper carregado.");
         }
         catch(err:Error)
         {
            log("ERRO ANE wrapper #" + err.errorID + ": " + err.message);
            return;
         }
         bridge.addEventListener("status",onNativeStatus);
         try
         {
            log("PING ANE: " + String(bridge.ping()));
         }
         catch(pingErr:Error)
         {
            log("ERRO ping #" + pingErr.errorID + ": " + pingErr.message);
         }
         try
         {
            log("OPEN ANE: " + String(bridge.openPortal(SERVER_LIST)));
         }
         catch(openErr:Error)
         {
            log("ERRO open #" + openErr.errorID + ": " + openErr.message);
         }
         graphicsProfile=new GraphicsProfile(stage,this,stageBaseline,function(percent:int):Boolean {return bridge && bridge.graphicsScale(percent)===true;},null,function(choice:Number):Boolean{return bridge && bridge.graphicsFPS(choice)===true;},function(enabled:Boolean,epoch:int):Boolean{return bridge && bridge.graphicsFSR(enabled,epoch)===true;});
         controls=new MobileControls(stage,function(a:String,x:Number,y:Number,key:int):Boolean {return bridge && bridge.input(a,String(x),String(y),String(key))===true;},restartPortal,switchAccount,graphicsProfile,openRecharge,function(enabled:Boolean,style:int):Boolean{return bridge && bridge.kaguyaMode(enabled,style)===true;},function():Object {return bridge ? bridge.audioDiagnostic() : "Indisponivel";});
         stage.addChild(controls);
         controls.visible=false;
         for(var baselineIndex:int=0;baselineIndex<stage.numChildren;baselineIndex++)stageBaseline.push(stage.getChildAt(baselineIndex));
         stage.addEventListener(Event.ACTIVATE,function(e:Event):void {AudioSession.start();report("APP ACTIVATE 1.3.18");});
         stage.addEventListener(Event.DEACTIVATE,function(e:Event):void {report("APP DEACTIVATE 1.3.18");});
         pollTimer = new Timer(700);
         pollTimer.addEventListener("timer",pollNativeStatus);
         pollTimer.start();
      }
      
      private function createLog() : void
      {
         logField = new TextField();
         logField.defaultTextFormat = new TextFormat("_sans",17,16777215);
         logField.multiline = true;
         logField.wordWrap = true;
         logField.selectable = true;
         logField.width = Math.max(600,stage.stageWidth - 40);
         logField.height = Math.max(240,stage.stageHeight - 40);
         logField.x = 20;
         logField.y = 20;
         logField.visible=false;
         addChild(logField);
      }
      
      private function log(param1:String) : void
      {
         // Release build: no log text or cross-thread diagnostic traffic.
      }
      
      private function report(param1:String) : void
      {
         // Release build: diagnostics are disabled.
      }
      
      private function pollNativeStatus(param1:TimerEvent) : void
      {
         if(!bridge || launched)return;
         try {if(++portalHealthTicks%7==0)bridge.portalHealth();}catch(error:Error){}
      }
      
      private function isPlaceholderSwf(param1:String) : Boolean
      {
         var _loc2_:String = (param1 || "").toLowerCase();
         return _loc2_.indexOf("/empty.swf") >= 0 || _loc2_.indexOf("/blank.swf") >= 0 || _loc2_ == "empty.swf" || _loc2_ == "blank.swf";
      }
      
      private function onNativeStatus(param1:StatusEvent) : void
      {
         var _loc3_:Object = null;
         var _loc2_:String = null;
         if(param1.code=="graphics"){if(graphicsProfile)graphicsProfile.nativeStatus(param1.level);return;}
         if(param1.code=="fsr"){if(graphicsProfile)graphicsProfile.fsrStatus(param1.level);return;}
         if(param1.code=="select-server" || param1.code=="account-cleared"){restartPortal();return;}
         if(param1.code=="account-error"){report(param1.level);restartPortal();return;}
         if(param1.code=="browser-callback") {
            try {BrowserExternalInterface.receive(param1.level);}catch(callbackError:Error){failEntry("Falha em callback da pagina oficial: "+callbackError.message);}
            return;
         }
         if(param1.code=="browser-error") {failEntry(param1.level);return;}
         if(param1.code == "log")
         {
            log("EVENTO: " + param1.level);
            return;
         }
         if(param1.code == "launch" && !launched)
         {
            try
            {
               _loc3_ = JSON.parse(param1.level);
               _loc2_ = _loc3_.swf ? String(_loc3_.swf) : "";
               if(!_loc2_ || _loc2_.toLowerCase().indexOf(".swf") < 0)
               {
                  report("Captura recebida sem SWF valido.");
                  return;
               }
               if(isPlaceholderSwf(_loc2_))
               {
                  report("PLACEHOLDER SWF rejeitado: " + _loc2_);
                  launched = false;
                  return;
               }
               launched = true;
               report("SWF real capturado; iniciando pelo AIR...");
               log("SWF: " + _loc2_);
               log("Pagina origem: " + (String(_loc3_.page || "")));
               if(_loc3_.originSwf)
               {
                  log("SWF origem CDN: " + String(_loc3_.originSwf));
               }
               if(_loc3_.runtimeProxyBase)
               {
                  log("Proxy runtime: " + String(_loc3_.runtimeProxyBase));
               }
               log("FlashVars: " + countKeys(_loc3_.flashvars || {}));
               if(_loc3_.candidates)
               {
                  log("Candidatos SWF observados: " + _loc3_.candidates.length);
               }
               launchSwf(_loc2_,_loc3_.flashvars || {},String(_loc3_.cookie || ""),String(_loc3_.page || ""),String(_loc3_.userAgent || ""),String(_loc3_.originSwf || _loc2_),_loc3_.browser);
            }
            catch(err:Error)
            {
               launched = false;
               report("Falha ao interpretar dados do portal: " + err.message);
            }
         }
      }
      
      private function openRecharge():void {
         if(!bridge || !swfStarted)return;
         try {if(bridge.recharge(rechargeUid,rechargeServer)!==true)report("Nao foi possivel abrir a recarga oficial.");}
         catch(error:Error){report("Recarga oficial indisponivel.");}
      }

      private function launchSwf(param1:String, param2:Object, param3:String, param4:String, param5:String, originalSwf:String, browserState:Object) : void
      {
         var _loc10_:Object = null;
         var _loc9_:Boolean = false;
         var _loc11_:String = null;
         var _loc13_:String = null;
         var _loc12_:int = 0;
         var _loc7_:int = 0;
         var _loc14_:URLRequest = null;
         var _loc6_:Array = null;
         var _loc8_:LoaderContext = null;
         try
         {
            cleanupEntry();
            entrySnapshot=null;
            failed = false;
            if(graphicsProfile)graphicsProfile.start();
            BrowserSecurity.configure(null);
            BrowserSocket.configure(null);
            if(!validEntryPair(param1, originalSwf)) throw new Error("Entrada SWF fora do CDN oficial ou proxy local invalido.");
            BrowserResources.configure(originalSwf,param1);
            if(bridge)bridge.audioDiagnostic("reset");
            BrowserResources.setReporter(null);
            AudioSession.start();
            loader = new BrowserLoader();
            // Add the empty Loader before execution, so the client can see Stage during INIT.
            addChildAt(loader, 0);
            loader.contentLoaderInfo.addEventListener("open",onSwfOpen);
            loader.contentLoaderInfo.addEventListener("init",onSwfInit);
            loader.contentLoaderInfo.addEventListener("complete",onLoaded);
            loader.contentLoaderInfo.addEventListener("progress",onProgress);
            loader.contentLoaderInfo.addEventListener("httpStatus",onHttpStatus);
            loader.contentLoaderInfo.addEventListener("ioError",onLoadError);
            loader.contentLoaderInfo.addEventListener("securityError",onSecurityError);
            loader.contentLoaderInfo.uncaughtErrorEvents.addEventListener("uncaughtError",onGameUncaughtError);
            diagnosticLines=[];
            _loc10_ = BrowserLaunchParameters.merge(originalSwf,param2);
            BrowserExternalInterface.configure(browserState,bridge.browserEval,bridge.browserConfigure,null);
            rechargeUid=String(browserState.rechargeUid || BrowserLaunchParameters.merge(param4,null).user_name || "");
            rechargeServer=String(param2.zone_id || "");
            report("CLIENT WEB BRIDGE: identidade obtida da pagina oficial; modo web habilitado para o cliente.");
            importedParams=countKeys(_loc10_);
            report("PARAMS 1.3.18: URL + FlashVars preservados; URL="+countKeys(BrowserLaunchParameters.merge(originalSwf,null))+"; FlashVars="+countKeys(param2 || {})+"; total="+importedParams+"; valores omitidos.");
            var parameterNames:Array=[];
            for(var parameterName:String in _loc10_) {
               if(/^[a-zA-Z_][a-zA-Z0-9_]{0,63}$/.test(parameterName))parameterNames.push(parameterName);
            }
            parameterNames.sort();
            report("PARAM NAMES: "+parameterNames.join(","));
            for each(parameterName in parameterNames) {
               if(!/(resource|version|config|cdn|base|root)/i.test(parameterName))continue;
               var assetValue:String=String(_loc10_[parameterName]);
               if(assetValue.length<=300 && (/^https?:\/\/(?:cdnnaruto-pt|naruto-pt)\.oasgames\.com\//i.test(assetValue) ||
                  /^(?:PT_NarutoAlpha[0-9.]+Build[0-9]+\/)?[a-zA-Z0-9_.\/-]+\.(?:cfg|xml|txt|dat|bin)(?:\?[^#]*)?$/.test(assetValue))) {
                  report("PARAM ASSET: "+parameterName+"="+assetValue.split(/[?#]/)[0]);
               }
            }
            _loc9_ = param1.indexOf("cdnnaruto-pt.oasgames.com/") >= 0 || param1.indexOf("127.0.0.1:") >= 0;
            _loc13_ = _loc11_ = _loc9_ ? param1 : appendMissingParams(param1,_loc10_);
            _loc12_ = _loc13_.indexOf("?");
            if(_loc12_ >= 0)
            {
               _loc13_ = _loc13_.substring(0,_loc12_);
            }
            _loc7_ = _loc13_.lastIndexOf("/");
            runtimeBase = _loc7_ >= 0 ? _loc13_.substring(0,_loc7_ + 1) : "";
            log("Runtime base CDN: " + runtimeBase);
            log("FlashVars via LoaderContext: " + countKeys(_loc10_));
            _loc14_ = new URLRequest(_loc11_);
            _loc14_.followRedirects = false;
            _loc14_.manageCookies = false;
            _loc14_.idleTimeout = 30000;
            if(param5 && param5.length > 0)
            {
               _loc14_.userAgent = param5;
            }
            _loc6_ = [];
            if(param3)
            {
               _loc6_.push(new URLRequestHeader("Cookie",param3));
            }
            if(param4)
            {
               _loc6_.push(new URLRequestHeader("Referer",param4));
            }
            _loc6_.push(new URLRequestHeader("X-Flash-Version","21,0,0,213"));
            if(param1.indexOf("http://127.0.0.1:") == 0) _loc6_.push(new URLRequestHeader("X-Naruto-AIR-Compat","security-1"));
            _loc6_.push(new URLRequestHeader("Accept","application/x-shockwave-flash,*/*;q=0.8"));
            _loc14_.requestHeaders = _loc6_;
            _loc8_ = new LoaderContext(false,new ApplicationDomain(ApplicationDomain.currentDomain),null);
            _loc8_.allowCodeImport = true;
            _loc8_.parameters = _loc10_;
            BrowserLoader.configure(adaptClientBytes,null,_loc8_.applicationDomain,failEntry,stage);
            report("RUNTIME 1.3.18: AIR="+Capabilities.version+"; ContextMenu="+ContextMenu.isSupported+"; playerType="+Capabilities.playerType+"; ExternalInterface="+ExternalInterface.available+"; perfil web anunciado=Flash 21.0.0.213");
            report("Solicitando SWF real com sessao do portal...");
            log("Cookie nativo: " + (param3 ? "SIM (" + param3.length + " chars)" : "NAO"));
            log("User-Agent recebido do WebView: " + (param5 ? "SIM" : "NAO"));
            swfStarted = false;
            lastProgress = -1;
            // allowCodeImport alone does not import Loader.load(URLRequest) in AIR.
            // Download the authenticated official entry as data, then loadBytes in this sandbox.
            entryContext = _loc8_;
            entryBytes = new ByteArray();
            entryHttpStatus = 0;
            download = new URLStream();
            download.addEventListener(Event.OPEN, onSwfOpen);
            download.addEventListener(ProgressEvent.PROGRESS, onEntryProgress);
            download.addEventListener(HTTPStatusEvent.HTTP_RESPONSE_STATUS, onEntryHttpStatus);
            download.addEventListener(Event.COMPLETE, onEntryDownloaded);
            download.addEventListener(IOErrorEvent.IO_ERROR, onLoadError);
            download.addEventListener(SecurityErrorEvent.SECURITY_ERROR, onSecurityError);
            loadTimeout = new Timer(30000,1);
            loadTimeout.addEventListener("timerComplete",onLoadTimeout);
            loadTimeout.start();
            download.load(_loc14_);
         }
         catch(err:Error)
         {
            failEntry("ERRO sincronico SWF #" + err.errorID + ": " + err.message);
         }
      }
      
      private function validEntryPair(url:String, original:String):Boolean {
         var official:RegExp = /^https:\/\/cdnnaruto-pt\.oasgames\.com(\/PT_NarutoAlpha[0-9.]+Build[0-9]+\/entry\.swf)(?:\?[^#]*)?$/;
         var remote:Array = official.exec(original);
         if(!remote) return false;
         if(url === original) return true;
         var proxy:Array = /^http:\/\/127\.0\.0\.1:([0-9]{1,5})(\/PT_NarutoAlpha[0-9.]+Build[0-9]+\/entry\.swf)(?:\?[^#]*)?$/.exec(url);
         return proxy != null && uint(proxy[1]) > 0 && uint(proxy[1]) <= 65535 && proxy[2] === remote[1];
      }

      private function adaptClientBytes(bytes:ByteArray):Object {
         if(!bridge) throw new Error("Ponte nativa do cliente indisponivel.");
         return bridge.adaptSwfBytes(bytes);
      }

      private function drainEntry():void {
         if(!download || !entryBytes) return;
         var available:uint = download.bytesAvailable;
         if(available > MAX_ENTRY_BYTES - entryBytes.length) throw new Error("Entrada SWF excede 4 MB.");
         if(available) download.readBytes(entryBytes, entryBytes.length, available);
      }

      private function onEntryProgress(event:ProgressEvent):void {
         if(failed || !download) return;
         try { drainEntry(); onProgress(event); }
         catch(err:Error) { failEntry(err.message); }
      }

      private function onEntryHttpStatus(event:HTTPStatusEvent):void {
         entryHttpStatus = event.status;
         onHttpStatus(event);
         var adapted:Boolean=false;
         for each(var header:URLRequestHeader in event.responseHeaders) {
            if(header.name.toLowerCase() == "x-naruto-air-compat") {
               report("ENTRY ADAPTER 1.3.18: referencias compat=" + header.value);adapted=true;
            }
         }
         if(event.status == 200 && !adapted) report("ENTRY ADAPTER 1.3.18: resposta sem adaptacao; chamadas do navegador podem falhar.");
         if(event.status >= 400) failEntry("HTTP ERROR SWF: " + event.status);
      }

      private function onEntryDownloaded(event:Event):void {
         if(failed || !loader || !download) return;
         try {
            drainEntry();
            if(entryHttpStatus != 200) throw new Error("Resposta de entrada invalida: HTTP " + entryHttpStatus);
            validateEntry(entryBytes);
            // Keep only static entry code, never the session, cookies or FlashVars.
            entrySnapshot=null;
            entryBytes.position = 0;
            report("SWF BYTES 1.3.18: entrada oficial validada; executando no sandbox do AIR.");
            loader.loadBytes(entryBytes, entryContext);
            releaseDownload();
         } catch(err:Error) { failEntry("SWF BYTES falhou #" + err.errorID + ": " + err.message); }
      }

      private function validateEntry(bytes:ByteArray):void {
         if(!bytes || bytes.length < 8) throw new Error("Entrada SWF vazia ou truncada.");
         bytes.position = 0;
         var signature:String = bytes.readUTFBytes(3);
         if(signature != "FWS" && signature != "CWS" && signature != "ZWS") throw new Error("Resposta nao e um SWF; HTML de erro recusado.");
         var version:uint = bytes.readUnsignedByte();
         bytes.endian = Endian.LITTLE_ENDIAN;
         var declared:uint = bytes.readUnsignedInt();
         if(!version || declared < 8 || declared > 32 * 1024 * 1024) throw new Error("Cabecalho SWF invalido ou expandido acima de 32 MB.");
         if(signature == "FWS" && declared != bytes.length) throw new Error("SWF sem compressao incompleto.");
      }

      private function releaseDownload():void {
         if(download) {
            download.removeEventListener(Event.OPEN, onSwfOpen);
            download.removeEventListener(ProgressEvent.PROGRESS, onEntryProgress);
            download.removeEventListener(HTTPStatusEvent.HTTP_RESPONSE_STATUS, onEntryHttpStatus);
            download.removeEventListener(Event.COMPLETE, onEntryDownloaded);
            download.removeEventListener(IOErrorEvent.IO_ERROR, onLoadError);
            download.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, onSecurityError);
            try { download.close(); } catch(err:Error) {}
            download = null;
         }
         entryBytes = null;
         entryContext = null;
      }

      private function cleanupEntry():void {
         rechargeUid="";rechargeServer="";
         if(graphicsProfile)graphicsProfile.stop();
         if(diagnosticTimer){diagnosticTimer.stop();diagnosticTimer.removeEventListener(TimerEvent.TIMER,diagnosticSnapshot);diagnosticTimer=null;}
         if(diagnosticButton)diagnosticButton.visible=false;
         if(portalButton)portalButton.visible=false;
         if(saveEntryButton)saveEntryButton.visible=false;
         if(controls)controls.stop();
         AudioSession.stop();
         BrowserSocket.disable();
         BrowserExternalInterface.disable();
         stopLoadTimeout();
         releaseDownload();
         if(loader) {
            loader.contentLoaderInfo.removeEventListener(Event.INIT, onSwfInit);
            loader.contentLoaderInfo.removeEventListener(Event.COMPLETE, onLoaded);
            loader.contentLoaderInfo.uncaughtErrorEvents.removeEventListener(UncaughtErrorEvent.UNCAUGHT_ERROR, onGameUncaughtError);
            try { loader.unloadAndStop(true); } catch(err:Error) {}
            if(loader.parent) loader.parent.removeChild(loader);
            loader = null;
         }
         BrowserLoader.disable();BrowserURLLoader.disable();BrowserURLStream.disable();
         BrowserResources.disable();
         // The official client mounts loading/world UI directly on Stage.
         // Keep app-owned overlays and remove the previous client's mounted UI.
         if(stageBaseline.length)for(var childIndex:int=stage.numChildren-1;childIndex>=0;childIndex--)
            if(stageBaseline.indexOf(stage.getChildAt(childIndex))<0)stage.removeChildAt(childIndex);
      }

      private function failEntry(message:String):void {
         if(failed)return;failed=true;launched=false;
         cleanupEntry();hidePortal();
         logField.text="Nao foi possivel carregar o jogo.\n\nVolte ao portal e selecione o servidor novamente.";
         logField.width=Math.max(1,stage.stageWidth-40);logField.height=130;logField.visible=true;
         portalButton.x=20;portalButton.y=170;portalButton.visible=true;
      }

      private function onSwfOpen(param1:Event) : void
      {
         swfStarted = true;
         report("SWF OPEN: conexao aceita.");
      }
      
      private function onHttpStatus(param1:HTTPStatusEvent) : void
      {
         report("HTTP STATUS SWF: " + param1.status + (param1.redirected ? " redirect" : ""));
      }
      
      private function onProgress(param1:ProgressEvent) : void
      {
         if(param1.bytesTotal <= 0)
         {
            return;
         }
         var _loc2_:int = param1.bytesLoaded * 100 / param1.bytesTotal;
         var _loc3_:int = int(_loc2_ / 10) * 10;
         if(_loc3_ != lastProgress && _loc3_ >= 10)
         {
            lastProgress = _loc3_;
            report("SWF download: " + _loc2_ + "%");
         }
      }
      
      private function onSwfInit(param1:Event) : void
      {
         if(failed || !loader) return;
         try {
            if(!loader.content || loader.content.stage !== stage) throw new Error("Cliente sem acesso ao Stage do AIR.");
            report("SWF INIT 1.3.18: Stage acessivel; entrada iniciou, modulos do jogo ainda podem carregar.");
            logField.visible=false;

            hidePortal();
            if(controls){controls.visible=true;stage.setChildIndex(controls,stage.numChildren-1);}
         } catch(err:Error) { failEntry("SWF INIT falhou #" + err.errorID + ": " + err.message); }
      }
      
      private function onLoaded(param1:Event) : void
      {
         if(failed || !loader) return;
         stopLoadTimeout();
         releaseDownload();
         report("ENTRY COMPLETE 1.3.18: entrada carregada; nao significa que todos os recursos do jogo terminaram.");
         hidePortal();
         if(loader && !contains(loader))
         {
            addChildAt(loader,0);
         }
         // The game's own stage layout sizes its preloader and world. Scaling the
         // Loader from the temporary preloader bounds distorts later screens.
         if(loader) {loader.x=0;loader.y=0;loader.scaleX=1;loader.scaleY=1;}

      }

      private function createDiagnosticButton():void {
         portalButton=new TextField();
         portalButton.defaultTextFormat=new TextFormat("_sans",17,0xffffff,true);
         portalButton.text="VOLTAR AO PORTAL";portalButton.selectable=false;
         portalButton.background=true;portalButton.backgroundColor=0x303030;
         portalButton.width=230;portalButton.height=42;portalButton.visible=false;
         portalButton.addEventListener(MouseEvent.CLICK,returnToPortal);addChild(portalButton);
      }

      private function saveEntrySnapshot(event:MouseEvent):void {
         if(!entrySnapshot || entrySave)return;
         entrySave=new FileReference();
         entrySave.addEventListener(Event.COMPLETE,onEntrySaveFinished);
         entrySave.addEventListener(Event.CANCEL,onEntrySaveFinished);
         entrySave.addEventListener(IOErrorEvent.IO_ERROR,onEntrySaveFinished);
         try {entrySave.save(entrySnapshot,"Naruto-entry-1.3.18.swf");}
         catch(error:Error){report("ENTRY SAVE ERROR: #"+error.errorID);onEntrySaveFinished(new IOErrorEvent(IOErrorEvent.IO_ERROR));}
      }

      private function onEntrySaveFinished(event:Event):void {
         if(entrySave){
            entrySave.removeEventListener(Event.COMPLETE,onEntrySaveFinished);
            entrySave.removeEventListener(Event.CANCEL,onEntrySaveFinished);
            entrySave.removeEventListener(IOErrorEvent.IO_ERROR,onEntrySaveFinished);
            entrySave=null;
         }
         saveEntryButton.text=event.type==Event.COMPLETE?"ENTRADA SALVA":event.type==Event.CANCEL?"SALVAR ENTRADA":"FALHA AO SALVAR";
         report("ENTRY SAVE: "+event.type);
      }

      private function restartPortal():void {
         cleanupEntry();launched=false;failed=false;portalHealthTicks=0;
         if(bridge){bridge.removeEventListener("status",onNativeStatus);bridge.dispose();}
         var marker:Class=getDefinitionByName("br.davi.narutoair.portal.PortalMarker") as Class;
         bridge=new marker();bridge.addEventListener("status",onNativeStatus);
         bridge.openPortal(SERVER_LIST);
      }

      private function switchAccount():void {
         cleanupEntry();launched=false;failed=false;
         report("TROCAR CONTA: saindo da sessao do portal por solicitacao do usuario.");
         if(!bridge || bridge.resetAccount()!==true){report("Nao foi possivel sair da conta.");restartPortal();}
      }

      private function returnToPortal(event:MouseEvent):void {
         restartPortal();
      }

      private function diagnosticSnapshot(event:TimerEvent=null):void {
         // No recurring tree walk or diagnostics in the release build.
      }

      private function loadingState():String {
         var queue:Array=[stage],visited:int=0;
         while(queue.length && visited++<400){
            var object:Object=queue.shift();
            var name:String=getQualifiedClassName(object);
            if(/LoadingView$/.test(name)) {
               try {return "carregamentoClasse="+name+"; percentual="+Number(object.percent)+"; visivel="+object.visible;}catch(e:Error){}
            }
            if(object is DisplayObjectContainer){var container:DisplayObjectContainer=object as DisplayObjectContainer;for(var j:int=0;j<container.numChildren && queue.length<400;j++)queue.push(container.getChildAt(j));}
         }
         return "preloader=nao localizado";
      }

      private function copyDiagnostic(event:MouseEvent):void {
         diagnosticSnapshot();
         try {
            Clipboard.generalClipboard.clear();
            var copied:Boolean=Clipboard.generalClipboard.setData(ClipboardFormats.TEXT_FORMAT,"Naruto AIR 1.3.18 — diagnostico de handshake\n"+diagnosticLines.join("\n"));
            diagnosticButton.text=copied?"COPIADO":"COPIA FALHOU";
         }catch(error:Error){diagnosticButton.text="COPIA FALHOU";report("DIAG COPY ERROR: #"+error.errorID);}
      }
      
      private function onLoadTimeout(param1:TimerEvent) : void
      {
         failEntry("TIMEOUT 30s: OPEN=" + (swfStarted ? "SIM" : "NAO"));
      }
      
      private function onLoadError(param1:IOErrorEvent) : void
      {
         failEntry("IO ERROR SWF: " + param1.text);
      }
      
      private function onGameUncaughtError(param1:UncaughtErrorEvent) : void
      {
         var _loc3_:Error = null;
         var _loc2_:String = "";
         try
         {
            if(param1.error is ErrorEvent)
            {
               _loc2_ = ErrorEvent(param1.error).text;
            }
            else if(param1.error is Error)
            {
               _loc3_ = Error(param1.error);
               _loc2_ = "#" + _loc3_.errorID + " " + _loc3_.message;
               if(_loc3_.getStackTrace())
               {
                  _loc2_ += " | " + _loc3_.getStackTrace();
               }
            }
            else
            {
               _loc2_ = String(param1.error);
            }
         }
         catch(x:Error)
         {
            _loc2_ = String(param1.error);
         }
         failEntry("GAME UNCAUGHT: " + _loc2_);
         if(runtimeBase)
         {
            log("Base de recursos: " + runtimeBase);
         }
         try
         {
            param1.preventDefault();
         }
         catch(ignore:Error)
         {
         }
      }
      
      private function onSecurityError(param1:SecurityErrorEvent) : void
      {
         failEntry("SECURITY ERROR SWF: " + param1.text);
      }
      
      private function hidePortal() : void
      {
         try
         {
            if(bridge)
            {
               bridge.hide();
            }
         }
         catch(err:Error)
         {
            log("ERRO hide: " + err.message);
         }
      }
      
      private function showPortal() : void
      {
         if(diagnosticButton)diagnosticButton.visible=false;
         if(portalButton)portalButton.visible=false;
         if(saveEntryButton)saveEntryButton.visible=false;
         if(logField)logField.visible=true;
         try
         {
            if(bridge)
            {
               bridge.show();
            }
         }
         catch(err:Error)
         {
            log("ERRO show: " + err.message);
         }
      }
      
      private function stopLoadTimeout() : void
      {
         if(loadTimeout)
         {
            loadTimeout.removeEventListener(TimerEvent.TIMER_COMPLETE, onLoadTimeout);
            loadTimeout.stop();
            loadTimeout = null;
         }
      }
      
      private function stringifyParams(param1:Object) : Object
      {
         var _loc3_:Object = {};
         if(!param1)
         {
            return _loc3_;
         }
         for(var _loc2_:String in param1)
         {
            try
            {
               if(_loc2_ === "__proto__" || _loc2_ === "constructor" || _loc2_ === "prototype") continue;
               _loc3_[_loc2_] = param1[_loc2_] == null ? "" : String(param1[_loc2_]);
            }
            catch(e:Error)
            {
               _loc3_[_loc2_] = "";
            }
         }
         return _loc3_;
      }
      
      private function appendMissingParams(param1:String, param2:Object) : String
      {
         var _loc6_:String = null;
         var _loc3_:String = param1;
         var _loc5_:String = _loc3_.indexOf("?") >= 0 ? "&" : "?";
         for(var _loc4_:String in param2)
         {
            _loc6_ = encodeURIComponent(_loc4_) + "=";
            if(_loc3_.indexOf(_loc6_) < 0)
            {
               _loc3_ += _loc5_ + encodeURIComponent(_loc4_) + "=" + encodeURIComponent(String(param2[_loc4_]));
               _loc5_ = "&";
            }
         }
         return _loc3_;
      }
      
      private function countKeys(param1:Object) : int
      {
         var _loc3_:int = 0;
         for(var _loc2_:String in param1)
         {
            _loc3_++;
         }
         return _loc3_;
      }
   }
}
