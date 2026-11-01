"""Fixtures only: no network calls, Gradle execution, or real waiting."""
import unittest
from unittest.mock import Mock
from build_android import GRADLE_ARGS, build_with_retry, retryable

RATE_LIMIT = "Could not GET 'https://repo.maven.apache.org/maven2/org/robolectric/nativeruntime-dist-compat/1.0.19/nativeruntime-dist-compat-1.0.19.jar'. Received status code 429 from server: Too Many Requests"


class BuildRetryTest(unittest.TestCase):
    def test_success_needs_no_retry(self):
        run = Mock(return_value=(0, 'BUILD SUCCESSFUL')); wait = Mock()
        self.assertEqual(build_with_retry(run, wait), 0)
        self.assertEqual(run.call_count, 1); wait.assert_not_called()

    def test_429_then_success_keeps_all_gates(self):
        run = Mock(side_effect=[(1, RATE_LIMIT), (0, 'BUILD SUCCESSFUL')]); wait = Mock()
        self.assertEqual(build_with_retry(run, wait), 0)
        self.assertEqual(run.call_count, 2); wait.assert_called_once_with(60)
        self.assertTrue({':app:testDebugUnitTest', ':app:lintDebug', ':app:bundleRelease', '-PbuildBundle=true'}.issubset(GRADLE_ARGS))

    def test_exhaustion_still_fails(self):
        run = Mock(return_value=(1, RATE_LIMIT)); wait = Mock()
        self.assertEqual(build_with_retry(run, wait), 1)
        self.assertEqual(run.call_count, 3)
        self.assertEqual([call.args[0] for call in wait.call_args_list], [60, 120])

    def test_real_test_compile_lint_and_other_http_errors_are_not_retried(self):
        for message in ['There were failing tests.', 'e: Compilation failed', 'Lint found 1 errors', 'OutOfMemoryError']:
            run = Mock(return_value=(1, message + '\n' + RATE_LIMIT)); wait = Mock()
            self.assertEqual(build_with_retry(run, wait), 1)
            self.assertEqual(run.call_count, 1); wait.assert_not_called()
        self.assertFalse(retryable(RATE_LIMIT.replace('429', '404')))
        self.assertFalse(retryable(RATE_LIMIT.replace('repo.maven.apache.org', 'other.example')))


if __name__ == '__main__':
    unittest.main()
