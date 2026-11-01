"""Restore product runtime configuration in a clean CI checkout, without logging secrets."""
import base64
import json
import os
from pathlib import Path
from urllib.parse import urlsplit

from prepare_signing import property_value


def main():
    names = ['CM_BUILD_DIR', 'GOOGLE_SERVICES_JSON_BASE64', 'APPSFLYER_DEV_KEY', 'BINOM_ANALYTICS_URL']
    missing = [name for name in names if not os.environ.get(name)]
    if missing:
        raise SystemExit('Missing runtime variables: ' + ', '.join(missing))
    root = Path(os.environ['CM_BUILD_DIR']) / 'android'
    firebase = root / 'app/google-services.json'
    properties = root / 'local.properties'
    if firebase.exists():
        raise SystemExit('google-services.json already exists; refusing to overwrite it')
    if not properties.is_file():
        raise SystemExit('Run configure_sdk.py before prepare_runtime.py')
    original = properties.read_text(encoding='ascii')
    if any(line.strip() and not line.startswith('sdk.dir=') for line in original.splitlines()):
        raise SystemExit('local.properties contains existing configuration; refusing to overwrite it')
    try:
        raw = base64.b64decode(''.join(os.environ['GOOGLE_SERVICES_JSON_BASE64'].split()), validate=True)
        config = json.loads(raw)
        packages = {c['client_info']['android_client_info']['package_name'] for c in config['client']}
        if 'com.cruaoctkred.app' not in packages:
            raise ValueError()
        if not config.get('project_info'):
            raise ValueError()
    except (ValueError, KeyError, TypeError):
        raise SystemExit('Invalid Firebase configuration or Android package mismatch') from None
    url = os.environ['BINOM_ANALYTICS_URL']
    parsed = urlsplit(url)
    if parsed.scheme != 'https' or not parsed.hostname or parsed.username or parsed.password:
        raise SystemExit('BINOM_ANALYTICS_URL must be a valid HTTPS tracker URL')
    if not os.environ['APPSFLYER_DEV_KEY'].isalnum():
        raise SystemExit('APPSFLYER_DEV_KEY must contain only letters and digits')
    values = {key: os.environ[key] for key in ['APPSFLYER_DEV_KEY', 'BINOM_ANALYTICS_URL']}
    values['DESIGN_VERSION'] = os.environ.get('DESIGN_VERSION', 'blue-ua-3')
    # Gradle interpolates these fields as Java strings; forbid quote/control injection.
    if any(any(ch in value for ch in ['"', '\\', '\n', '\r', '\x00']) for value in values.values()):
        raise SystemExit('Runtime values contain unsupported quote, backslash or control characters')
    with firebase.open('xb') as stream:
        firebase.chmod(0o600)
        stream.write(raw)
    properties.chmod(0o600)
    with properties.open('a', encoding='ascii') as stream:
        stream.write(''.join(key + '=' + property_value(value) + '\n' for key, value in values.items()))
    print('Firebase package and analytics configuration restored; secret values are not logged.')


if __name__ == '__main__':
    main()
