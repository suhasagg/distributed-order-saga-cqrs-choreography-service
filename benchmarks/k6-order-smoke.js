import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = { vus: 5, duration: '20s' };

export default function () {
  const payload = JSON.stringify({
    userId: `user-${__VU}`,
    sku: 'SKU-LOAD',
    quantity: 1,
    amount: 42.0,
    paymentMethod: 'CARD',
    shippingAddress: 'Delhi NCR'
  });
  const res = http.post('http://localhost:8080/orders', payload, {
    headers: { 'Content-Type': 'application/json', 'X-Tenant-ID': 'tenant-a' }
  });
  check(res, { 'created': r => r.status === 200 });
  sleep(1);
}
