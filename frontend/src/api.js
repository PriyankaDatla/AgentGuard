const BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8081/api'

async function request(method, path, body) {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  const data = text ? JSON.parse(text) : {}
  if (!res.ok) throw Object.assign(new Error(data.message || 'Request failed'), { status: res.status, data })
  return data
}

// Create user
export const createUser = (body) => request('POST', '/users', body)
// Get user
export const getUser = (id) => request('GET', `/users/${id}`)
// Create policy
export const createPolicy = (body) => request('POST', '/policies', body)
// Get policies for user
export const getPolicies = (userId) => request('GET', `/policies/user/${userId}`)
// Evaluate via agent (LLM + policy)
export const agentEvaluate = (body) => request('POST', '/agent/evaluate', body)
// Evaluate directly (no LLM)
export const evaluateTransaction = (body) => request('POST', '/transactions/evaluate', body)
// Get transactions for user
export const getTransactions = (userId) => request('GET', `/transactions/user/${userId}`)
// Get audit for transaction
export const getAudit = (txId) => request('GET', `/audit/transaction/${txId}`)
// Create Razorpay payment order
export const createPaymentOrder = (transactionId) =>
  request('POST', '/payments/create', { transactionId })
