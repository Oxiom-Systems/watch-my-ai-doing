"""Exercise the immutable public leaf guard and its disposable build route."""
import ast
import json
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
RECIPE = ROOT / '.github/workflows/ci-build-recipe.yml'
REPOSITORY_ID = 1317333090


class PublicBuildRouteTest(unittest.TestCase):
    def source(self):
        return RECIPE.read_text()

    def test_exact_leaf_guard_matches_owned_source_fixtures(self):
        source = self.source()
        match = re.search(r'(?m)^    if: >-\n((?:      .*\n)+)', source)
        self.assertIsNotNone(match, 'public admission must be guarded inside the immutable leaf job')
        guard = ' '.join(line.strip() for line in match[1].splitlines())
        self.assertIn("github.repository_id == '" + str(REPOSITORY_ID) + "'", guard)
        self.assertIn('github.event.pull_request.head.repo.id == ' + str(REPOSITORY_ID), guard)
        for fixture in json.loads((ROOT / 'tests/ci_public_guard_cases.json').read_text()):
            with self.subTest(case=fixture['name']):
                expression = guard
                for key, value in sorted(fixture['github'].items(), key=lambda item: -len(item[0])):
                    expression = expression.replace(key, repr(value))
                expression = expression.replace('&&', ' and ').replace('||', ' or ')
                expression = re.sub(r'\btrue\b', 'True', expression)
                expression = re.sub(r'\bfalse\b', 'False', expression)
                tree = ast.parse(expression.strip(), mode='eval')
                allowed = (ast.Expression, ast.BoolOp, ast.And, ast.Or, ast.Compare,
                           ast.Eq, ast.Constant, ast.Load)
                self.assertTrue(all(isinstance(node, allowed) for node in ast.walk(tree)))
                self.assertEqual(eval(compile(tree, '<actual-job-guard>', 'eval'), {'__builtins__': {}}), fixture['allowed'])

    def test_public_build_route_is_static_read_only_and_bounded(self):
        source = self.source()
        self.assertIn('group: ci-build-public-ephemeral', source)
        self.assertIn('labels: [ci-linux-cpu-v1-r' + str(REPOSITORY_ID) + ']', source)
        self.assertIn('ref: ${{ github.sha }}', source)
        self.assertIn('persist-credentials: false', source)
        timeout = re.search(r'(?m)^    timeout-minutes: ([0-9]+)$', source)
        self.assertIsNotNone(timeout)
        self.assertGreater(int(timeout[1]), 0)
        self.assertLessEqual(int(timeout[1]), 120)
        for forbidden in ('secrets:', 'secrets.', ': write', 'inputs.', 'pull_request_target',
                          'repository_dispatch', '--privileged', '--network host', '--volume', '--mount', '--device'):
            self.assertNotIn(forbidden, source)
        for action in re.findall(r'uses: ([^\s#]+)', source):
            self.assertRegex(action, r'@[a-f0-9]{40}$')
        self.assertIn("python -m unittest discover -s tests -p 'test_ci_public_route.py'", source)

    def test_caller_pins_the_own_repository_leaf_without_secret_or_ref_inputs(self):
        source = (ROOT / '.github/workflows/manual-build.yml').read_text().split('  sign-and-publish-held:')[0]
        self.assertRegex(source, r'uses: Oxiom-Systems/watch-my-ai-doing/\.github/workflows/ci-build-recipe\.yml@[a-f0-9]{40}')
        for forbidden in ('secrets:', 'with:', ': write', 'pull_request_target', 'repository_dispatch'):
            self.assertNotIn(forbidden, source)

    def test_unsigned_build_and_separate_held_authority(self):
        source = self.source()
        for command in [':phone:assembleRelease', ':wear:assembleRelease', ':phone:bundleRelease', ':wear:bundleRelease', '--max-workers=2', '-Xmx1536m', '-XX:MaxMetaspaceSize=512m', '-Dorg.gradle.parallel=false', 'artifact-digest', 'github.run_attempt', 'sha256sum', 'if-no-files-found: error']:
            self.assertIn(command, source)
        self.assertNotIn('environment:', source)
        self.assertNotIn('WMAID_KEYSTORE', source)
        self.assertNotIn('connectedAndroidTest', source)
        caller = (ROOT / '.github/workflows/manual-build.yml').read_text()
        self.assertIn('ci-android-signing-held', caller)
        self.assertIn('timeout-minutes: 5', caller)
        self.assertIn('publish_release', caller)
        self.assertIn('gh release create', caller)
        self.assertIn('environment: production', caller)
        self.assertNotIn('runs-on: ubuntu-latest', caller)

    def test_tool_versions_and_wrapper_integrity(self):
        manifest = json.loads((ROOT / '.github/ci/android-toolchain.json').read_text())
        self.assertEqual(manifest['android_cli']['version'], '19.0')
        self.assertEqual(manifest['sdk_packages'], ['platform-tools', 'platforms;android-36', 'build-tools;36.0.0'])
        source = self.source()
        self.assertIn('java-version: \"17\"', source)
        self.assertIn('distributionSha256Sum=2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb', (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text())


if __name__ == '__main__':
    unittest.main()
