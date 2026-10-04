package com.adobe.fre;
import java.nio.ByteBuffer;
public class FREByteArray extends FREObject {
    private byte[] bytes;private boolean locked;
    public FREByteArray(byte[] bytes){super(null);this.bytes=bytes;}
    public static FREByteArray newByteArray(long size){if(acquired!=0)throw new IllegalStateException();return new FREByteArray(new byte[(int)size]);}
    public void acquire(){if(locked)throw new IllegalStateException();locked=true;acquired++;}
    public void release(){if(!locked)throw new IllegalStateException();locked=false;acquired--;}
    public long getLength(){if(!locked)throw new IllegalStateException();return bytes.length;}
    public ByteBuffer getBytes(){if(!locked)throw new IllegalStateException();return ByteBuffer.wrap(bytes);}
    public byte[] snapshot(){if(locked)throw new IllegalStateException();return bytes.clone();}
}
