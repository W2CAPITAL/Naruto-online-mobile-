from pathlib import Path
import os, shutil, subprocess, sys, tempfile, zipfile
root = Path(__file__).resolve().parent
sdk = Path(os.environ["AIR_HOME"]).resolve()
android_api=Path(os.environ.get("ANDROID_API_JAR",str(root.parent/"tooling/android-35.jar"))).resolve()
if not android_api.is_file():raise SystemExit("Defina ANDROID_API_JAR para o android.jar da API 35")
key = Path(os.environ["NARUTO_SIGNING_KEY"]).resolve()
password_file = Path(os.environ["NARUTO_SIGNING_PASSWORD_FILE"]).resolve()
java = shutil.which("java")
env = os.environ.copy()
if sys.platform.startswith("linux"):
    libs = str(sdk / "lib/android/bin/lib64")
    env["LD_LIBRARY_PATH"] = libs + ":" + env.get("LD_LIBRARY_PATH", "")
(sdk / "lib/adt.cfg").write_text("UseLegacyAPK=true\n")
adt = [java, "-Dfile.encoding=UTF-8", "-jar", str(sdk / "lib/adt.jar")]
def run(args, name):
    result = subprocess.run(args, cwd=root, env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (root / name).write_text(result.stdout, encoding="utf-8")
    print(result.stdout)
    if result.returncode:
        raise SystemExit("Etapa falhou: " + name)
run(adt + ["-version"], "sdk-version.log")
# Compile the available transport against compile/test-only Android signatures.
# Only production classes enter the ANE; the original PortalContext stays intact.
with tempfile.TemporaryDirectory(prefix="naruto-transport-") as tmp:
    classes = Path(tmp) / "classes"
    classes.mkdir()
    sources = sorted((root / "test-stubs").rglob("*.java")) + sorted((root / "transport-src").rglob("*.java"))
    lzma = sdk / "lib/lzma-sdk-9.2.jar"
    run([java, "-m", "jdk.compiler/com.sun.tools.javac.Main", "--release", "8", "-cp", str(lzma), "-d", str(classes)] +
        [str(p) for p in sources], "transport-compile.log")
    bridge_classes=Path(tmp)/"bridge"
    bridge_classes.mkdir()
    guard_classes=Path(tmp)/"guard"
    guard_classes.mkdir()
    guard_export="java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED"
    run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","--add-exports",guard_export,"-d",str(guard_classes),
        str(root/"build-tools/PopupGuard.java"),str(root/"build-tools/BrowserBridgePatch.java"),str(root/"build-tools/PortalUxPatch.java"),str(root/"build-tools/ReleasePortalPatch.java"),str(root/"build-tools/FsrRuntimePatch.java")],"popup-guard-compile.log")
    fsr_classes=Path(tmp)/"fsr";fsr_classes.mkdir()
    run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","--release","8","-cp",os.pathsep.join([str(android_api),str(sdk/"lib/android/FlashRuntimeExtensions.jar")]),"-d",str(fsr_classes)]+
        [str(p) for p in sorted((root/"fsr-src").rglob("*.java"))],"fsr-compile.log")
    runtime_jar=sdk/"lib/android/lib/runtimeClasses.jar"
    runtime_original=runtime_jar.read_bytes()
    runtime_class=Path(tmp)/"egl14-original.class";runtime_patched=Path(tmp)/"egl14-fsr.class"
    with zipfile.ZipFile(runtime_jar) as source:runtime_class.write_bytes(source.read("com/adobe/air/FlashEGL14.class"))
    run([java,"--add-exports",guard_export,"-cp",str(guard_classes),"FsrRuntimePatch",str(runtime_class),str(runtime_patched)],"fsr-runtime-patch.log")
    # Retain only the transformed class until package time; never leave the user's SDK modified.
    fsr_runtime_class=runtime_patched.read_bytes()
    popup_name="br/davi/narutoair/portal/PortalContext$1.class"
    original_popup=Path(tmp)/"popup-original.class"
    guarded_popup=Path(tmp)/"popup-guarded.class"
    with zipfile.ZipFile(root/"portal.jar") as source:original_popup.write_bytes(source.read(popup_name))
    run([java,"--add-exports",guard_export,"-cp",str(guard_classes),"PopupGuard",str(original_popup),str(guarded_popup)],"popup-guard.log")
    patched={popup_name:guarded_popup.read_bytes()}
    page_script=Path(tmp)/"browser-page.js"
    page_script.write_text((root/"build-tools/browser-page.js").read_text()+"\n"+(root/"build-tools/login-recovery.js").read_text())
    for mode,name in [("portal","br/davi/narutoair/portal/PortalContext.class"),("interface","br/davi/narutoair/portal/PortalContext$PortalJsBridge.class")]:
        source_class=Path(tmp)/(mode+"-original.class");target_class=Path(tmp)/(mode+"-patched.class")
        with zipfile.ZipFile(root/"portal.jar") as source:source_class.write_bytes(source.read(name))
        run([java,"--add-exports",guard_export,"-cp",str(guard_classes),"BrowserBridgePatch",mode,str(source_class),str(target_class),
             str(page_script)],"browser-"+mode+"-patch.log")
        patched[name]=target_class.read_bytes()
    ux_source=Path(tmp)/"portal-ux-original.class";ux_target=Path(tmp)/"portal-ux-patched.class"
    ux_name="br/davi/narutoair/portal/PortalContext.class"
    ux_source.write_bytes(patched[ux_name])
    run([java,"--add-exports",guard_export,"-cp",str(guard_classes),"PortalUxPatch",str(ux_source),str(ux_target)],"portal-ux-patch.log")
    release_target=Path(tmp)/"portal-release.class"
    run([java,"--add-exports",guard_export,"-cp",str(guard_classes),"ReleasePortalPatch",str(ux_target),str(release_target)],"release-portal-patch.log")
    patched[ux_name]=release_target.read_bytes()
    bridge_base=Path(tmp)/"portal-base.jar"
    with zipfile.ZipFile(root/"portal.jar") as source,zipfile.ZipFile(bridge_base,"w") as base:
        for info in source.infolist():
            if not info.filename.startswith("br/davi/narutoair/portal/EntryCompatibility"):
                base.writestr(info,patched.get(info.filename,source.read(info.filename)))
    bridge_cp=os.pathsep.join([str(bridge_base),str(sdk/"lib/android/FlashRuntimeExtensions.jar"),str(classes)])
    run([java,"-m","jdk.compiler/com.sun.tools.javac.Main","--release","8","-cp",bridge_cp,"-d",str(bridge_classes)]+
        [str(p) for p in sorted((root/"bridge-src").rglob("*.java"))],"bridge-compile.log")
    updated = Path(tmp) / "portal.jar"
    with zipfile.ZipFile(root / "portal.jar") as original, zipfile.ZipFile(updated, "w", zipfile.ZIP_DEFLATED) as jar:
        for info in original.infolist():
            if not info.filename.startswith(("br/davi/narutoair/portal/RuntimeTransport", "br/davi/narutoair/portal/EntryCompatibility", "br/davi/narutoair/portal/MobileMenuCompatibility", "br/davi/narutoair/portal/SoundConstructorCompatibility", "br/davi/narutoair/portal/KaguyaResources", "br/davi/narutoair/portal/StaticAssetCache", "br/davi/narutoair/portal/KaguyaBridge",
                "br/davi/narutoair/portal/MobilePortalContext", "br/davi/narutoair/portal/AdaptSwfBytes",
                "br/davi/narutoair/portal/BrowserPageBridge", "br/davi/narutoair/portal/MobileInputBridge", "br/davi/narutoair/portal/PortalHealth", "br/davi/narutoair/portal/PortalLoginRecovery",
                "br/davi/narutoair/portal/PortalAccess", "br/davi/narutoair/portal/PortalExtension", "br/davi/narutoair/portal/PortalSession", "br/davi/narutoair/portal/NativeFloatingButton", "br/davi/narutoair/portal/GraphicsBridge", "br/davi/narutoair/portal/FsrBridge", "br/davi/narutoair/fsr/", "br/davi/narutoair/portal/WindowRefresh", "br/davi/narutoair/portal/PortalBranding", "SevenZip/")):
                jar.writestr(info,patched.get(info.filename,original.read(info.filename)))
        for p in sorted(classes.rglob("*.class")):
            name = p.relative_to(classes).as_posix()
            if name.startswith(("br/davi/narutoair/portal/RuntimeTransport", "br/davi/narutoair/portal/EntryCompatibility", "br/davi/narutoair/portal/MobileMenuCompatibility", "br/davi/narutoair/portal/SoundConstructorCompatibility", "br/davi/narutoair/portal/KaguyaResources", "br/davi/narutoair/portal/StaticAssetCache", "br/davi/narutoair/portal/KaguyaBridge")):
                jar.writestr(name, p.read_bytes())
        for p in sorted(bridge_classes.rglob("*.class")):
            jar.writestr(p.relative_to(bridge_classes).as_posix(),p.read_bytes())
        for p in sorted(fsr_classes.rglob("*.class")):
            jar.writestr(p.relative_to(fsr_classes).as_posix(),p.read_bytes())
        with zipfile.ZipFile(lzma) as dependency:
            for info in dependency.infolist():
                if info.filename.startswith(("SevenZip/Compression/LZMA/Decoder", "SevenZip/Compression/RangeCoder/Decoder",
                    "SevenZip/Compression/RangeCoder/BitTreeDecoder", "SevenZip/Compression/LZ/OutWindow",
                    "SevenZip/Compression/LZMA/Base")) or info.filename == "SevenZip/7zC.txt":
                    jar.writestr(info, dependency.read(info.filename))
    with zipfile.ZipFile(updated) as verified:
        names=verified.namelist()
        if len(names)!=len(set(names)):raise SystemExit("Classes nativas duplicadas no JAR")
    shutil.copyfile(updated, root / "portal.jar")
run([java,"-Dflexlib="+str(sdk/"frameworks"),"-jar",str(sdk/"lib/compc-cli.jar"),"+configname=air",
    "-source-path="+str(root/"wrapper-src"),"-include-classes=br.davi.narutoair.portal.PortalMarker",
    "-output="+str(root/"NativePortal.swc")],"wrapper-compile.log")
run([java, "-Dflexlib="+str(sdk / "frameworks"), "-jar", str(sdk / "lib/mxmlc-cli.jar"),
    "+configname=air", str(root / "src/NarutoAir.as"), "-debug=false", "-output="+str(root / "NarutoAir.swf")], "compile.log")
with __import__("zipfile").ZipFile(root / "NativePortal.swc") as archive:
    (root / "library.swf").write_bytes(archive.read("library.swf"))
(root / "extensions").mkdir(exist_ok=True)
run(adt + ["-package", "-target", "ane", str(root / "extensions/NativePortal.ane"),
    str(root / "extension.xml"), "-swc", str(root / "NativePortal.swc"), "-platform", "Android-ARM64",
    "-platformoptions", str(root / "platform.xml"), "-C", str(root), "portal.jar", "library.swf"], "extension.log")
with tempfile.TemporaryDirectory(prefix="naruto-apk-") as temp:
    artifact="NarutoOnline_RealmeC71_1.3.17.apk"
    staged=Path(temp)/artifact
    runtime_jar=sdk/"lib/android/lib/runtimeClasses.jar"
    patched=Path(temp)/"runtime-fsr.jar"
    with zipfile.ZipFile(runtime_jar) as source,zipfile.ZipFile(patched,"w",zipfile.ZIP_DEFLATED) as target:
        for info in source.infolist():target.writestr(info,fsr_runtime_class if info.filename=="com/adobe/air/FlashEGL14.class" else source.read(info.filename))
    try:
        shutil.copyfile(patched,runtime_jar)
        run(adt + ["-package", "-target", "apk-captive-runtime", "-arch", "armv8", "-storetype", "pkcs12",
            "-keystore", str(key), "-storepass", password_file.read_text().strip(),
            str(staged), str(root / "NarutoAir-app.xml"),
            "-C", str(root), "NarutoAir.swf", "icons", "brand", "mods", "fsr", "-extdir", str(root / "extensions")], "package.log")
    finally:runtime_jar.write_bytes(runtime_original)
    # ADT can print an error while returning zero; validate the actual output.
    with zipfile.ZipFile(staged) as archive:
        if archive.testzip() is not None:raise SystemExit("APK com ZIP invalido")
        if archive.read("assets/NarutoAir.swf")!=(root/"NarutoAir.swf").read_bytes():raise SystemExit("APK com bootstrap incorreto")
    dex_dir=Path(temp)/"dex-check"
    run([java,"-jar",str(sdk/"lib/android/lib/baksmali.jar"),"disassemble","--classes",
         "Lcom/adobe/air/FlashEGL14;,Lbr/davi/narutoair/fsr/FsrRenderer$Session;,Lbr/davi/narutoair/portal/MobilePortalContext;,Lbr/davi/narutoair/portal/PortalContext;","-o",str(dex_dir),str(staged)],"fsr-dex-check.log")
    egl=(dex_dir/"com/adobe/air/FlashEGL14.smali").read_text()
    hook="Lbr/davi/narutoair/fsr/FsrRenderer;->beforeSwap"
    if egl.count(hook)!=1 or egl.count("Lbr/davi/narutoair/fsr/FsrRenderer;->register")!=1:raise SystemExit("APK sem hook FSR exato")
    swap=egl[egl.index(".method public SwapEGLBuffers"):];swap=swap[:swap.index(".end method")]
    if swap.index(hook)>swap.index("Landroid/opengl/EGL14;->eglSwapBuffers"):raise SystemExit("Hook FSR depois do swap")
    compositor=(dex_dir/"br/davi/narutoair/fsr/FsrRenderer$Session.smali").read_text()
    for method in ["glCopyTexSubImage2D","eglMakeCurrent","eglSwapBuffers","eglSwapInterval"]:
        if method not in compositor:raise SystemExit("Compositor FSR incompleto: "+method)
    if "graphicsFSR" not in (dex_dir/"br/davi/narutoair/portal/MobilePortalContext.smali").read_text():raise SystemExit("FSR sem funcao ANE")
    portal=(dex_dir/"br/davi/narutoair/portal/PortalContext.smali").read_text()
    method=portal[portal.index(".method private injectInspector("):];method=method[:method.index(".end method")]
    if "invoke-" in method or "const-string" in method:raise SystemExit("APK ainda injeta LOG no HTML")
    detection=portal[portal.index(".method private detectCloudflareBlock("):];detection=detection[:detection.index(".end method")]
    if "PortalAccess;->inspect" not in detection:raise SystemExit("APK sem detector de bloqueio do portal")
    run([java,"-jar",str(sdk/"lib/android/lib/apksigner.jar"),"verify","--verbose","--print-certs",str(staged)],"signature.log")
    shutil.copyfile(staged,root/artifact)
print("APK gerado pelo ADT. Não modificar depois do empacotamento.")
