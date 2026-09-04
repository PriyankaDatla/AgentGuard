import React, { useState, useEffect, useCallback } from 'react'
import {
  agentEvaluate,
  evaluateTransaction,
  getTransactions,
  createPaymentOrder,
  createUser,
  createPolicy,
  getPolicies,
  getUser,
} from './api.js'
import './App.css'

// ── Helpers ───────────────────────────────────────────────────────────────────

const fmt = (n) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(n)

const statusColor = (s) => {
  if (!s) return ''
  if (s === 'APPROVED' || s === 'COMPLETED' || s === 'PAYMENT_CREATED') return 'green'
  if (s === 'BLOCKED' || s === 'PAYMENT_FAILED') return 'red'
  return 'yellow'
}

const statusLabel = (s) => ({
  APPROVED: 'Approved', BLOCKED: 'Blocked', MANUAL_REVIEW: 'Manual Review',
  PAYMENT_CREATED: 'Payment Created', PAYMENT_FAILED: 'Payment Failed', COMPLETED: 'Completed',
}[s] || s)

// ── Sub-components ────────────────────────────────────────────────────────────

function Badge({ status }) {
  return <span className={`badge badge-${statusColor(status)}`}>{statusLabel(status)}</span>
}

function Stat({ label, value, color }) {
  return (
    <div className="stat-card">
      <div className={`stat-value ${color || ''}`}>{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  )
}

function PolicyCheckRow({ check }) {
  return (
    <div className={`policy-check ${check.passed ? 'passed' : 'failed'}`}>
      <span className="check-icon">{check.passed ? '✓' : '✗'}</span>
      <span className="check-rule">{check.rule}</span>
      <span className="check-explanation">{check.explanation}</span>
    </div>
  )
}

function DecisionPanel({ result, onPayment, paymentState }) {
  if (!result) return null
  const { decision } = result
  const status = decision?.decision
  const isApproved = status === 'APPROVED'
  const transactionId = decision?.transactionId

  return (
    <div className={`decision-panel decision-${statusColor(status)}`}>
      <div className="decision-header">
        <span className="decision-icon">
          {isApproved ? '✅' : status === 'BLOCKED' ? '🚫' : '⚠️'}
        </span>
        <div>
          <div className="decision-status">{statusLabel(status)}</div>
          <div className="decision-score">Risk score: {decision?.riskScore ?? '—'}</div>
        </div>
        {isApproved && transactionId && (
          <button
            className="btn-pay"
            onClick={() => onPayment(transactionId)}
            disabled={paymentState.loading || !!paymentState.orderId}
          >
            {paymentState.loading ? 'Creating order…' : paymentState.orderId ? '✓ Order Created' : 'Proceed to Payment'}
          </button>
        )}
      </div>

      {paymentState.orderId && (
        <div className="payment-info">
          Razorpay Order ID: <code>{paymentState.orderId}</code>
        </div>
      )}
      {paymentState.error && <div className="payment-error">{paymentState.error}</div>}

      {decision?.reasons?.length > 0 && (
        <div className="decision-reasons">
          {decision.reasons.map((r, i) => <p key={i}>{r}</p>)}
        </div>
      )}

      {result.intent && (
        <div className="intent-row">
          <span className="intent-label">Parsed intent:</span>
          <span>
            {result.intent.category}
            {result.intent.preferredBrand ? ` · ${result.intent.preferredBrand}` : ''}
            {' · max '}{fmt(result.intent.maxAmount)}
          </span>
        </div>
      )}

      {decision?.policyChecks?.length > 0 && (
        <div className="policy-checks">
          <div className="section-title">Policy Checks</div>
          {decision.policyChecks.map((c, i) => <PolicyCheckRow key={i} check={c} />)}
        </div>
      )}
    </div>
  )
}

function TransactionRow({ tx, onSelect, selected }) {
  return (
    <tr className={`tx-row ${selected ? 'tx-selected' : ''}`} onClick={() => onSelect(tx)}>
      <td>{tx.merchant}</td>
      <td>{tx.category}</td>
      <td className="tx-amount">{fmt(tx.requestedAmount)}</td>
      <td><Badge status={tx.status} /></td>
      <td className="tx-risk">{tx.riskScore}</td>
      <td className="tx-date">{new Date(tx.createdAt).toLocaleString('en-IN', { dateStyle: 'short', timeStyle: 'short' })}</td>
    </tr>
  )
}

// ── Policy Panel ──────────────────────────────────────────────────────────────

function PolicyPanel({ userId, policies, onRefresh }) {
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState({ category: '', maxAmount: '', requiresApproval: false })
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState(null)

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function handleAdd(e) {
    e.preventDefault()
    if (!form.category.trim() || !form.maxAmount) return
    setSaving(true)
    setError(null)
    try {
      await createPolicy({
        userId,
        category: form.category.trim().toLowerCase(),
        maxAmount: parseFloat(form.maxAmount),
        requiresApproval: form.requiresApproval,
        active: true,
      })
      setForm({ category: '', maxAmount: '', requiresApproval: false })
      setShowForm(false)
      onRefresh()
    } catch (err) {
      setError(err.data?.message || err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="card policy-card">
      <div className="card-header">
        <h2>Policies</h2>
        <button className="btn-ghost" onClick={() => { setShowForm(s => !s); setError(null) }}>
          {showForm ? '✕ Cancel' : '+ Add Policy'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleAdd} className="policy-form">
          <label>Category</label>
          <input value={form.category} onChange={set('category')} placeholder="e.g. electronics" required />
          <label>Max Amount (₹)</label>
          <input type="number" value={form.maxAmount} onChange={set('maxAmount')} placeholder="e.g. 25000" min="1" required />
          <div className="checkbox-row" style={{ marginTop: 4 }}>
            <label>
              <input
                type="checkbox"
                checked={form.requiresApproval}
                onChange={(e) => setForm((f) => ({ ...f, requiresApproval: e.target.checked }))}
              />
              &nbsp;Requires Approval
            </label>
          </div>
          {error && <div className="eval-error">{error}</div>}
          <button type="submit" className="btn-primary" disabled={saving} style={{ marginTop: 8 }}>
            {saving ? 'Saving…' : 'Save Policy'}
          </button>
        </form>
      )}

      {policies.length === 0 && !showForm ? (
        <div className="empty-state">No policies yet. Add one to allow purchases.</div>
      ) : (
        <div className="policy-list">
          {policies.map((p) => (
            <div key={p.id} className={`policy-item ${p.active ? '' : 'policy-inactive'}`}>
              <div className="policy-item-main">
                <span className="policy-category">{p.category}</span>
                <span className="policy-amount">{fmt(p.maxAmount)}</span>
              </div>
              <div className="policy-item-meta">
                {p.requiresApproval && <span className="policy-tag">Approval required</span>}
                {!p.active && <span className="policy-tag policy-tag-off">Inactive</span>}
                {p.active && !p.requiresApproval && <span className="policy-tag policy-tag-ok">Active</span>}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

// ── User Switcher ─────────────────────────────────────────────────────────────

function UserSwitcher({ currentUserId, onSwitch, onNew }) {
  const [open, setOpen] = useState(false)
  const [inputId, setInputId] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  async function handleLoad(e) {
    e.preventDefault()
    const id = parseInt(inputId, 10)
    if (!id || id < 1) { setError('Enter a valid user ID'); return }
    setLoading(true)
    setError(null)
    try {
      await getUser(id)
      localStorage.setItem('ag_userId', id)
      setOpen(false)
      setInputId('')
      onSwitch(id)
    } catch (err) {
      setError(err.status === 404 ? `User #${id} not found` : 'Could not load user')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ position: 'relative' }}>
      <span className="user-badge">User #{currentUserId}</span>
      <button className="btn-ghost" onClick={() => { setOpen(o => !o); setError(null); setInputId('') }}>
        Switch User
      </button>
      {open && (
        <div className="switcher-popover">
          <div className="switcher-title">Load existing user</div>
          <form onSubmit={handleLoad} className="switcher-form">
            <input
              type="number" min="1"
              value={inputId}
              onChange={e => setInputId(e.target.value)}
              placeholder="User ID (e.g. 1)"
              autoFocus
            />
            <button type="submit" className="btn-primary switcher-btn" disabled={loading}>
              {loading ? '…' : 'Load'}
            </button>
          </form>
          {error && <div className="switcher-error">{error}</div>}
          <div className="switcher-divider" />
          <button className="btn-ghost switcher-new" onClick={() => { setOpen(false); onNew() }}>
            + Create new user
          </button>
        </div>
      )}
    </div>
  )
}

// ── Setup Panel ───────────────────────────────────────────────────────────────

function SetupPanel({ onSetup }) {
  const [form, setForm] = useState({
    name: 'Demo Agent',
    email: `demo${Date.now()}@agentguard.dev`,
    spendingLimit: '50000',
    hourlySpendingLimit: '10000',
    category: 'electronics',
    policyMax: '20000',
    requiresApproval: false,
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function handleSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const user = await createUser({
        name: form.name,
        email: form.email,
        spendingLimit: parseFloat(form.spendingLimit),
        hourlySpendingLimit: parseFloat(form.hourlySpendingLimit),
      })
      await createPolicy({
        userId: user.id,
        category: form.category.trim().toLowerCase(),
        maxAmount: parseFloat(form.policyMax),
        requiresApproval: form.requiresApproval,
        active: true,
      })
      onSetup(user.id)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="setup-overlay">
      <div className="setup-modal">
        <div className="setup-logo">🛡️</div>
        <h2>AgentGuard Setup</h2>
        <p className="setup-sub">Create a user and initial spending policy to get started.</p>
        <form onSubmit={handleSubmit} className="setup-form">
          <label>Name</label>
          <input value={form.name} onChange={set('name')} required />
          <label>Email</label>
          <input value={form.email} onChange={set('email')} required />
          <div className="row2">
            <div>
              <label>Spending Limit (₹)</label>
              <input type="number" value={form.spendingLimit} onChange={set('spendingLimit')} min="1" required />
            </div>
            <div>
              <label>Hourly Limit (₹)</label>
              <input type="number" value={form.hourlySpendingLimit} onChange={set('hourlySpendingLimit')} min="1" required />
            </div>
          </div>
          <label>Initial Policy Category</label>
          <input value={form.category} onChange={set('category')} placeholder="e.g. electronics" required />
          <div className="row2">
            <div>
              <label>Policy Max Amount (₹)</label>
              <input type="number" value={form.policyMax} onChange={set('policyMax')} min="1" required />
            </div>
            <div className="checkbox-row">
              <label>
                <input
                  type="checkbox"
                  checked={form.requiresApproval}
                  onChange={(e) => setForm((f) => ({ ...f, requiresApproval: e.target.checked }))}
                />
                &nbsp;Requires Approval
              </label>
            </div>
          </div>
          {error && <div className="setup-error">{error}</div>}
          <button type="submit" className="btn-primary" disabled={loading}>
            {loading ? 'Creating…' : 'Create User & Start'}
          </button>
        </form>
      </div>
    </div>
  )
}

// ── Main App ──────────────────────────────────────────────────────────────────

export default function App() {
  const [userId, setUserId] = useState(() => {
    const v = localStorage.getItem('ag_userId')
    return v ? parseInt(v, 10) : null
  })
  const [showSetup, setShowSetup] = useState(false)
  const [transactions, setTransactions] = useState([])
  const [policies, setPolicies] = useState([])
  const [selectedTx, setSelectedTx] = useState(null)
  const [result, setResult] = useState(null)
  const [evaluating, setEvaluating] = useState(false)
  const [evalError, setEvalError] = useState(null)
  const [paymentState, setPaymentState] = useState({ loading: false, orderId: null, error: null })
  const [useAgent, setUseAgent] = useState(true)
  const [form, setForm] = useState({ merchant: '', request: '', amount: '', category: '' })

  const loadTransactions = useCallback(async (uid) => {
    try { setTransactions(await getTransactions(uid)) } catch (_) {}
  }, [])

  const loadPolicies = useCallback(async (uid) => {
    try { setPolicies(await getPolicies(uid)) } catch (_) {}
  }, [])

  useEffect(() => {
    if (userId) {
      loadTransactions(userId)
      loadPolicies(userId)
    }
  }, [userId, loadTransactions, loadPolicies])

  function handleSetup(uid) {
    localStorage.setItem('ag_userId', uid)
    setUserId(uid)
    setShowSetup(false)
  }

  function handleSwitchUser(uid) {
    setUserId(uid)
    setTransactions([])
    setPolicies([])
    setSelectedTx(null)
    setResult(null)
    setEvalError(null)
    setPaymentState({ loading: false, orderId: null, error: null })
    // useEffect picks up the new uid and loads data
  }

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  const stats = {
    approved: transactions.filter((t) => ['APPROVED', 'PAYMENT_CREATED', 'COMPLETED'].includes(t.status)).length,
    blocked: transactions.filter((t) => t.status === 'BLOCKED').length,
    manual: transactions.filter((t) => t.status === 'MANUAL_REVIEW').length,
    protected: transactions
      .filter((t) => ['APPROVED', 'PAYMENT_CREATED', 'COMPLETED'].includes(t.status))
      .reduce((s, t) => s + parseFloat(t.requestedAmount || 0), 0),
  }

  async function handleEvaluate(e) {
    e.preventDefault()
    if (!form.merchant.trim() || !form.amount) return
    setEvaluating(true)
    setEvalError(null)
    setResult(null)
    setPaymentState({ loading: false, orderId: null, error: null })
    try {
      if (useAgent) {
        if (!form.request.trim()) { setEvalError('Purchase request is required for AI evaluation.'); setEvaluating(false); return }
        const res = await agentEvaluate({ userId, merchant: form.merchant, request: form.request, amount: parseFloat(form.amount) })
        setResult({ intent: res.intent, decision: res.decision })
      } else {
        if (!form.category.trim()) { setEvalError('Category is required for direct evaluation.'); setEvaluating(false); return }
        const res = await evaluateTransaction({ userId, merchant: form.merchant, category: form.category, requestedAmount: parseFloat(form.amount) })
        setResult({ intent: null, decision: res })
      }
      loadTransactions(userId)
    } catch (err) {
      setEvalError(err.data?.message || err.message)
    } finally {
      setEvaluating(false)
    }
  }

  async function handlePayment(transactionId) {
    setPaymentState({ loading: true, orderId: null, error: null })
    try {
      const order = await createPaymentOrder(transactionId)
      setPaymentState({ loading: false, orderId: order.razorpayOrderId, error: null })
      loadTransactions(userId)
    } catch (err) {
      setPaymentState({ loading: false, orderId: null, error: err.data?.message || err.message })
    }
  }

  if (!userId || showSetup) return <SetupPanel onSetup={handleSetup} />

  return (
    <div className="layout">
      {/* Header */}
      <header className="header">
        <div className="header-left">
          <span className="logo">🛡️</span>
          <div>
            <div className="header-title">AgentGuard</div>
            <div className="header-sub">Financial Firewall for AI Agents</div>
          </div>
        </div>
        <div className="header-right">
          <UserSwitcher
            currentUserId={userId}
            onSwitch={handleSwitchUser}
            onNew={() => setShowSetup(true)}
          />
        </div>
      </header>

      {/* Stats */}
      <section className="stats-row">
        <Stat label="Approved" value={stats.approved} color="green" />
        <Stat label="Blocked" value={stats.blocked} color="red" />
        <Stat label="Manual Review" value={stats.manual} color="yellow" />
        <Stat label="Protected Amount" value={fmt(stats.protected)} />
      </section>

      <div className="main-grid">
        {/* Left: Evaluate form */}
        <section className="card eval-card">
          <div className="card-header">
            <h2>Evaluate Purchase</h2>
            <div className="toggle-row">
              <button className={`toggle-btn ${useAgent ? 'active' : ''}`} onClick={() => setUseAgent(true)} type="button">
                AI Agent
              </button>
              <button className={`toggle-btn ${!useAgent ? 'active' : ''}`} onClick={() => setUseAgent(false)} type="button">
                Direct
              </button>
            </div>
          </div>

          <form onSubmit={handleEvaluate} className="eval-form">
            <label>Merchant</label>
            <input value={form.merchant} onChange={set('merchant')} placeholder="e.g. Croma, Amazon" required />
            <label>Amount (₹)</label>
            <input type="number" value={form.amount} onChange={set('amount')} placeholder="e.g. 15000" min="0.01" step="0.01" required />
            {useAgent ? (
              <>
                <label>Purchase Request</label>
                <textarea value={form.request} onChange={set('request')} placeholder="e.g. Buy Sony WH-1000XM5 headphones under ₹25,000" />
              </>
            ) : (
              <>
                <label>Category</label>
                <input value={form.category} onChange={set('category')} placeholder="e.g. electronics" />
              </>
            )}
            {evalError && <div className="eval-error">{evalError}</div>}
            <button type="submit" className="btn-primary" disabled={evaluating}>
              {evaluating ? 'Evaluating…' : 'Evaluate Purchase'}
            </button>
          </form>

          <DecisionPanel result={result} onPayment={handlePayment} paymentState={paymentState} />
        </section>

        {/* Right column: transactions + policies */}
        <div className="right-col">
          {/* Transaction history */}
          <section className="card tx-card">
            <div className="card-header">
              <h2>Transaction History</h2>
              <button className="btn-ghost" onClick={() => loadTransactions(userId)}>↻ Refresh</button>
            </div>
            {transactions.length === 0 ? (
              <div className="empty-state">No transactions yet.</div>
            ) : (
              <div className="table-wrap">
                <table className="tx-table">
                  <thead>
                    <tr>
                      <th>Merchant</th>
                      <th>Category</th>
                      <th>Amount</th>
                      <th>Decision</th>
                      <th>Risk</th>
                      <th>Time</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map((tx) => (
                      <TransactionRow key={tx.id} tx={tx} selected={selectedTx?.id === tx.id} onSelect={setSelectedTx} />
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            {selectedTx && (
              <div className="tx-detail">
                <div className="tx-detail-header">
                  <strong>{selectedTx.merchant}</strong>
                  <Badge status={selectedTx.status} />
                </div>
                <p className="tx-reason">{selectedTx.decisionReason}</p>
                {selectedTx.razorpayOrderId && (
                  <p className="tx-order">Order: <code>{selectedTx.razorpayOrderId}</code></p>
                )}
              </div>
            )}
          </section>

          {/* Policies */}
          <PolicyPanel
            userId={userId}
            policies={policies}
            onRefresh={() => loadPolicies(userId)}
          />
        </div>
      </div>
    </div>
  )
}
