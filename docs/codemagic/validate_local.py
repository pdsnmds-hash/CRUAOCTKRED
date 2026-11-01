"""Local fixture checks only; never generate a production upload key or run Codemagic."""
import base64
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import zipfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
JAVA = Path(os.environ.get('JAVA_HOME') or subprocess.check_output(['/usr/libexec/java_home', '-v', '17'], text=True).strip())
ENV = dict(os.environ, JAVA_HOME=str(JAVA), PYTHONDONTWRITEBYTECODE='1')
for key in list(ENV):
    if key.startswith(('CM_', 'NEW_KEY', 'GOOGLE_SERVICES_JSON', 'APPSFLYER_DEV_KEY', 'BINOM_ANALYTICS_URL')):
        ENV.pop(key)
report = {}
private = [ROOT / p for p in ['android/signing.properties', 'android/signing/upload-keystore.jks', 'android/signing/upload-certificate.pem', 'android/local.properties', 'android/app/google-services.json']]
before = {p: hashlib.sha256(p.read_bytes()).digest() for p in private if p.exists()}


def call(args, env=None, ok=True, **kwargs):
    result = subprocess.run([str(x) for x in args], env=env or ENV, capture_output=True, text=True, **kwargs)
    if (result.returncode == 0) != ok:
        raise AssertionError('Unexpected command result: ' + str(args[:2]) + '\n' + result.stdout + result.stderr)
    return result


def helper(name, env, ok=True):
    return call([sys.executable, HERE / name], env=env, ok=ok)


with tempfile.TemporaryDirectory(prefix='crua-ci-validation-') as directory:
    temp = Path(directory)
    configurations = {}
    for p in [ROOT / 'codemagic.yaml', HERE / 'generate-upload-key.yaml', HERE / 'codemagic.yaml']:
        result = call(['ruby', '-ryaml', '-rjson', '-e', 'puts JSON.generate(YAML.safe_load(File.read(ARGV[0])))', p])
        config = json.loads(result.stdout)
        configurations[str(p)] = config
        for workflow in config['workflows'].values():
            for script in workflow['scripts']:
                call(['bash', '-n'], input=script['script'])
    assert (ROOT / 'codemagic.yaml').read_bytes() in [(HERE / 'generate-upload-key.yaml').read_bytes(), (HERE / 'codemagic.yaml').read_bytes()]
    generate = configurations[str(HERE / 'generate-upload-key.yaml')]['workflows']['my_crua-create-upload-key-once']
    ordinary = configurations[str(HERE / 'codemagic.yaml')]['workflows']['my-crua-aab']
    assert ordinary['environment']['android_signing'] == ['my_crua_upload']
    assert not any('.jks' in pattern for pattern in ordinary['artifacts'])
    assert '-PbuildBundle=true' in str(ordinary['scripts'])
    assert generate['environment']['groups'] == ['my_crua_key_generation']
    script = generate['scripts'][0]['script']
    missing_generator = call(['bash', '-c', script], env=dict(ENV, CM_BUILD_DIR=str(temp)), ok=False)
    assert not (temp / 'generated-signing').exists()
    report['yaml_and_shell_syntax'] = 'PASS'
    report['generator_missing_secrets_rejected_without_generating_key'] = 'PASS'

    checkout = temp / 'checkout'
    (checkout / 'android/app').mkdir(parents=True)
    source = temp / 'identity.jks'
    source.write_bytes(b'fixture key bytes, not a real production key')
    signing_env = dict(ENV, CM_BUILD_DIR=str(checkout), CM_KEYSTORE_PATH=str(source),
                       CM_KEYSTORE_PASSWORD=' leading:=#!\\ \nпароль😀', CM_KEY_ALIAS='upload:ключ',
                       CM_KEY_PASSWORD='key= pass\t!😀')
    helper('prepare_signing.py', signing_env)
    key = checkout / 'android/signing/upload-keystore.jks'
    properties = checkout / 'android/signing.properties'
    assert key.read_bytes() == source.read_bytes()
    assert key.stat().st_mode & 0o777 == 0o600
    assert properties.stat().st_mode & 0o777 == 0o600
    assert key.parent.stat().st_mode & 0o777 == 0o700
    original = properties.read_bytes()
    helper('prepare_signing.py', signing_env, ok=False)
    assert original == properties.read_bytes()
    for name in ['CM_BUILD_DIR', 'CM_KEYSTORE_PATH', 'CM_KEYSTORE_PASSWORD', 'CM_KEY_ALIAS', 'CM_KEY_PASSWORD']:
        invalid = signing_env.copy(); invalid.pop(name)
        helper('prepare_signing.py', invalid, ok=False)
    report['signing_bridge_permissions_paths_no_overwrite_missing_env'] = 'PASS'

    java = temp / 'ReadProperties.java'
    java.write_text('''import java.util.*; import java.nio.file.*; import java.nio.charset.*;
class ReadProperties { public static void main(String[] args) throws Exception {
Properties p = new Properties(); try(var in=Files.newInputStream(Path.of(args[0]))) {p.load(in);}
for(String k: new TreeSet<>(p.stringPropertyNames())) System.out.println(k+"\\t"+Base64.getEncoder().encodeToString(p.getProperty(k).getBytes(StandardCharsets.UTF_8)));
}}''')
    loaded = call([JAVA / 'bin/java', java, properties]).stdout
    values = {k: base64.b64decode(v).decode() for k, v in (line.split('\t') for line in loaded.splitlines())}
    assert values == {'storeFile': 'signing/upload-keystore.jks', 'storePassword': signing_env['CM_KEYSTORE_PASSWORD'],
                      'keyAlias': signing_env['CM_KEY_ALIAS'], 'keyPassword': signing_env['CM_KEY_PASSWORD']}
    report['java_properties_unicode_special_character_roundtrip'] = 'PASS'

    sdk = temp / 'sdk'
    calls = temp / 'sdk-calls.json'
    for name, revision in [('latest', '9.0'), ('latest-2', '21.0')]:
        folder = sdk / 'cmdline-tools' / name
        (folder / 'bin').mkdir(parents=True)
        (folder / 'source.properties').write_text('Pkg.Revision=' + revision + '\n')
        tool = folder / 'bin/sdkmanager'
        tool.write_text('#!' + sys.executable + '\nimport json,os,sys\nfrom pathlib import Path\nPath(os.environ["SDK_TEST_CALLS"]).write_text(json.dumps(sys.argv))\nsys.exit(int(os.environ.get("SDK_TEST_EXIT", "0")))\n')
        tool.chmod(0o755)
    sdk_env = dict(ENV, CM_BUILD_DIR=str(checkout), ANDROID_SDK_ROOT=str(sdk), SDK_TEST_CALLS=str(calls))
    helper('configure_sdk.py', sdk_env)
    args = json.loads(calls.read_text())
    assert 'latest-2/bin/sdkmanager' in args[0]
    assert args[1:] == ['--sdk_root=' + str(sdk), 'platforms;android-37.0', 'build-tools;36.0.0']
    local = checkout / 'android/local.properties'
    assert local.stat().st_mode & 0o777 == 0o600
    helper('configure_sdk.py', sdk_env, ok=False)
    local.unlink()
    helper('configure_sdk.py', dict(sdk_env, SDK_TEST_EXIT='7'), ok=False)
    assert not local.exists()
    helper('configure_sdk.py', dict(sdk_env, ANDROID_SDK_ROOT=str(temp / 'missing-sdk')), ok=False)
    helper('configure_sdk.py', sdk_env)
    report['sdk_newest_revision_exact_packages_failure_and_missing_tool'] = 'PASS'

    firebase = {'project_info': {'project_id': 'fixture'}, 'client': [{'client_info': {'android_client_info': {'package_name': 'com.cruaoctkred.app'}}}]}
    runtime_env = dict(ENV, CM_BUILD_DIR=str(checkout), GOOGLE_SERVICES_JSON_BASE64=base64.b64encode(json.dumps(firebase).encode()).decode(),
                       APPSFLYER_DEV_KEY='Fixture123', BINOM_ANALYTICS_URL='https://tracker.example/click?targetid=fixture&t1={t1}', DESIGN_VERSION='blue-ua-3')
    for name in ['GOOGLE_SERVICES_JSON_BASE64', 'APPSFLYER_DEV_KEY', 'BINOM_ANALYTICS_URL']:
        invalid = runtime_env.copy(); invalid.pop(name); helper('prepare_runtime.py', invalid, ok=False)
    helper('prepare_runtime.py', dict(runtime_env, GOOGLE_SERVICES_JSON_BASE64='bad-base64'), ok=False)
    wrong = json.loads(json.dumps(firebase)); wrong['client'][0]['client_info']['android_client_info']['package_name'] = 'other.package'
    helper('prepare_runtime.py', dict(runtime_env, GOOGLE_SERVICES_JSON_BASE64=base64.b64encode(json.dumps(wrong).encode()).decode()), ok=False)
    helper('prepare_runtime.py', dict(runtime_env, BINOM_ANALYTICS_URL='http://tracker.example'), ok=False)
    for name in ['APPSFLYER_DEV_KEY', 'BINOM_ANALYTICS_URL', 'DESIGN_VERSION']:
        for suffix in ['"', '\\', '\ninside', '\tinside']:
            result = helper('prepare_runtime.py', dict(runtime_env, **{name: runtime_env[name] + suffix}), ok=False)
            assert name in result.stderr
            assert runtime_env[name] not in result.stderr
    helper('prepare_runtime.py', {key: (' \r\n' + value + '\r\n ') if key in ['APPSFLYER_DEV_KEY', 'BINOM_ANALYTICS_URL', 'DESIGN_VERSION'] else value for key, value in runtime_env.items()})
    firebase_file = checkout / 'android/app/google-services.json'
    assert firebase_file.stat().st_mode & 0o777 == 0o600
    assert json.loads(firebase_file.read_bytes()) == firebase
    helper('prepare_runtime.py', runtime_env, ok=False)
    roundtrip = call([JAVA / 'bin/java', java, local]).stdout
    fields = {k: base64.b64decode(v).decode() for k, v in (line.split('\t') for line in roundtrip.splitlines())}
    assert fields['sdk.dir'] == str(sdk)
    for name in ['APPSFLYER_DEV_KEY', 'BINOM_ANALYTICS_URL', 'DESIGN_VERSION']: assert fields[name] == runtime_env[name]
    report['runtime_configuration_missing_invalid_package_no_overwrite_and_roundtrip'] = 'PASS'
    report['runtime_trim_outer_whitespace_and_redacted_named_errors'] = 'PASS'

    fixture_env = dict(ENV, CM_KEYSTORE_PATH=str(temp / 'fixture.jks'), CM_KEYSTORE_PASSWORD='TemporaryStore123!', CM_KEY_ALIAS='fixture', CM_KEY_PASSWORD='TemporaryKey123!')
    call([JAVA / 'bin/keytool', '-genkeypair', '-noprompt', '-storetype', 'JKS', '-keystore', fixture_env['CM_KEYSTORE_PATH'],
          '-alias', 'fixture', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '1', '-dname', 'CN=Temporary fixture',
          '-storepass:env', 'CM_KEYSTORE_PASSWORD', '-keypass:env', 'CM_KEY_PASSWORD'], env=fixture_env)

    def fixture(name, signed=True, abis=None, private=False):
        p = temp / name
        with zipfile.ZipFile(p, 'w') as z:
            for abi in abis or ['armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64']: z.writestr('base/lib/' + abi + '/libfixture.so', b'ABI presence fixture')
            if private: z.writestr('base/assets/signing.properties', 'fixture=true')
        if signed:
            call([JAVA / 'bin/jarsigner', '-keystore', fixture_env['CM_KEYSTORE_PATH'], '-storepass:env', 'CM_KEYSTORE_PASSWORD',
                  '-keypass:env', 'CM_KEY_PASSWORD', p, 'fixture'], env=fixture_env)
        return p

    call([sys.executable, HERE / 'verify_aab.py', fixture('signed.aab')], env=fixture_env)
    call([sys.executable, HERE / 'verify_aab.py', fixture('unsigned.aab', signed=False)], env=fixture_env, ok=False)
    call([sys.executable, HERE / 'verify_aab.py', fixture('missing-abi.aab', abis=['arm64-v8a'])], env=fixture_env, ok=False)
    call([sys.executable, HERE / 'verify_aab.py', fixture('private.aab', private=True)], env=fixture_env, ok=False)
    partial = temp / 'partial.aab'
    partial.write_bytes((temp / 'signed.aab').read_bytes())
    with zipfile.ZipFile(partial, 'a') as z: z.writestr('base/assets/unsigned.txt', 'fixture')
    call([sys.executable, HERE / 'verify_aab.py', partial], env=fixture_env, ok=False)
    other_env = dict(fixture_env, CM_KEYSTORE_PATH=str(temp / 'other-key.jks'))
    call([JAVA / 'bin/keytool', '-genkeypair', '-noprompt', '-storetype', 'JKS', '-keystore', other_env['CM_KEYSTORE_PATH'],
          '-alias', 'fixture', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '1', '-dname', 'CN=Other temporary fixture',
          '-storepass:env', 'CM_KEYSTORE_PASSWORD', '-keypass:env', 'CM_KEY_PASSWORD'], env=other_env)
    call([sys.executable, HERE / 'verify_aab.py', temp / 'signed.aab'], env=other_env, ok=False)
    report['verifier_signed_four_abi_fixture_unsigned_missing_abi_private_file_rejection'] = 'PASS'
    report['verifier_wrong_identity_and_unsigned_entries_rejection'] = 'PASS'

    gitdir = temp / 'git-index'
    call(['git', 'init', '--bare', '-q', gitdir])
    git = ['git', '--git-dir=' + str(gitdir), '--work-tree=' + str(ROOT)]
    excluded = ['android/signing/upload-keystore.jks', 'android/signing.properties', 'android/local.properties',
                'android/app/google-services.json', 'serviceAccountKey.json', 'generated-signing/my-crua-upload.jks',
                'android/app/build/outputs/bundle/release/app-release.aab', 'private.zip', 'artifacts/log.txt']
    for name in excluded: call(git + ['check-ignore', '--no-index', name])
    call(git + ['check-ignore', '--no-index', 'android/gradle/wrapper/gradle-wrapper.jar'], ok=False)
    candidates = call(git + ['ls-files', '--others', '--exclude-standard']).stdout.splitlines()
    assert not any(p.endswith(('.jks', '.keystore', '.apk', '.aab', '.zip')) or p.endswith(('google-services.json', 'local.properties', 'signing.properties')) for p in candidates)
    report['temporary_git_index_ignore_rules_and_wrapper_inclusion'] = 'PASS'

assert all(p.exists() and hashlib.sha256(p.read_bytes()).digest() == value for p, value in before.items())
report['existing_private_keys_and_runtime_configuration'] = 'UNCHANGED'
report['temporary_fixture_keys'] = 'DELETED'
report['production_key_generation_on_mac'] = 'NOT RUN'
report['cloud_build'] = 'See cloud-status.json; local validation does not execute Codemagic'
git_state = subprocess.run(['git', '-C', str(ROOT), 'rev-parse', '--is-inside-work-tree'], capture_output=True, text=True)
if git_state.returncode == 0:
    staged = subprocess.check_output(['git', '-C', str(ROOT), 'diff', '--cached', '--name-only'], text=True).splitlines()
    forbidden = ['.jks', '.keystore', 'google-services.json', 'local.properties', 'signing.properties', 'serviceAccountKey.json']
    assert not any(path.endswith(tuple(forbidden)) for path in staged), 'Private configuration is staged; remove it before committing'
    report['real_git_staged_files'] = f'CHECKED: {len(staged)} paths; no signing/runtime secrets'
else:
    report['real_git_staged_files'] = 'NOT AVAILABLE: project is not a Git repository' 
report['application_build_in_clean_checkout'] = 'NOT RUN; local checks validate CI helpers, not the cloud application build'
(HERE / 'local-validation.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
