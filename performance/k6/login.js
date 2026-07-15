import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const thresholdsConfig = JSON.parse(open('../thresholds.json'));

export const options = {
  vus: 50,
  duration: '5s',
  thresholds: thresholdsConfig.thresholds,
};

export function setup() {
  const headers = { 'Content-Type': 'application/json' };
  const runId = Date.now();
  for (let i = 1; i <= options.vus; i++) {
    const registerPayload = JSON.stringify({
      userName: `k6_user_${runId}_${i}`,
      email: `k6_user_${runId}_${i}@example.com`,
      password: 'password'
    });
    http.post(`${BASE_URL}/api/auth/register`, registerPayload, { headers });
  }
  return { runId };
}

export default function (data) {
  const vuId = __VU;
  const loginPayload = JSON.stringify({
    userName: `k6_user_${data.runId}_${vuId}`,
    password: 'password'
  });
  const headers = { 'Content-Type': 'application/json' };
  const res = http.post(`${BASE_URL}/api/auth/login`, loginPayload, { headers });

  check(res, {
    'status is 200': (r) => r.status === 200,
    'body says Login Successful': (r) => r.body.includes('Login Successful'),
  });

  sleep(1);
}

export function handleSummary(data) {
  return {
    'stdout': `k6 login test run completed: p95 duration = ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms, fail rate = ${(data.metrics.http_req_failed.values.rate * 100).toFixed(2)}%`,
    '/app/performance/reports/k6/login_summary.json': JSON.stringify(data, null, 2),
  };
}
