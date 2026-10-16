"""Sign verified APK/AAB data with official tools; never execute application code."""
import argparse,base64,hashlib,json,os,re,shutil,subprocess,tempfile,zipfile
from pathlib import Path
from verify_artifact import Refused,verify,bounded_directory
FILES=['WMAID-phone-unsigned.apk','WMAID-watch-unsigned.apk','WMAID-phone-unsigned.aab','WMAID-watch-unsigned.aab']
ROOT=Path(os.environ.get('RUNNER_TEMP','/tmp'))/'wmaid-artifact-signing'
TOOLS=Path('/tmp/wmaid-sign-tools')
class Held(ValueError):pass
def require(ok,reason):
 if not ok:raise Held(reason)
def digest(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  for data in iter(lambda:f.read(65536),b''):h.update(data)
 return h.hexdigest()
def run(args,env=None):
 p=subprocess.run([str(x) for x in args],env=env,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,timeout=180)
 require(p.returncode==0,'official tool verification/signing failed');return p.stdout
def identity(proof,context):
 expected={**context,'head_sha':context['sha']};del expected['sha']
 require(context['repository']=='Oxiom-Systems/watch-my-ai-doing' and context['repository_id']==1317333090 and context['branch']=='main','literal main source')
 require(all(proof.get(k)==v for k,v in expected.items()),'current source/producer identity')
def context():
 return dict(repository=os.environ['GITHUB_REPOSITORY'],repository_id=int(os.environ['GITHUB_REPOSITORY_ID']),sha=os.environ['GITHUB_SHA'],branch=os.environ['GITHUB_REF_NAME'],run_id=int(os.environ['GITHUB_RUN_ID']),run_attempt=int(os.environ['GITHUB_RUN_ATTEMPT']))
def check_manifest(actual,text):
 require(set(actual)==set(FILES),'exact four unsigned outputs');parsed={}
 for line in text.splitlines():
  match=re.fullmatch(r'([0-9a-f]{64})  (?:\./)?(WMAID-(?:phone|watch)-unsigned\.(?:apk|aab))',line);require(match is not None,'fixed checksum manifest')
  require(match[2] not in parsed,'duplicate checksum');parsed[match[2]]=match[1]
 require(parsed==actual,'unsigned checksum bytes');return actual
SIG=re.compile(r'^META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))$',re.I)
def payload(path,unsigned=False):
 bounded_directory(path)
 result={};total=0
 with zipfile.ZipFile(path) as z:
  require(len(z.infolist())<=10000,'Android ZIP entry bound')
  for e in z.infolist():
   if e.is_dir():continue
   require(e.filename not in result and e.file_size<=256*1024**2,'Android ZIP entry shape')
   if SIG.fullmatch(e.filename):require(not unsigned,'input already JAR signed');continue
   total+=e.file_size;require(total<=512*1024**2,'Android ZIP expanded bound')
   h=hashlib.sha256()
   with z.open(e) as f:
    for chunk in iter(lambda:f.read(65536),b''):h.update(chunk)
   result[e.filename]=h.hexdigest()
 require(result,'Android ZIP payload');return result

def prepare(policy,run_id,attempt,artifact_id):
 os.umask(0o077);ROOT.mkdir(mode=0o700);proof=verify(policy,run_id,attempt,artifact_id,output=ROOT/'verified');ctx=context();identity(proof,ctx);require(proof['event']=='workflow_dispatch' and os.environ['GITHUB_EVENT_NAME']=='workflow_dispatch','manual signing source')
 require((run_id,attempt)==(ctx['run_id'],ctx['run_attempt']),'current numeric producer')
 files=ROOT/'verified/files';require({p.name for p in files.iterdir()}==set(FILES+['SHA256SUMS.txt']),'fixed unsigned artifact members')
 hashes={f:digest(files/f) for f in FILES};check_manifest(hashes,(files/'SHA256SUMS.txt').read_text())
 entries={f:payload(files/f,True) for f in FILES}
 for f in FILES[:2]:
  meta=run([TOOLS/'aapt2','dump','badging',files/f]).decode();require("package: name='com.oxiomsystems.wmaid' versionCode='1' versionName='0.1.0'" in meta,'original package/version metadata')
  require(b'APK Sig Block 42' not in (files/f).read_bytes(),'input already APK signed')
 (ROOT/'prepared.json').write_text(json.dumps({'context':ctx,'hashes':hashes,'payloads':entries},sort_keys=True))

def sign():
 plan=json.loads((ROOT/'prepared.json').read_text());identity({**plan['context'],'head_sha':plan['context']['sha']},context())
 files=ROOT/'verified/files';require(all(digest(files/f)==h for f,h in plan['hashes'].items()),'verified bytes changed')
 spec=json.loads(Path(__file__).with_name('tools.json').read_text());require(all(digest(TOOLS/f)==h for f,h in spec['members'].items()),'SDK tool identity')
 jdk=Path(os.environ['JAVA_HOME'])/'bin';out=ROOT/'signed';out.mkdir(mode=0o700);keydir=ROOT/'key';keydir.mkdir(mode=0o700)
 try:
  raw=os.environ['KEYSTORE_BASE64'];require(len(raw)<=2*1024**2,'keystore size bound');key=keydir/'release.jks';key.write_bytes(base64.b64decode(re.sub(r'[\r\n\t ]','',raw),validate=True));key.chmod(0o600)
  alias=os.environ['WMAID_KEY_ALIAS'];require(re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9 _.\-]{0,127}',alias) is not None,'keystore alias')
  cert=run([jdk/'keytool','-exportcert','-keystore',key,'-alias',alias,'-storepass:env','WMAID_KEYSTORE_PASSWORD']);fingerprint=hashlib.sha256(cert).hexdigest()
  for name in FILES:
   source=files/name;dest=out/name.replace('-unsigned','')
   if name.endswith('.apk'):
    aligned=keydir/name;run([TOOLS/'zipalign','-P','16','-f','4',source,aligned])
    run([jdk/'java','-jar',TOOLS/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias',alias,'--ks-pass','env:WMAID_KEYSTORE_PASSWORD','--key-pass','env:WMAID_KEY_PASSWORD','--v4-signing-enabled','false','--out',dest,aligned])
    result=run([jdk/'java','-jar',TOOLS/'lib/apksigner.jar','verify','--print-certs',dest]).decode();require('certificate SHA-256 digest: '+fingerprint in result,'APK signer certificate')
    run([TOOLS/'zipalign','-c','-P','16','4',dest])
   else:
    run([jdk/'jarsigner','-keystore',key,'-storepass:env','WMAID_KEYSTORE_PASSWORD','-keypass:env','WMAID_KEY_PASSWORD','-digestalg','SHA-256','-signedjar',dest,source,alias])
    run([jdk/'java','-cp',TOOLS,'BundleVerifier',dest,fingerprint])
   require(payload(dest)==plan['payloads'][name],'Android application/version payload changed')
  require({p.name for p in out.iterdir()}=={f.replace('-unsigned','') for f in FILES},'exact four signed outputs')
  hashes={p.name:digest(p) for p in out.iterdir()};(out/'ALL_SHA256SUMS.txt').write_text(''.join(h+'  '+n+'\n' for n,h in hashes.items()))
  (out/'SHA256SUMS.txt').write_text(''.join(hashes[n]+'  '+n+'\n' for n in ['WMAID-phone.apk','WMAID-watch.apk']))
  (ROOT/'signed-proof.json').write_text(json.dumps({'context':plan['context'],'certificate_sha256':fingerprint,'hashes':hashes},sort_keys=True))
 finally:shutil.rmtree(keydir)

def release():
 plan=json.loads((ROOT/'signed-proof.json').read_text());identity({**plan['context'],'head_sha':plan['context']['sha']},context());out=ROOT/'signed';require(all(digest(out/n)==h for n,h in plan['hashes'].items()),'signed bytes changed')
 require((out/'SHA256SUMS.txt').read_text()==''.join(plan['hashes'][n]+'  '+n+'\n' for n in ['WMAID-phone.apk','WMAID-watch.apk']),'release checksum bytes')
 gh=Path('/tmp/ci-publish-tools/gh');tag='showcase-'+str(int(os.environ['GITHUB_RUN_NUMBER']));assets=['WMAID-phone.apk','WMAID-watch.apk','SHA256SUMS.txt']
 run([gh,'release','create',tag,*[out/n for n in assets],'--repo','Oxiom-Systems/watch-my-ai-doing','--target',os.environ['GITHUB_SHA'],'--prerelease','--title','WMAID showcase '+os.environ['GITHUB_RUN_NUMBER'],'--notes','Manually published emulator-first showcase APKs. These are not a Play Store production release.'])
 with tempfile.TemporaryDirectory(dir=ROOT) as td:
  for n in assets:run([gh,'release','download',tag,'--repo','Oxiom-Systems/watch-my-ai-doing','--pattern',n,'--dir',td])
  require(all(digest(Path(td)/n)==digest(out/n) for n in assets),'actual release asset readback bytes')

def main(policy):
 p=argparse.ArgumentParser();p.add_argument('mode',choices=['verify','sign','release']);p.add_argument('ids',nargs='*',type=int);args=p.parse_args()
 try:
  if args.mode=='verify':require(len(args.ids)==3,'three numeric IDs');prepare(policy,*args.ids)
  elif args.mode=='sign':require(not args.ids,'fixed sign operation');sign()
  else:require(not args.ids,'fixed release operation');release()
  return 0
 except (Held,Refused,KeyError,ValueError,TypeError,OSError,zipfile.BadZipFile,subprocess.SubprocessError):
  print('Android signing/publication held: source, artifact, tool or signature verification failed.',file=__import__('sys').stderr);return 1
