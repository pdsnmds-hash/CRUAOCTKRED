"""Run the unchanged AAB gates; retry only Maven Central HTTP 429 downloads."""
from collections import deque
import os
from pathlib import Path
import re
import subprocess
import sys
import time

GRADLE_ARGS = (
    '--no-daemon', '--max-workers=2', '-PbuildBundle=true',
    ':app:testDebugUnitTest', ':app:lintDebug', ':app:bundleRelease',
)
RETRY_DELAYS = (60, 120)
CENTRAL_429 = re.compile(
    r"Could not (?:GET|HEAD) ['\"]https://(?:repo\.maven\.apache\.org|repo1\.maven\.org)/maven2/[^'\"\r\n]+['\"]"
    r"[^\r\n]*Received status code 429 from server", re.I,
)
NON_NETWORK_FAILURE = re.compile(
    r'There were failing tests|[1-9]\d* tests? (?:completed,.* )?failed|'
    r'^e: |Compilation error|Lint found [1-9]\d* errors|OutOfMemoryError', re.M,
)


def retryable(output):
    return bool(CENTRAL_429.search(output)) and not NON_NETWORK_FAILURE.search(output)


def build_with_retry(run_once, sleep=time.sleep):
    for attempt in range(len(RETRY_DELAYS) + 1):
        status, output = run_once()
        if status == 0:
            return 0
        if not retryable(output) or attempt == len(RETRY_DELAYS):
            return status
        delay = RETRY_DELAYS[attempt]
        print(f'Maven Central rate-limited dependency download (HTTP 429). '
              f'Retrying the same build in {delay}s; attempt {attempt + 2}/3. '
              'Tests and lint remain enabled; dependency cache is preserved.', flush=True)
        sleep(delay)
    raise AssertionError('unreachable')


def main():
    root = Path(os.environ['CM_BUILD_DIR']) / 'android'
    wrapper = root / 'gradlew'
    if not wrapper.is_file():
        raise SystemExit('Android Gradle wrapper not found in checkout')
    wrapper.chmod(wrapper.stat().st_mode | 0o100)

    def run_once():
        tail = deque(maxlen=1500)
        process = subprocess.Popen([str(wrapper), *GRADLE_ARGS], cwd=root,
                                   stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                   text=True, errors='replace')
        for line in process.stdout:
            print(line, end='', flush=True)
            tail.append(line)
        return process.wait(), ''.join(tail)

    raise SystemExit(build_with_retry(run_once))


if __name__ == '__main__':
    main()
