package br.davi.narutoair.portal;
import java.nio.file.*;
import java.util.*;
public final class OfficialEntryTest {
 public static void main(String[] args)throws Exception {
  byte[] original=Files.readAllBytes(Paths.get(args[0]));
  EntryCompatibility.Result result=EntryCompatibility.adapt(original);
  if(result.securityReferences!=1 || result.loaderReferences!=1 || result.resourceReferences!=1 || result.pageReferences!=1 || result.menuGuards!=1)
   throw new AssertionError("Official entry class references differ from the analyzed client");
  if(!Arrays.equals(original,Files.readAllBytes(Paths.get(args[0]))))throw new AssertionError("Original changed");
  if(EntryCompatibility.adapt(result.bytes).references!=0)throw new AssertionError("Not idempotent");
  Files.write(Paths.get(args[1]),result.bytes);
  System.out.println("PASS: original user-supplied entry; Security=1, Loader=1, URLLoader=1, ExternalInterface=1, Menu=1; bounded import and idempotence; bytes="+result.bytes.length);
 }
}
