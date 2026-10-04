"""Reproducible GLES 3.0 wrappers around the unmodified MIT AMD FSR 1 FP32 kernels."""
from pathlib import Path
import subprocess, hashlib, json, re
root=Path(__file__).resolve().parent.parent
vendor=root/'fsr'/'upstream'
out=root/'fsr'/'shaders';out.mkdir(parents=True,exist_ok=True)
prefix='''#version 300 es
precision highp float;
precision highp int;
uniform highp sampler2D sourceImage;
uniform vec2 inputSize;
uniform vec2 outputSize;
layout(location=0) out vec4 outputColor;
'''
fetch='''vec4 loadPixel(ivec2 p){return texelFetch(sourceImage,clamp(p,ivec2(0),ivec2(inputSize)-1),0);}
'''
easu='''
// Equivalent GLSL gather ordering, clamped borders; GLES 3.0 needs no textureGather extension.
vec4 gatherChannel(vec2 p,int c){ivec2 q=ivec2(floor(p*inputSize-0.5));
 return vec4(loadPixel(q+ivec2(0,1))[c],loadPixel(q+ivec2(1,1))[c],loadPixel(q+ivec2(1,0))[c],loadPixel(q)[c]);}
vec4 FsrEasuRF(vec2 p){return gatherChannel(p,0);}
vec4 FsrEasuGF(vec2 p){return gatherChannel(p,1);}
vec4 FsrEasuBF(vec2 p){return gatherChannel(p,2);}
void main(){uvec4 c0,c1,c2,c3;FsrEasuCon(c0,c1,c2,c3,inputSize.x,inputSize.y,inputSize.x,inputSize.y,outputSize.x,outputSize.y);
 vec3 rgb;FsrEasuF(rgb,uvec2(gl_FragCoord.xy),c0,c1,c2,c3);outputColor=vec4(clamp(rgb,0.0,1.0),1.0);}
'''
rcas='''
vec4 FsrRcasLoadF(ivec2 p){return loadPixel(p);}
void FsrRcasInputF(inout float r,inout float g,inout float b){}
void main(){uvec4 con;FsrRcasCon(con,0.5);vec3 rgb;FsrRcasF(rgb.r,rgb.g,rgb.b,uvec2(gl_FragCoord.xy),con);
 outputColor=vec4(clamp(rgb,0.0,1.0),1.0);}
'''
for name,kernel,tail in [('easu','FSR_EASU_F',easu),('rcas','FSR_RCAS_F',rcas)]:
 source='#define A_GPU 1\n#define A_GLSL 1\n#define '+kernel+' 1\n#include "ffx_a.h"\n#include "ffx_fsr1.h"\n'
 result=subprocess.run(['cpp','-P','-I',str(vendor),'-'],input=source,text=True,capture_output=True,check=True)
 # Desktop GLSL permits implicit uint conversions that GLES rejects. Kernel arithmetic is unchanged.
 body=result.stdout.replace('con3[2]=con3[3]=0;','con3[2]=con3[3]=0u;').replace('con[2]=0;','con[2]=0u;').replace('con[3]=0;','con[3]=0u;')
 # These two unused utility functions require GLES 3.1; neither EASU nor RCAS calls them.
 body=re.sub(r'^ uint ABfe\([^\n]+\n','',body,flags=re.M)
 body=re.sub(r'^ uint ABfiM\([^\n]+\n','',body,flags=re.M)
 body=re.sub(r'^ uvec2 ARmp(?:Red)?8x8\([^\n]+\n','',body,flags=re.M)
 (out/(name+'.frag')).write_text(prefix+body+fetch+tail)
(out/'fullscreen.vert').write_text('''#version 300 es
precision highp float;
void main(){vec2 p=vec2(float((gl_VertexID<<1)&2),float(gl_VertexID&2));gl_Position=vec4(p*2.0-1.0,0.0,1.0);}
''')
print('FSR 1 EASU + RCAS GLES 3.0 shaders generated from AMD source')
