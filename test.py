from pathlib import Path
import os, subprocess, tempfile
root=Path(__file__).resolve().parent
subprocess.run(["node",str(root/"resource-tests/test-resources.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-audio.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-launch-parameters.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-browser-page.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-session-cookie.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-portal-health.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-portal-access.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-login-recovery.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-mouse-feedback.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-floating-button.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-graphics.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-fsr-controls.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-load-scheduler.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-socket-release.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-socket-diagnostics.mjs")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-fps-hud.mjs")],check=True)
subprocess.run(["python3",str(root/"resource-tests/test-kaguya-assets.py")],check=True)
subprocess.run(["node",str(root/"resource-tests/test-kaguya-controls.mjs")],check=True)
sdk=Path(os.environ["AIR_HOME"]).resolve()
java="java"
with tempfile.TemporaryDirectory(prefix="naruto-tests-") as temp:
    work=Path(temp);classes=work/"classes";classes.mkdir()
    lzma=sdk/"lib/lzma-sdk-9.2.jar"
    sources=list((root/"test-stubs").rglob("*.java"))+list((root/"transport-src").rglob("*.java"))+list((root/"transport-tests").rglob("*.java"))+list((root/"compat-tests/br").rglob("*.java"))
    def run(args): subprocess.run(args,check=True)
    run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","--release","8","-cp",str(lzma),"-d",str(classes)]+[str(p) for p in sources])
    for name in ["BrowserEntry","PlainEntry","AudioTypes"]:
        run([java,"-Dflexlib="+str(sdk/"frameworks"),"-jar",str(sdk/"lib/mxmlc-cli.jar"),"+configname=air",str(root/"compat-tests/fixture"/(name+".as")),"-debug=false","-output="+str(work/(name+".swf"))])
    cp=str(classes)+os.pathsep+str(lzma)
    run([java,"-cp",cp,"br.davi.narutoair.portal.CompatibilityTest",str(work/"BrowserEntry.swf"),str(work/"PlainEntry.swf"),str(work/"adapted.swf")])
    run([java,"-Xmx24m","-cp",cp,"br.davi.narutoair.portal.TransportTest"])
    run([java,"-Xmx24m","-cp",cp,"br.davi.narutoair.portal.KaguyaCacheTest",str(root)])
    bridge=work/"bridge";bridge.mkdir()
    stub_sources=list((root/"bridge-tests/stubs").rglob("*.java"))
    run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","--release","8","-cp",cp,"-d",str(bridge)]+
        [str(p) for p in stub_sources+list((root/"bridge-src").rglob("*.java"))]+[str(root/"bridge-tests/BridgeTest.java"),str(root/"bridge-tests/PageBridgeTest.java"),str(root/"bridge-tests/InputTest.java"),str(root/"bridge-tests/GraphicsTest.java"),str(root/"bridge-tests/BrandingTest.java"),str(root/"bridge-tests/KaguyaBridgeTest.java"),str(root/"bridge-tests/PortalAccessTest.java")])
    run([java,"-cp",str(bridge)+os.pathsep+cp,"BridgeTest",str(work/"BrowserEntry.swf"),str(work/"adapted.swf")])
    run([java,"-cp",str(bridge)+os.pathsep+cp,"PageBridgeTest"])
    run([java,"-cp",str(bridge)+os.pathsep+cp,"InputTest"])
    run([java,"-cp",str(bridge)+os.pathsep+cp,"GraphicsTest"])
    run([java,"-cp",str(bridge)+os.pathsep+cp,"BrandingTest"])
    run([java,"-cp",str(bridge)+os.pathsep+cp,"PortalAccessTest"])
    run([java,"-cp",str(bridge)+os.pathsep+cp,"KaguyaBridgeTest",str(root)])
    guard_export="java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED"
    run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","--add-exports",guard_export,"-cp",str(bridge)+os.pathsep+cp,"-d",str(bridge),
        str(root/"build-tools/PopupGuard.java"),str(root/"build-tools/PopupGuardTest.java"),str(root/"build-tools/BrowserBridgePatch.java"),str(root/"build-tools/CacheModeTest.java"),str(root/"build-tools/PortalUxPatch.java"),str(root/"bridge-tests/PortalUxTest.java"),str(root/"build-tools/ReleasePortalPatch.java"),str(root/"build-tools/ReleasePortalTest.java")])
    run([java,"-Xverify:all","--add-exports",guard_export,"-cp",str(bridge)+os.pathsep+cp,"PopupGuardTest"])
    run([java,"-Xverify:all","--add-exports",guard_export,"-cp",str(bridge)+os.pathsep+cp,"CacheModeTest"])
    run([java,"-Xverify:all","--add-exports",guard_export,"-cp",str(bridge)+os.pathsep+cp,"PortalUxTest"])
    run([java,"-Xverify:all","--add-exports",guard_export,"-cp",str(bridge)+os.pathsep+cp,"ReleasePortalTest"])
    if os.environ.get("FFDEC_JAR"):
        ffdec=str(Path(os.environ["FFDEC_JAR"]).resolve())
        run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","-cp",cp+os.pathsep+ffdec,"-d",str(classes),str(root/"compat-tests/IndependentVerification.java"),str(root/"compat-tests/AudioConstructorVerification.java")])
        run([java,"-cp",cp+os.pathsep+ffdec,"IndependentVerification",str(work/"BrowserEntry.swf"),str(work/"adapted.swf"),str(work/"browser-fws.swf")])
        run([java,"-cp",cp+os.pathsep+ffdec,"AudioConstructorVerification",str(work/"AudioTypes.swf"),str(work/"audio-adapted.swf"),str(work/"audio-fws.swf")])
        run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","-cp",cp+os.pathsep+ffdec,"-d",str(classes),str(root/"compat-tests/AudioAssetVerification.java")])
        run([java,"-cp",cp+os.pathsep+ffdec,"AudioAssetVerification",str(root/"mods/kaguya/sounds")])
        if os.environ.get("NARUTO_OFFICIAL_ENTRY"):
            original=str(Path(os.environ["NARUTO_OFFICIAL_ENTRY"]).resolve())
            run([java,"-cp",cp,"br.davi.narutoair.portal.OfficialEntryTest",original,str(work/"official-adapted.swf")])
            run([java,"-cp",cp+os.pathsep+ffdec,"IndependentVerification",original,str(work/"official-adapted.swf"),str(work/"official-fws.swf")])

    # Compile real production GLES/EGL against Android, then test state and patched runtime bytecode.
    android_api=Path(os.environ.get("ANDROID_API_JAR",str(root.parent/"tooling/android-35.jar"))).resolve()
    fre=sdk/"lib/android/FlashRuntimeExtensions.jar"
    fsr=work/"fsr";fsr.mkdir(); fsr_cp=os.pathsep.join([str(android_api),str(fre)])
    run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","--release","8","-cp",fsr_cp,"-d",str(fsr)]+[str(p) for p in (root/"fsr-src").rglob("*.java")])
    state=work/"fsr-state";state.mkdir()
    run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","-cp",str(fsr)+os.pathsep+fsr_cp,"-d",str(state),str(root/"fsr-tests/stubs/android/opengl/GLES30.java"),str(root/"fsr-tests/FsrStateTest.java"),str(root/"fsr-tests/FsrVerifyTest.java")])
    run([java,"-cp",os.pathsep.join([str(state),str(fsr),str(android_api),str(fre)]),"FsrStateTest"])
    run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","--add-exports",guard_export,"-d",str(bridge),str(root/"build-tools/FsrRuntimePatch.java"),str(root/"build-tools/FsrRuntimePatchTest.java")])
    runtime=work/"runtime-fsr.jar"
    run([java,"--add-exports",guard_export,"-cp",str(bridge),"FsrRuntimePatchTest",str(sdk/"lib/android/lib/runtimeClasses.jar"),str(runtime)])
    run([java,"-Xverify:all","-cp",os.pathsep.join([str(state),str(fsr),str(runtime),str(android_api),str(fre)]),"FsrVerifyTest"])
