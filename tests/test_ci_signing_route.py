import ast,json,re,sys,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.github/signing'))
class SigningRoute(unittest.TestCase):
 def test_unsigned_artifact_name_matches_unchanged_reviewed_verifier_interface(self):
  self.assertIn('name: wmaid-unsigned-${{ github.run_id }}-${{ github.run_attempt }}',(ROOT/'.github/workflows/ci-build-recipe.yml').read_text())
 def test_real_producer_digest_manifest_requires_exact_four_files(self):
  import signer
  actual=json.loads((ROOT/'tests/fixtures/wmaid-unsigned-files.json').read_text());sums=''.join(v+'  ./'+k+'\n' for k,v in actual.items())
  self.assertEqual(signer.check_manifest(actual,sums),actual)
  for change in [{**actual,'install.sh':'a'*64},{**actual,'WMAID-phone-unsigned.apk':'a'*64}]:
   with self.assertRaises(signer.Held):signer.check_manifest(change,sums)
 def test_source_identity_and_manual_main_only_before_secret_access(self):
  import signer
  context={'repository':'Oxiom-Systems/watch-my-ai-doing','repository_id':1317333090,'sha':'a'*40,'branch':'main','run_id':31,'run_attempt':2}
  proof={**context,'head_sha':context['sha']};del proof['sha'];signer.identity(proof,context)
  for key,value in [('repository_id',1),('branch','dev'),('head_sha','b'*40),('run_attempt',1)]:
   with self.subTest(key=key),self.assertRaises(signer.Held):signer.identity({**proof,key:value},context)
 def test_signer_leaf_only_immutable_helpers_and_four_production_secret_names(self):
  import yaml
  d=yaml.safe_load((ROOT/'.github/workflows/ci-sign-recipe.yml').read_text());j=d['jobs']['sign'];self.assertEqual(j['environment'],'production');self.assertEqual(j['runs-on']['group'],'ci-publish-public-ephemeral');self.assertEqual(j['runs-on']['labels'],['ci-linux-publish-v1-r1317333090'])
  self.assertIn('refs/heads/main',j['if']);self.assertIn('workflow_dispatch',j['if']);self.assertIn('github.ref_protected == true',j['if']);self.assertIn("github.repository_id == '1317333090'",j['if'])
  c=j['steps'][0]['with'];self.assertRegex(c['ref'],r'^[a-f0-9]{40}$');self.assertFalse(c['persist-credentials']);self.assertFalse(c['sparse-checkout-cone-mode']);self.assertTrue(all(p.startswith('/.github/signing/') for p in c['sparse-checkout'].splitlines()))
  expected={'WMAID_DEMO_KEYSTORE_BASE64','WMAID_DEMO_KEYSTORE_PASSWORD','WMAID_DEMO_KEY_ALIAS','WMAID_DEMO_KEY_PASSWORD'};self.assertEqual(set(re.findall(r'secrets\.([A-Z_]+)',str(j))),expected)
  secretsteps=[s for s in j['steps'] if 'secrets.' in str(s)];self.assertEqual(len(secretsteps),1);self.assertIn(' sign',secretsteps[0]['run']);self.assertNotIn('gradlew',str(j));self.assertNotIn('setup-gradle',str(j));self.assertNotIn('inherit',str(j))
 def test_release_preserves_manual_optin_tag_and_original_assets(self):
  source=(ROOT/'.github/signing/signer.py').read_text();self.assertIn('showcase-',source);self.assertIn('WMAID-phone.apk',source);self.assertIn('WMAID-watch.apk',source);self.assertIn('SHA256SUMS.txt',source)
  caller=(ROOT/'.github/workflows/manual-build.yml').read_text();self.assertIn('publish_release',caller);self.assertIn('needs: build',caller);self.assertIn('ci-sign-recipe.yml@',caller)
if __name__=='__main__':unittest.main()
