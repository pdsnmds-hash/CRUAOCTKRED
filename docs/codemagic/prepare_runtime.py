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
    # Copy/paste from the UI often adds a final newline. Trim edges only;
    # never remove characters from the middle of a URL or change its meaning.
    values = {key: os.environ[key].strip() for key in ['APPSFLYER_DEV_KEY', 'BINOM_ANALYTICS_URL']}
    values['DESIGN_VERSION'] = os.environ.get('DESIGN_VERSION', 'blue-ua-3').strip()
    for name, value in values.items():
        if not value:
            raise SystemExit(f'{name} is empty after trimming whitespace')
        problems = []
        if '"' in value:
            problems.append('double quote')
        if "\\" in value:
            problems.append('backslash')
        if any(ord(char) < 32 or ord(char) == 127 for char in value):
            problems.append('embedded line break or control character')
        if problems:
            raise SystemExit(f'Invalid {name}: contains ' + ', '.join(problems)
                             + '. Paste the raw value on one line, without surrounding quotes or shell escaping. Value is not logged.')
    url = values['BINOM_ANALYTICS_URL']
    if any(char.isspace() for char in url):
        raise SystemExit('Invalid BINOM_ANALYTICS_URL: contains embedded whitespace. Paste the complete URL on one line.')
    try:
        parsed = urlsplit(url)
        valid_url = parsed.scheme == 'https' and bool(parsed.hostname) and not parsed.username and not parsed.password
    except ValueError:
        valid_url = False
    if not valid_url:
        raise SystemExit('BINOM_ANALYTICS_URL must be a valid HTTPS tracker URL')
    if not values['APPSFLYER_DEV_KEY'].isascii() or not values['APPSFLYER_DEV_KEY'].isalnum():
        raise SystemExit('APPSFLYER_DEV_KEY must contain only ASCII letters and digits')
    with firebase.open('xb') as stream:
        firebase.chmod(0o600)
        stream.write(raw)
    properties.chmod(0o600)
    with properties.open('a', encoding='ascii') as stream:
        stream.write(''.join(key + '=' + property_value(value) + '\n' for key, value in values.items()))
    print('Firebase package and analytics configuration restored; secret values are not logged.')


if __name__ == '__main__':
    main()
