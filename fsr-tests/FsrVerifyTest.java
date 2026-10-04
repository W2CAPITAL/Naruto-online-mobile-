public class FsrVerifyTest {public static void main(String[] a)throws Exception {
 Class<?> type=Class.forName("com.adobe.air.FlashEGL14",false,FsrVerifyTest.class.getClassLoader());
 if(type.getDeclaredMethod("SwapEGLBuffers")==null)throw new AssertionError();
 type.getDeclaredMethods();System.out.println("PASS: JVM -Xverify:all accepts transformed AIR EGL14 with production renderer");
}}
