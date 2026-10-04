package br.davi.narutoair.fsr;

import android.app.Activity;
import android.view.*;
import android.widget.FrameLayout;
import android.graphics.PixelFormat;
import android.opengl.*;
import com.adobe.fre.FREContext;
import java.lang.ref.WeakReference;
import java.io.*;
import java.util.*;
import static android.opengl.GLES30.*;

/** Optional FSR 1 postprocessor on the AIR EGL14 render thread. No CPU screenshots or frame interpolation. */
public final class FsrRenderer {
 private static final Map<Object,WeakReference<SurfaceView>> owners=new WeakHashMap<Object,WeakReference<SurfaceView>>();
 private static final Map<SurfaceView,Session> sessions=new WeakHashMap<SurfaceView,Session>();
 private FsrRenderer(){}
 public static void register(Object egl,SurfaceView view){try{synchronized(owners){WeakReference<SurfaceView> previous=owners.get(egl);if(previous==null || previous.get()!=view)owners.put(egl,new WeakReference<SurfaceView>(view));}}catch(Throwable ignored){}}
 /** Must run on Android UI thread. Preference deliberately starts OFF on each launch. */
 public static boolean configure(FREContext context,Object rawView,boolean enabled,int epoch){
  if(!(rawView instanceof SurfaceView))return false;
  SurfaceView view=(SurfaceView)rawView;
  Session s;synchronized(sessions){s=sessions.get(view);}
  if(!enabled){if(s!=null){if(epoch>0)s.request=epoch;s.disable("off");}return true;}
  if(s==null){s=new Session(context,view);synchronized(sessions){sessions.put(view,s);}}
  if(epoch<=0)return false;s.request=epoch;
  try{s.enable();return true;}catch(Throwable unavailable){s.disable("fallback:Sem compositor GPU compativel");return false;}
 }
 public static void beforeSwap(Object egl){
  Session s=null;long attempt=0;
  try{
   SurfaceView view; synchronized(owners){WeakReference<SurfaceView> owner=owners.get(egl);view=owner==null?null:owner.get();}
   if(view==null)return;
   synchronized(sessions){s=sessions.get(view);}
   if(s!=null){attempt=s.token;s.render();}
  }catch(Throwable unavailable){if(s!=null && attempt==s.token)s.disable("fallback:FSR indisponivel; imagem original restaurada");}
 }
 private static final class Session implements SurfaceHolder.Callback,View.OnLayoutChangeListener {
  final WeakReference<FREContext> context;final WeakReference<SurfaceView> source;
  volatile boolean enabled;volatile Surface targetSurface;volatile int width,height;
  FrameLayout root;SurfaceView output;String vertex,easu,rcas;volatile long token;long start,lastSignal;volatile long frames;volatile int request;
  Resources resources;
  Session(FREContext c,SurfaceView view){context=new WeakReference<FREContext>(c);source=new WeakReference<SurfaceView>(view);}
  void signal(String status){FREContext c=context.get();if(c!=null)try{c.dispatchStatusEventAsync("fsr",request+":"+status);}catch(Throwable ignored){}}
  void enable()throws Exception {
   if(enabled)return;
   FREContext c=context.get();SurfaceView view=source.get();if(c==null || view==null)throw new IllegalStateException();
   Activity activity=c.getActivity();View decor=activity.getWindow().getDecorView();if(!(decor instanceof FrameLayout))throw new IllegalStateException();
   if(vertex==null){vertex=asset(activity,"fsr/shaders/fullscreen.vert");easu=asset(activity,"fsr/shaders/easu.frag");rcas=asset(activity,"fsr/shaders/rcas.frag");}
   root=(FrameLayout)decor;output=new SurfaceView(activity);output.setZOrderOnTop(true);output.getHolder().setFormat(PixelFormat.TRANSLUCENT);
   output.setClickable(false);output.setFocusable(false);output.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
   output.getHolder().addCallback(this);root.addView(output,new FrameLayout.LayoutParams(1,1,Gravity.TOP|Gravity.LEFT));
   view.addOnLayoutChangeListener(this);enabled=true;frames=0;start=lastSignal=System.nanoTime();position();signal("pending");
   final long attempt=++token;
   output.postDelayed(new Runnable(){public void run(){if(enabled && token==attempt && frames==0)disable("fallback:Nenhum quadro EGL14 recebido");}},4000);
  }
  void position(){SurfaceView view=source.get();if(view==null || root==null || output==null)return;
   int[] a=new int[2],b=new int[2];view.getLocationInWindow(a);root.getLocationInWindow(b);
   FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(view.getWidth(),view.getHeight(),Gravity.TOP|Gravity.LEFT);p.leftMargin=a[0]-b[0];p.topMargin=a[1]-b[1];output.setLayoutParams(p);
  }
  public void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob){if(enabled)position();}
  public void surfaceCreated(SurfaceHolder h){if(output!=null && h==output.getHolder())targetSurface=h.getSurface();}
  public void surfaceChanged(SurfaceHolder h,int format,int w,int hgt){if(output!=null && h==output.getHolder()){width=w;height=hgt;targetSurface=h.getSurface();}}
  public void surfaceDestroyed(SurfaceHolder h){if(output!=null && h==output.getHolder()){targetSurface=null;width=height=0;}}
  void disable(final String reason){enabled=false;token++;signal(reason);
   final SurfaceView retiring=output;final FrameLayout retiringRoot=root;
   FREContext c=context.get();if(c==null)return;
   try{c.getActivity().runOnUiThread(new Runnable(){public void run(){
    // A rapid re-enable must not let obsolete cleanup remove the new view or its listener.
    if(retiring!=null){retiring.getHolder().removeCallback(Session.this);if(retiringRoot!=null)retiringRoot.removeView(retiring);}
    if(!enabled && output==retiring){SurfaceView view=source.get();if(view!=null)view.removeOnLayoutChangeListener(Session.this);output=null;root=null;targetSurface=null;width=height=0;}
   }});}catch(Throwable ignored){}
  }
  void render()throws Exception {
   final long attempt=token;
   EGLDisplay display=EGL14.eglGetCurrentDisplay();EGLContext context=EGL14.eglGetCurrentContext();
   if(context.equals(EGL14.EGL_NO_CONTEXT) || display.equals(EGL14.EGL_NO_DISPLAY))return;
   if(!enabled){if(resources!=null){resources.release(display,context);resources=null;}return;}
   Surface target=targetSurface;int w=width,h=height;if(target==null || !target.isValid() || w<=0 || h<=0)return;
   EGLSurface draw=EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW),read=EGL14.eglGetCurrentSurface(EGL14.EGL_READ);
   int[] size=new int[2];if(!EGL14.eglQuerySurface(display,draw,EGL14.EGL_WIDTH,size,0) || !EGL14.eglQuerySurface(display,draw,EGL14.EGL_HEIGHT,size,1))throw new IllegalStateException();
   int iw=size[0],ih=size[1];if(iw<=0 || ih<=0 || iw>w || ih>h || (long)w*h>8_388_608L)throw new IllegalStateException();
   String version=glGetString(GL_VERSION);if(version==null || !version.contains("OpenGL ES 3"))throw new IllegalStateException("GLES3 required");
   State saved=new State();boolean restored=false;
   try{
    if(resources!=null && !resources.context.equals(context)){resources.release(display,context);resources=null;}
    if(resources==null)resources=new Resources(context,vertex,easu,rcas);
    resources.prepare(display,draw,target,iw,ih,w,h);
    // Source is AIR's completed default framebuffer, copied entirely GPU-to-GPU.
    glBindFramebuffer(GL_READ_FRAMEBUFFER,0);int oldRead=integer(GL_READ_BUFFER);glReadBuffer(GL_BACK);
    try{glActiveTexture(GL_TEXTURE0);glBindSampler(0,0);glBindTexture(GL_TEXTURE_2D,resources.input);
     glCopyTexSubImage2D(GL_TEXTURE_2D,0,0,0,0,0,iw,ih);
    }finally{glReadBuffer(oldRead);}
    if(!EGL14.eglMakeCurrent(display,resources.output,resources.output,context))throw new IllegalStateException("Output EGL unavailable");
    // Only the extra output surface is asynchronous; the original AIR surface keeps its cadence.
    if(!resources.pacingSet){if(!EGL14.eglSwapInterval(display,0))throw new IllegalStateException("Output swap interval unavailable");resources.pacingSet=true;}
    glDisable(GL_SCISSOR_TEST);glDisable(GL_BLEND);glDisable(GL_DEPTH_TEST);glDisable(GL_STENCIL_TEST);glDisable(GL_CULL_FACE);
    glDisable(GL_RASTERIZER_DISCARD);glDisable(GL_SAMPLE_ALPHA_TO_COVERAGE);glDisable(GL_SAMPLE_COVERAGE);glColorMask(true,true,true,true);
    glBindVertexArray(resources.vao);glViewport(0,0,w,h);
    glBindFramebuffer(GL_DRAW_FRAMEBUFFER,resources.fbo);resources.draw(resources.easu,resources.input,iw,ih,w,h);
    glBindFramebuffer(GL_DRAW_FRAMEBUFFER,0);resources.draw(resources.rcas,resources.intermediate,w,h,w,h);
    if(!enabled || token!=attempt)return;
    if(!EGL14.eglSwapBuffers(display,resources.output))throw new IllegalStateException("Output presentation failed");
    frames++;long now=System.nanoTime();
    if(frames==1 || now-lastSignal>=1_000_000_000L){double fps=frames*1e9/Math.max(1,now-start);signal("active:"+iw+"x"+ih+":"+w+"x"+h+":"+(frames==1?"medindo":String.format(Locale.ROOT,"%.1f",fps)));lastSignal=now;}
   }finally{
    restored=EGL14.eglMakeCurrent(display,draw,read,context);
    if(restored)saved.restore();
    else{enabled=false;signal("fallback:Contexto grafico interrompido");}
   }
   if(!restored)throw new IllegalStateException("Cannot restore AIR EGL");
  }
 }
 private static String asset(Activity a,String name)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=a.getAssets().open(name)){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)bytes.write(b,0,n);}return new String(bytes.toByteArray(),"UTF-8");}
 private static int integer(int name){int[] value=new int[1];glGetIntegerv(name,value,0);return value[0];}
 private static final class State {
  final int program=integer(GL_CURRENT_PROGRAM),vao=integer(GL_VERTEX_ARRAY_BINDING),draw=integer(GL_DRAW_FRAMEBUFFER_BINDING),read=integer(GL_READ_FRAMEBUFFER_BINDING),active=integer(GL_ACTIVE_TEXTURE);
  final int[] viewport=new int[4],scissor=new int[4],mask=new int[4];
  final int[] caps={GL_SCISSOR_TEST,GL_BLEND,GL_DEPTH_TEST,GL_STENCIL_TEST,GL_CULL_FACE,GL_RASTERIZER_DISCARD,GL_SAMPLE_ALPHA_TO_COVERAGE,GL_SAMPLE_COVERAGE};
  final boolean[] enabled=new boolean[caps.length];final int texture,sampler;
  State(){glGetIntegerv(GL_VIEWPORT,viewport,0);glGetIntegerv(GL_SCISSOR_BOX,scissor,0);glGetIntegerv(GL_COLOR_WRITEMASK,mask,0);
   for(int i=0;i<caps.length;i++)enabled[i]=glIsEnabled(caps[i]);glActiveTexture(GL_TEXTURE0);texture=integer(GL_TEXTURE_BINDING_2D);sampler=integer(GL_SAMPLER_BINDING);
  }
  void restore(){glUseProgram(program);glBindVertexArray(vao);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw);glBindFramebuffer(GL_READ_FRAMEBUFFER,read);
   glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);glScissor(scissor[0],scissor[1],scissor[2],scissor[3]);glColorMask(mask[0]!=0,mask[1]!=0,mask[2]!=0,mask[3]!=0);
   for(int i=0;i<caps.length;i++){if(enabled[i])glEnable(caps[i]);else glDisable(caps[i]);}
   glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture);glBindSampler(0,sampler);glActiveTexture(active);
  }
 }
 private static final class Resources {
  final EGLContext context;int easu,rcas,input,intermediate,fbo,vao;int iw,ih,ow,oh;EGLSurface output=EGL14.EGL_NO_SURFACE;boolean pacingSet;Surface target;EGLDisplay display;
  Resources(EGLContext c,String vertex,String e,String r){context=c;
   try{easu=program(vertex,e);rcas=program(vertex,r);int[] ids=new int[2];glGenTextures(2,ids,0);input=ids[0];intermediate=ids[1];glGenFramebuffers(1,ids,0);fbo=ids[0];glGenVertexArrays(1,ids,0);vao=ids[0];
    if(input==0 || intermediate==0 || fbo==0 || vao==0)throw new IllegalStateException();
   }catch(Throwable error){release(EGL14.eglGetCurrentDisplay(),c);throw error;}
  }
  void prepare(EGLDisplay d,EGLSurface original,Surface s,int inW,int inH,int outW,int outH){
   display=d;
   if(!s.equals(target) || output.equals(EGL14.EGL_NO_SURFACE)){
    if(!output.equals(EGL14.EGL_NO_SURFACE))EGL14.eglDestroySurface(d,output);
    int[] id=new int[1],count=new int[1];EGLConfig[] configs=new EGLConfig[1];
    if(!EGL14.eglQuerySurface(d,original,EGL14.EGL_CONFIG_ID,id,0) || !EGL14.eglChooseConfig(d,new int[]{EGL14.EGL_CONFIG_ID,id[0],EGL14.EGL_NONE},0,configs,0,1,count,0) || count[0]!=1)throw new IllegalStateException();
    int[] minimum=new int[1];if(!EGL14.eglGetConfigAttrib(d,configs[0],EGL14.EGL_MIN_SWAP_INTERVAL,minimum,0) || minimum[0]>0)throw new IllegalStateException("Extra surface would add vsync blocking");
    pacingSet=false;output=EGL14.eglCreateWindowSurface(d,configs[0],s,new int[]{EGL14.EGL_NONE},0);if(output.equals(EGL14.EGL_NO_SURFACE))throw new IllegalStateException();target=s;
   }
   if(inW==iw && inH==ih && outW==ow && outH==oh)return;
   if(outW>integer(GL_MAX_TEXTURE_SIZE) || outH>integer(GL_MAX_TEXTURE_SIZE))throw new IllegalStateException();
   glActiveTexture(GL_TEXTURE0);glBindSampler(0,0);glBindFramebuffer(GL_DRAW_FRAMEBUFFER,fbo);
   allocate(input,inW,inH);glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,input,0);
   if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Input storage unavailable");
   allocate(intermediate,outW,outH);glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER,GL_COLOR_ATTACHMENT0,GL_TEXTURE_2D,intermediate,0);
   if(glCheckFramebufferStatus(GL_DRAW_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("FSR storage unavailable");
   iw=inW;ih=inH;ow=outW;oh=outH;
  }
  void allocate(int texture,int w,int h){glBindTexture(GL_TEXTURE_2D,texture);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
   // AIR may leave a pixel-unpack buffer bound; client null must mean no initial upload.
   int unpack=integer(GL_PIXEL_UNPACK_BUFFER_BINDING);glBindBuffer(GL_PIXEL_UNPACK_BUFFER,0);
   try{glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA8,w,h,0,GL_RGBA,GL_UNSIGNED_BYTE,(java.nio.Buffer)null);}finally{glBindBuffer(GL_PIXEL_UNPACK_BUFFER,unpack);}
  }
  void draw(int program,int texture,int inW,int inH,int outW,int outH){glUseProgram(program);glActiveTexture(GL_TEXTURE0);glBindTexture(GL_TEXTURE_2D,texture);glUniform1i(glGetUniformLocation(program,"sourceImage"),0);glUniform2f(glGetUniformLocation(program,"inputSize"),inW,inH);glUniform2f(glGetUniformLocation(program,"outputSize"),outW,outH);glDrawArrays(GL_TRIANGLES,0,3);}
  void release(EGLDisplay d,EGLContext current){if(!output.equals(EGL14.EGL_NO_SURFACE) && display!=null)EGL14.eglDestroySurface(display,output);output=EGL14.EGL_NO_SURFACE;target=null;
   if(context.equals(current)){if(easu!=0)glDeleteProgram(easu);if(rcas!=0)glDeleteProgram(rcas);glDeleteTextures(2,new int[]{input,intermediate},0);glDeleteFramebuffers(1,new int[]{fbo},0);glDeleteVertexArrays(1,new int[]{vao},0);}easu=rcas=input=intermediate=fbo=vao=0;
  }
 }
 private static int shader(int type,String source){int shader=glCreateShader(type);glShaderSource(shader,source);glCompileShader(shader);int[] ok=new int[1];glGetShaderiv(shader,GL_COMPILE_STATUS,ok,0);if(ok[0]!=1){String error=glGetShaderInfoLog(shader);glDeleteShader(shader);throw new IllegalStateException(error);}return shader;}
 private static int program(String vertex,String fragment){int v=0,f=0,p=0;try{v=shader(GL_VERTEX_SHADER,vertex);f=shader(GL_FRAGMENT_SHADER,fragment);p=glCreateProgram();glAttachShader(p,v);glAttachShader(p,f);glLinkProgram(p);int[] ok=new int[1];glGetProgramiv(p,GL_LINK_STATUS,ok,0);if(ok[0]!=1)throw new IllegalStateException(glGetProgramInfoLog(p));return p;}catch(Throwable error){if(p!=0)glDeleteProgram(p);throw error;}finally{if(v!=0)glDeleteShader(v);if(f!=0)glDeleteShader(f);}}
}
