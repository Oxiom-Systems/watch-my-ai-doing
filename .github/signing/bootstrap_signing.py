"""Pinned official Android data-signing tools only; run before signing keys exist."""
import hashlib,json,os,subprocess,urllib.request,zipfile
from pathlib import Path
TOOLS=Path('/tmp/wmaid-sign-tools')
def install():
 spec=json.loads(Path(__file__).with_name('tools.json').read_text());TOOLS.mkdir(mode=0o700)
 archive=TOOLS/'sdk.zip';h=hashlib.sha256();size=0
 with urllib.request.urlopen(spec['url'],timeout=60) as response,archive.open('xb') as out:
  while chunk:=response.read(65536):
   size+=len(chunk)
   if size>spec['bytes']:raise ValueError('SDK size')
   h.update(chunk);out.write(chunk)
 if size!=spec['bytes'] or h.hexdigest()!=spec['sha256']:raise ValueError('SDK checksum')
 with zipfile.ZipFile(archive) as z:
  for name,digest in spec['members'].items():
   data=z.read('android-16/'+name)
   if hashlib.sha256(data).hexdigest()!=digest:raise ValueError('SDK member checksum')
   target=TOOLS/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data);target.chmod(0o700)
 archive.unlink()
 subprocess.run([os.environ['JAVA_HOME']+'/bin/javac','-d',str(TOOLS),str(Path(__file__).with_name('BundleVerifier.java'))],check=True,timeout=30)
if __name__=='__main__':install()
