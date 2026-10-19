"""Provision verified Android tools inside one disposable job's temporary disk."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import urllib.request
import zipfile


def download(url, target, expected_sha256, max_bytes=None):
    if not url.startswith('https://'):
        raise ValueError('Tool archives require HTTPS')
    checksum = hashlib.sha256()
    size = 0
    try:
        with urllib.request.urlopen(url, timeout=60) as response, target.open('wb') as output:
            while chunk := response.read(1024 * 1024):
                size += len(chunk)
                if max_bytes is not None and size > max_bytes:
                    raise ValueError('Tool archive exceeds its pinned size')
                checksum.update(chunk)
                output.write(chunk)
        if checksum.hexdigest() != expected_sha256:
            raise ValueError('Tool archive checksum mismatch')
    except Exception:
        target.unlink(missing_ok=True)
        raise


def extract_cli(archive, sdk_root, version):
    stage = sdk_root / '.cli-extract'
    stage.mkdir(parents=True)
    with zipfile.ZipFile(archive) as contents:
        for member in contents.infolist():
            if not (stage / member.filename).resolve().is_relative_to(stage.resolve()):
                raise ValueError('Android archive escapes its job cache')
        contents.extractall(stage)
    destination = sdk_root / 'cmdline-tools' / version
    destination.parent.mkdir(parents=True)
    shutil.move(str(stage / 'cmdline-tools'), destination)
    stage.rmdir()
    for command in (destination / 'bin').iterdir():
        if command.is_file():
            command.chmod(0o755)
    return destination / 'bin/sdkmanager'


def disk_report(directory, stage):
    usage = shutil.disk_usage(directory)
    print(json.dumps({'stage': stage, 'disk_total_bytes': usage.total,
                      'disk_used_bytes': usage.used, 'disk_free_bytes': usage.free}), flush=True)


def sdk_command(manager, sdk_root, arguments, log, answers=None):
    with log.open('a') as output:
        result = subprocess.run(
            [str(manager), '--sdk_root=' + str(sdk_root), *arguments],
            input=answers, text=True, stdout=output, stderr=subprocess.STDOUT,
            timeout=900,
        )
    if result.returncode:
        print(log.read_text()[-12000:], flush=True)
        raise RuntimeError('Android SDK command failed: ' + str(result.returncode))


def main():
    cache = Path(os.environ['RUNNER_TEMP']) / 'wmaid-toolchain'
    cache.mkdir()
    manifest = json.loads(Path(__file__).with_name('android-toolchain.json').read_text())
    disk_report(cache, 'before-toolchain')
    archive = cache / 'android-cli.zip'
    spec = manifest['android_cli']
    download(spec['url'], archive, spec['sha256'], spec['compressed_bytes'])
    sdk_root = cache / 'android-sdk'
    manager = extract_cli(archive, sdk_root, spec['version'])
    archive.unlink()
    log = cache / 'sdkmanager.log'
    sdk_command(manager, sdk_root, ['--licenses'], log, answers='y\n' * 100)
    sdk_command(manager, sdk_root, ['--install', *manifest['sdk_packages']], log)
    disk_report(cache, 'after-android-sdk')
    with Path(os.environ['GITHUB_ENV']).open('a') as output:
        output.write('ANDROID_HOME=' + str(sdk_root) + '\n')
        output.write('ANDROID_SDK_ROOT=' + str(sdk_root) + '\n')
    with Path(os.environ['GITHUB_PATH']).open('a') as output:
        for directory in [manager.parent, sdk_root / 'platform-tools']:
            output.write(str(directory) + '\n')


if __name__ == '__main__':
    main()
