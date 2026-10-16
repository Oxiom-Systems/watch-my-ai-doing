import java.util.jar.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;
public class BundleVerifier {
 public static void main(String[] args) throws Exception {
  int count=0;
  try(JarFile jar=new JarFile(args[0],true)) {
   var entries=jar.entries(); byte[] buf=new byte[65536];
   while(entries.hasMoreElements()) {
    JarEntry e=entries.nextElement(); if(e.isDirectory())continue;
    try(var in=jar.getInputStream(e)){while(in.read(buf)!=-1){}}
    if(e.getName().startsWith("META-INF/"))continue;
    Certificate[] certs=e.getCertificates();
    if(certs==null||certs.length==0)throw new SecurityException("unsigned bundle entry");
    String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certs[0].getEncoded()));
    if(!sha.equals(args[1]))throw new SecurityException("bundle signer mismatch");
    count++;
   }
  }
  if(count==0)throw new SecurityException("empty bundle");
 }
}
