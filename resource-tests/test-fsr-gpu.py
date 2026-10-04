"""Real GLES shader execution on a surfaceless EGL implementation. Not an Android performance benchmark."""
from pathlib import Path
import ctypes as C, json, math, os, re
os.environ.setdefault('EGL_PLATFORM','surfaceless')
root=Path(__file__).resolve().parent.parent
egl=C.CDLL('libEGL.so.1');egl.eglGetProcAddress.restype=C.c_void_p;egl.eglGetProcAddress.argtypes=[C.c_char_p]
def proc(name,ret,*args):
    address=egl.eglGetProcAddress(name.encode());assert address,name
    return C.CFUNCTYPE(ret,*args)(address)
i=C.c_int;u=C.c_uint;p=C.c_void_p;f=C.c_float
get_display=proc('eglGetPlatformDisplayEXT',p,u,p,p)
d=get_display(0x31DD,None,None);assert d
init=proc('eglInitialize',u,p,p,p);assert init(d,None,None)
assert proc('eglBindAPI',u,u)(0x30A0)
attrs=(i*13)(0x3033,1,0x3040,0x40,0x3024,8,0x3023,8,0x3022,8,0x3021,8,0x3038)
config=p();count=i();assert proc('eglChooseConfig',u,p,p,p,i,p)(d,attrs,C.byref(config),1,C.byref(count)) and count.value
ctx=proc('eglCreateContext',p,p,p,p,p)(d,config,None,(i*3)(0x3098,3,0x3038));assert ctx
surface=proc('eglCreatePbufferSurface',p,p,p,p)(d,config,(i*5)(0x3057,128,0x3056,128,0x3038));assert surface
assert proc('eglMakeCurrent',u,p,p,p,p)(d,surface,surface,ctx)
gl_string=proc('glGetString',C.c_char_p,u)
print(gl_string(0x1F02).decode(),gl_string(0x1F01).decode())
def shader(kind,source):
    sid=proc('glCreateShader',u,u)(kind);raw=C.c_char_p(source.encode())
    proc('glShaderSource',None,u,i,p,p)(sid,1,C.byref(raw),None);proc('glCompileShader',None,u)(sid)
    ok=i();proc('glGetShaderiv',None,u,u,p)(sid,0x8B81,C.byref(ok))
    if not ok.value:
        log=C.create_string_buffer(32768);proc('glGetShaderInfoLog',None,u,i,p,p)(sid,len(log),None,log);raise AssertionError(log.value.decode())
    return sid
vertex=(root/'fsr/shaders/fullscreen.vert').read_text()
def program(fragment):
    vs=shader(0x8B31,vertex.replace('#version 300 es','#version 310 es') if '#version 310 es' in fragment else vertex);fs=shader(0x8B30,fragment);pid=proc('glCreateProgram',u)()
    attach=proc('glAttachShader',None,u,u);attach(pid,vs);attach(pid,fs);proc('glLinkProgram',None,u)(pid)
    ok=i();proc('glGetProgramiv',None,u,u,p)(pid,0x8B82,C.byref(ok));assert ok.value,'link failed'
    proc('glDeleteShader',None,u)(vs);proc('glDeleteShader',None,u)(fs);return pid
easu=(root/'fsr/shaders/easu.frag').read_text();rcas=(root/'fsr/shaders/rcas.frag').read_text()
e=program(easu);r=program(rcas)
# Compare GLES 3.0 gather adapter against native gather on the same unmodified AMD FP32 kernels.
reference=easu.replace('#version 300 es','#version 310 es')
for letter,channel in [('R',0),('G',1),('B',2)]:
    reference=reference.replace(f'return gatherChannel(p,{channel});',f'return textureGather(sourceImage,p,{channel});')
ref=program(reference)
gen_tex=proc('glGenTextures',None,i,p);bind_tex=proc('glBindTexture',None,u,u);param=proc('glTexParameteri',None,u,u,i)
image=proc('glTexImage2D',None,u,i,i,i,i,i,u,u,p)
def texture(w,h,data=None):
    tid=u();gen_tex(1,C.byref(tid));bind_tex(0x0DE1,tid.value)
    for n,v in [(0x2801,0x2600),(0x2800,0x2600),(0x2802,0x812F),(0x2803,0x812F)]:param(0x0DE1,n,v)
    buf=C.create_string_buffer(data) if data is not None else None
    image(0x0DE1,0,0x8058,w,h,0,0x1908,0x1401,buf);return tid.value
fbo=u();proc('glGenFramebuffers',None,i,p)(1,C.byref(fbo));vao=u();proc('glGenVertexArrays',None,i,p)(1,C.byref(vao));proc('glBindVertexArray',None,u)(vao.value)
bind_fbo=proc('glBindFramebuffer',None,u,u);attach=proc('glFramebufferTexture2D',None,u,u,u,u,i)
def target(tid):
    bind_fbo(0x8D40,fbo.value);attach(0x8D40,0x8CE0,0x0DE1,tid,0)
    assert proc('glCheckFramebufferStatus',u,u)(0x8D40)==0x8CD5
def dispatch(pid,src,iw,ih,ow,oh):
    proc('glViewport',None,i,i,i,i)(0,0,ow,oh);proc('glUseProgram',None,u)(pid);bind_tex(0x0DE1,src)
    loc=proc('glGetUniformLocation',i,u,C.c_char_p)
    proc('glUniform1i',None,i,i)(loc(pid,b'sourceImage'),0)
    uniform=proc('glUniform2f',None,i,f,f)
    uniform(loc(pid,b'inputSize'),iw,ih);uniform(loc(pid,b'outputSize'),ow,oh)
    proc('glDrawArrays',None,u,i,i)(0x0004,0,3)
def read(w,h):
    pixels=(C.c_ubyte*(w*h*4))();proc('glReadPixels',None,i,i,i,i,u,u,p)(0,0,w,h,0x1908,0x1401,pixels);return bytes(pixels)
checks=[]
for iw,ih,ow,oh in [(16,12,20,15),(48,28,60,35),(31,19,39,24)]:
    data=bytes(v for y in range(ih) for x in range(iw) for v in (((x//4+y//3)%2)*220,round(x*255/max(1,iw-1)),round(y*255/max(1,ih-1)),255))
    source=texture(iw,ih,data);scaled=texture(ow,oh);final=texture(ow,oh)
    target(scaled);dispatch(e,source,iw,ih,ow,oh);actual=read(ow,oh)
    target(final);dispatch(ref,source,iw,ih,ow,oh);expected=read(ow,oh)
    error=max(abs(a-b) for a,b in zip(actual,expected));assert error<=1,(iw,ih,error)
    target(final);dispatch(r,scaled,ow,oh,ow,oh);result=read(ow,oh)
    assert all(result[n]==255 for n in range(3,len(result),4))
    assert result[2]<result[-2],'vertical orientation reversed'
    assert result[1]<result[-3],'horizontal orientation reversed'
    assert result!=actual,'RCAS should change textured fixture'
    # GPU copy path used by the AIR hook: no readback involved in the production algorithm.
    copied=texture(iw,ih);target(source);bind_tex(0x0DE1,copied)
    proc('glCopyTexSubImage2D',None,u,i,i,i,i,i,i,i)(0x0DE1,0,0,0,0,0,iw,ih)
    target(scaled);dispatch(e,copied,iw,ih,ow,oh);assert read(ow,oh)==actual
    checks.append({'input':[iw,ih],'output':[ow,oh],'max_reference_error_8bit':error,'gpu_copy_matches':True})
for rgba in [(0,0,0,255),(255,255,255,255),(64,128,192,255)]:
    source=texture(8,6,bytes(rgba)*48);scaled=texture(10,8);final=texture(10,8)
    target(scaled);dispatch(e,source,8,6,10,8);target(final);dispatch(r,scaled,10,8,10,8);result=read(10,8)
    assert max(abs(result[n]-rgba[n%4]) for n in range(len(result)))<=2,(rgba,result[:16])
assert proc('glGetError',u)()==0,'GL error after all tests'
report={'backend':gl_string(0x1F01).decode(),'gles':gl_string(0x1F02).decode(),'kernels':'unmodified AMD FSR 1 FP32 EASU/RCAS','fixtures':checks,'constant_colors_and_black_borders':True,'android_device_tested':False,'frame_generation':False}
(root/'fsr/GPU_VALIDATION.json').write_text(json.dumps(report,indent=2)+'\n')
print('PASS: actual GLES compile/link/EASU/RCAS, reference gather parity, corners/orientation, constants, GPU framebuffer copy')
