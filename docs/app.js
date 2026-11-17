import {
  DEFAULT_PREFS,
  DEFAULT_TIMER,
  bulkInsert,
  deleteRecord,
  loadAllData,
  putRecord,
  replaceAllBackupData,
  resetDatabase,
  savePreferences,
  saveTimerSnapshot
} from './db.js';
import { esc, renderBarChartSvg, renderDonutChartSvg, renderTrendLineSvg } from './charts.js';

const SUBJECTS = [
  { key: 'MATHEMATICS', displayName: 'Mathematics', shortName: 'Math', color: '#5B8DEF', maxMarks: 50 },
  { key: 'PHYSICS', displayName: 'Physics', shortName: 'Physics', color: '#38BDF8', maxMarks: 25 },
  { key: 'CHEMISTRY', displayName: 'Chemistry', shortName: 'Chem', color: '#34D399', maxMarks: 25 },
  { key: 'ECE', displayName: 'Electronics & Communication (ECE)', shortName: 'ECE', color: '#8B7CF6', maxMarks: 100 }
];

const STATUSES = [
  { key: 'NOT_STARTED', label: 'Not Started', color: '#94A3B8' },
  { key: 'IN_PROGRESS', label: 'In Progress', color: '#FBBF24' },
  { key: 'COMPLETED', label: 'Completed', color: '#34D399' }
];

const PRIORITIES = [
  { key: 'HIGH', label: 'High', color: '#F87171' },
  { key: 'MEDIUM', label: 'Medium', color: '#FBBF24' },
  { key: 'LOW', label: 'Low', color: '#5B8DEF' }
];

const PHASES = [
  { key: 'FOCUS', label: 'Focus Session' },
  { key: 'SHORT_BREAK', label: 'Short Break' },
  { key: 'LONG_BREAK', label: 'Long Break' }
];

const DAY_MS = 86_400_000;

function todayEpochDay() {
  const now = new Date();
  return civilToEpochDay(now.getFullYear(), now.getMonth() + 1, now.getDate());
}

function epochMillisToLocalEpochDay(ms) {
  const d = new Date(ms);
  return civilToEpochDay(d.getFullYear(), d.getMonth() + 1, d.getDate());
}

function civilToEpochDay(year, month, day) {
  let y = year - (month <= 2 ? 1 : 0);
  const era = Math.floor((y >= 0 ? y : y - 399) / 400);
  const yoe = y - era * 400;
  const doy = Math.floor((153 * (month + (month > 2 ? -3 : 9)) + 2) / 5) + day - 1;
  const doe = yoe * 365 + Math.floor(yoe / 4) - Math.floor(yoe / 100) + doy;
  return era * 146097 + doe - 719468;
}

function epochDayToDate(epochDay) {
  const z = epochDay + 719468;
  const era = Math.floor((z >= 0 ? z : z - 146096) / 146097);
  const doe = z - era * 146097;
  const yoe = Math.floor((doe - Math.floor(doe / 1460) + Math.floor(doe / 36524) - Math.floor(doe / 146096)) / 365);
  let y = yoe + era * 400;
  const doy = doe - (365 * yoe + Math.floor(yoe / 4) - Math.floor(yoe / 100));
  const mp = Math.floor((5 * doy + 2) / 153);
  const d = doy - Math.floor((153 * mp + 2) / 5) + 1;
  const m = mp + (mp < 10 ? 3 : -9);
  y += m <= 2 ? 1 : 0;
  return new Date(y, m - 1, d);
}

function epochDayToIso(epochDay) {
  const d = epochDayToDate(epochDay);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function isoToEpochDay(iso) {
  if (!iso || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) return null;
  const [y, m, d] = iso.split('-').map(Number);
  return civilToEpochDay(y, m, d);
}

function formatEpochDay(epochDay) {
  return epochDayToDate(epochDay).toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' });
}

function formatShortDate(epochDay) {
  return epochDayToDate(epochDay).toLocaleDateString(undefined, { day: '2-digit', month: 'short' });
}

function formatWeekdayShort(epochDay) {
  return epochDayToDate(epochDay).toLocaleDateString(undefined, { weekday: 'short' });
}

function formatTimeOfDay(ms) {
  return new Date(ms).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
}

function formatDurationHms(ms) {
  const sec = Math.max(0, Math.floor(ms / 1000));
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  if (h > 0) {
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

function formatHoursDecimal(sec) {
  return `${(sec / 3600).toFixed(1)}h`;
}

function formatHoursCompact(mins) {
  const m = Math.max(0, Math.round(mins));
  const h = Math.floor(m / 60);
  const rem = m % 60;
  if (h === 0) return `${rem}m`;
  if (rem === 0) return `${h}h`;
  return `${h}h ${rem}m`;
}

function getSubject(key) {
  return SUBJECTS.find((s) => s.key === key) || SUBJECTS[3];
}
function getStatus(key) {
  return STATUSES.find((s) => s.key === key) || STATUSES[0];
}
function getPriority(key) {
  return PRIORITIES.find((p) => p.key === key) || PRIORITIES[1];
}

// App State
const state = {
  tab: 'home',
  studySubTab: 'TIMER',
  preferences: { ...DEFAULT_PREFS },
  timerSnapshot: { ...DEFAULT_TIMER },
  topics: [],
  studySessions: [],
  dailyTasks: [],
  mcqSessions: [],
  mockTests: [],
  syllabusFilterSubject: null,
  syllabusFilterStatus: null,
  syllabusFilterPriority: null,
  syllabusSearch: '',
  tasksSelectedDate: todayEpochDay(),
  tasksShowMonth: false,
  calendarYear: new Date().getFullYear(),
  calendarMonth: new Date().getMonth() + 1,
  deferredInstallPrompt: null,
  installDismissed: false
};

// Navigation Icons
const NAV_TABS = [
  { key: 'home', label: 'Home', svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/></svg>' },
  { key: 'syllabus', label: 'Syllabus', svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"/></svg>' },
  { key: 'study', label: 'Study', svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="13" r="8"/><path d="M12 9v4l2.5 2.5"/><path d="M9 2h6"/></svg>' },
  { key: 'analytics', label: 'Analytics', svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 3v18h18"/><path d="m7 15 4-4 4 3 5-6"/></svg>' },
  { key: 'profile', label: 'Profile', svg: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 3.6-7 8-7s8 3 8 7"/></svg>' }
];

function toast(message) {
  const container = document.getElementById('toast-container');
  const el = document.createElement('div');
  el.className = 'toast';
  el.textContent = message;
  container.appendChild(el);
  setTimeout(() => {
    el.remove();
  }, 3200);
}

function applyTheme(themeMode) {
  let effective = 'dark';
  if (themeMode === 'LIGHT') effective = 'light';
  else if (themeMode === 'SYSTEM') {
    effective = window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
  }
  document.documentElement.setAttribute('data-theme', effective);
  const metaTheme = document.querySelector('meta[name="theme-color"]');
  if (metaTheme) metaTheme.setAttribute('content', effective === 'light' ? '#F5F7FC' : '#090D16');
}

function showModal(html, onBind) {
  const backdrop = document.getElementById('modal-backdrop');
  const card = document.getElementById('modal-card');
  card.innerHTML = html;
  backdrop.classList.remove('hidden');
  backdrop.onclick = (e) => {
    if (e.target === backdrop) closeModal();
  };
  if (onBind) onBind(card);
}

function closeModal() {
  const backdrop = document.getElementById('modal-backdrop');
  backdrop.classList.add('hidden');
  document.getElementById('modal-card').innerHTML = '';
}

function computeAggregates() {
  const today = todayEpochDay();
  const subjectSummaries = SUBJECTS.map((sub) => {
    const list = state.topics.filter((t) => t.subject === sub.key);
    const completed = list.filter((t) => t.status === 'COMPLETED').length;
    const inProgress = list.filter((t) => t.status === 'IN_PROGRESS').length;
    const notStarted = list.filter((t) => t.status === 'NOT_STARTED').length;
    const total = list.length;
    const pending = inProgress + notStarted;
    const pct = total > 0 ? (completed * 100) / total : 0;
    return { subject: sub, total, completed, inProgress, notStarted, pending, pct };
  });

  const totalTopics = state.topics.length;
  const completedTopics = state.topics.filter((t) => t.status === 'COMPLETED').length;
  const inProgressTopics = state.topics.filter((t) => t.status === 'IN_PROGRESS').length;
  const notStartedTopics = totalTopics - completedTopics - inProgressTopics;
  const pendingTopics = totalTopics - completedTopics;
  const overallPct = totalTopics > 0 ? (completedTopics * 100) / totalTopics : 0;

  const totalStudySeconds = state.studySessions.reduce((acc, s) => acc + (s.durationSeconds || 0), 0);
  const todayStudySeconds = state.studySessions
    .filter((s) => s.dateEpochDay === today)
    .reduce((acc, s) => acc + (s.durationSeconds || 0), 0);
  const weeklyStudySeconds = state.studySessions
    .filter((s) => s.dateEpochDay >= today - 6 && s.dateEpochDay <= today)
    .reduce((acc, s) => acc + (s.durationSeconds || 0), 0);
  const targetSec = Math.max(60, state.preferences.dailyStudyTargetMinutes * 60);
  const dailyTargetPct = (todayStudySeconds / targetSec) * 100;

  const sessionsByDay = {};
  state.studySessions.forEach((s) => {
    sessionsByDay[s.dateEpochDay] = (sessionsByDay[s.dateEpochDay] || 0) + (s.durationSeconds || 0);
  });
  const completedTasksByDay = {};
  state.dailyTasks
    .filter((t) => t.isCompleted)
    .forEach((t) => {
      completedTasksByDay[t.dateEpochDay] = (completedTasksByDay[t.dateEpochDay] || 0) + 1;
    });

  const activeDaySet = new Set([
    ...Object.keys(sessionsByDay).filter((k) => sessionsByDay[k] > 0).map(Number),
    ...Object.keys(completedTasksByDay).map(Number)
  ]);

  let currentStreak = 0;
  let checkDay = activeDaySet.has(today) ? today : today - 1;
  while (activeDaySet.has(checkDay)) {
    currentStreak++;
    checkDay--;
  }

  let bestStreak = 0;
  let run = 0;
  let prev = null;
  Array.from(activeDaySet)
    .sort((a, b) => a - b)
    .forEach((d) => {
      run = prev !== null && d === prev + 1 ? run + 1 : 1;
      if (run > bestStreak) bestStreak = run;
      prev = d;
    });

  let activeLast30 = 0;
  for (let d = today - 29; d <= today; d++) {
    if (activeDaySet.has(d)) activeLast30++;
  }

  const weeklyDays = [];
  for (let d = today - 6; d <= today; d++) {
    const sec = sessionsByDay[d] || 0;
    weeklyDays.push({
      epochDay: d,
      label: formatWeekdayShort(d).slice(0, 3),
      studyHours: sec / 3600,
      studyMinutes: Math.floor(sec / 60),
      tasksCompleted: completedTasksByDay[d] || 0
    });
  }

  const monthlyBuckets = [];
  for (let w = 0; w < 4; w++) {
    const endD = today - (3 - w) * 7;
    const startD = endD - 6;
    let sec = 0;
    for (let d = startD; d <= endD; d++) sec += sessionsByDay[d] || 0;
    monthlyBuckets.push({
      epochDay: startD,
      label: w === 3 ? 'This Wk' : formatShortDate(startD),
      studyHours: sec / 3600
    });
  }

  const heatmapDays = [];
  for (let d = today - 83; d <= today; d++) {
    const sec = sessionsByDay[d] || 0;
    heatmapDays.push({
      epochDay: d,
      studyMinutes: Math.floor(sec / 60),
      tasksCompleted: completedTasksByDay[d] || 0
    });
  }

  const mcqAccuracySeries = [...state.mcqSessions]
    .sort((a, b) => a.dateEpochDay - b.dateEpochDay || (a.createdAtMs || 0) - (b.createdAtMs || 0))
    .slice(-12)
    .map((m) => ({ label: formatShortDate(m.dateEpochDay), value: m.accuracyPercent || 0 }));

  const mockScoreSeries = [...state.mockTests]
    .sort((a, b) => a.testDateEpochDay - b.testDateEpochDay || (a.createdAtMs || 0) - (b.createdAtMs || 0))
    .slice(-12)
    .map((m) => ({
      label: formatShortDate(m.testDateEpochDay),
      value: m.maxTotalMarks > 0 ? (m.totalMarks / m.maxTotalMarks) * 100 : 0
    }));

  return {
    today,
    subjectSummaries,
    totalTopics,
    completedTopics,
    inProgressTopics,
    notStartedTopics,
    pendingTopics,
    overallPct,
    totalStudySeconds,
    todayStudySeconds,
    weeklyStudySeconds,
    dailyTargetPct,
    currentStreak,
    bestStreak,
    activeLast30,
    weeklyDays,
    monthlyBuckets,
    heatmapDays,
    mcqAccuracySeries,
    mockScoreSeries
  };
}

function getTimerComputed() {
  const snap = state.timerSnapshot;
  const prefs = state.preferences;
  const now = Date.now();
  const elapsedMs = snap.isRunning ? Math.max(0, snap.accumulatedMs + (now - snap.startWallMs)) : Math.max(0, snap.accumulatedMs);
  const mins =
    snap.phase === 'FOCUS'
      ? prefs.pomodoroFocusMinutes
      : snap.phase === 'SHORT_BREAK'
      ? prefs.pomodoroShortBreakMinutes
      : prefs.pomodoroLongBreakMinutes;
  const targetMs = mins * 60_000;
  const remainingMs = Math.max(0, targetMs - elapsedMs);
  return { ...snap, elapsedMs, targetMs, remainingMs };
}

let timerHandlingCompletion = false;
async function evaluateTimerTick() {
  const snap = state.timerSnapshot;
  if (!snap.isRunning || timerHandlingCompletion) return;

  const computed = getTimerComputed();
  if (snap.mode === 'POMODORO' && computed.elapsedMs >= computed.targetMs) {
    timerHandlingCompletion = true;
    try {
      const now = Date.now();
      const prefs = state.preferences;
      if (snap.phase === 'FOCUS') {
        const endWall = Math.min(now, snap.startWallMs + (computed.targetMs - snap.accumulatedMs));
        const session = {
          subject: snap.subject,
          topicId: snap.topicId || null,
          topicTitle: snap.topicTitle || null,
          mode: 'POMODORO',
          durationSeconds: Math.max(1, Math.floor(computed.targetMs / 1000)),
          startedAtMs: endWall - computed.targetMs,
          endedAtMs: endWall,
          dateEpochDay: epochMillisToLocalEpochDay(endWall)
        };
        await putRecord('studySessions', session);
        const nextPhase = snap.pomodoroRound % 4 === 0 ? 'LONG_BREAK' : 'SHORT_BREAK';
        state.timerSnapshot = { ...snap, isRunning: false, phase: nextPhase, startWallMs: 0, accumulatedMs: 0 };
        await saveTimerSnapshot(state.timerSnapshot);
        await reloadData();
        const msg = `${getSubject(snap.subject).shortName} focus (${prefs.pomodoroFocusMinutes}m) logged!`;
        toast(msg);
        sendBrowserNotification('Focus Session Complete', msg);
      } else {
        const nextRound = snap.phase === 'LONG_BREAK' ? 1 : snap.pomodoroRound + 1;
        state.timerSnapshot = {
          ...snap,
          isRunning: false,
          phase: 'FOCUS',
          startWallMs: 0,
          accumulatedMs: 0,
          pomodoroRound: nextRound
        };
        await saveTimerSnapshot(state.timerSnapshot);
        const msg = `Break finished! Ready for Focus Round #${nextRound}.`;
        toast(msg);
        sendBrowserNotification('Break Complete', msg);
      }
      render();
    } finally {
      timerHandlingCompletion = false;
    }
    return;
  }

  // Lightweight DOM update for live timer readout without rebuilding the entire page
  const readoutEl = document.getElementById('live-timer-readout');
  const bannerEl = document.getElementById('home-live-timer');
  const displayMs = snap.mode === 'POMODORO' ? computed.remainingMs : computed.elapsedMs;
  if (readoutEl) readoutEl.textContent = formatDurationHms(displayMs);
  if (bannerEl) bannerEl.textContent = formatDurationHms(displayMs);
  const ringEl = document.getElementById('live-timer-ring');
  if (ringEl) {
    const r = 92;
    const c = 2 * Math.PI * r;
    const progress =
      snap.mode === 'POMODORO' && computed.targetMs > 0
        ? Math.min(1, computed.elapsedMs / computed.targetMs)
        : (computed.elapsedMs % 3_600_000) / 3_600_000;
    ringEl.setAttribute('stroke-dasharray', `${(progress * c).toFixed(2)} ${c.toFixed(2)}`);
  }
}

function sendBrowserNotification(title, body) {
  if (!state.preferences.pomodoroAlertsEnabled) return;
  if ('Notification' in window && Notification.permission === 'granted') {
    try {
      new Notification(title, { body, icon: './icon-192.png' });
    } catch (_) {}
  }
}

async function reloadData() {
  const data = await loadAllData();
  state.preferences = data.preferences;
  state.timerSnapshot = data.timerSnapshot;
  state.topics = data.topics;
  state.studySessions = data.studySessions;
  state.dailyTasks = data.dailyTasks;
  state.mcqSessions = data.mcqSessions;
  state.mockTests = data.mockTests;
  applyTheme(state.preferences.themeMode);
}

function renderNav() {
  const bottom = document.getElementById('bottom-nav');
  const rail = document.getElementById('rail-nav-items');
  bottom.innerHTML = NAV_TABS.map(
    (t) => `
    <button class="nav-btn ${state.tab === t.key ? 'active' : ''}" data-nav="${t.key}" type="button">
      <span class="nav-pill">${t.svg}</span>
      <span>${t.label}</span>
    </button>
  `
  ).join('');
  rail.innerHTML = NAV_TABS.map(
    (t) => `
    <button class="nav-btn ${state.tab === t.key ? 'active' : ''}" data-nav="${t.key}" type="button">
      <span class="nav-pill">${t.svg}</span>
      <span>${t.label}</span>
    </button>
  `
  ).join('');
  document.querySelectorAll('[data-nav]').forEach((btn) => {
    btn.onclick = () => {
      state.tab = btn.getAttribute('data-nav');
      render();
      window.scrollTo(0, 0);
    };
  });
}

function render() {
  renderNav();
  const main = document.getElementById('main-content');
  const agg = computeAggregates();

  if (state.tab === 'home') renderHome(main, agg);
  else if (state.tab === 'syllabus') renderSyllabus(main, agg);
  else if (state.tab === 'study') renderStudyHub(main, agg);
  else if (state.tab === 'analytics') renderAnalytics(main, agg);
  else renderProfile(main, agg);
}

// 1. HOME SCREEN
function renderHome(main, agg) {
  const prefs = state.preferences;
  const timer = getTimerComputed();
  const examDay = prefs.examDateEpochDay;
  const daysRemaining = examDay !== null && examDay !== undefined ? examDay - agg.today : null;
  const todayTasks = state.dailyTasks.filter((t) => t.dateEpochDay === agg.today);
  const doneToday = todayTasks.filter((t) => t.isCompleted).length;

  main.innerHTML = `
    <div class="row-between">
      <div>
        <div style="font-size:11px;font-weight:700;color:var(--primary);letter-spacing:0.6px;">${esc(prefs.targetExam.toUpperCase())}</div>
        <h1 class="h1">Welcome, ${esc(prefs.studentName)}</h1>
        <div class="muted">${esc(prefs.branch)}</div>
      </div>
      ${agg.currentStreak > 0 ? `<span class="badge mono" style="background:var(--warning-soft);color:var(--warning);border-color:rgba(251,191,36,0.4);">🔥 ${agg.currentStreak}d</span>` : ''}
    </div>

    ${
      timer.isRunning || timer.elapsedMs > 0
        ? `
      <div class="card row-between" id="banner-open-timer" style="cursor:pointer;background:var(--primary-soft);border-color:var(--primary);">
        <div>
          <div class="h3">${timer.isRunning ? 'Studying' : 'Paused'} · ${esc(getSubject(timer.subject).shortName)}</div>
          <div class="muted">${esc(timer.topicTitle || timer.mode)}</div>
        </div>
        <div id="home-live-timer" class="mono h2" style="color:var(--primary);">${formatDurationHms(timer.mode === 'POMODORO' ? timer.remainingMs : timer.elapsedMs)}</div>
      </div>
    `
        : ''
    }

    <div class="card card-hero">
      <div class="row-between">
        <span style="font-size:11.5px;font-weight:700;color:var(--primary);letter-spacing:0.5px;">TG ECET 2027 COUNTDOWN</span>
        <button id="btn-set-exam-date" class="btn btn-tonal btn-sm" type="button">${examDay === null ? 'Set Date' : 'Update'}</button>
      </div>
      <div style="margin-top:10px;">
        ${
          daysRemaining === null
            ? `<div class="h2">Exam date not set</div><p class="muted" style="margin-top:4px;">Set your official TG ECET 2027 exam date once announced to track your countdown.</p>`
            : `<div style="display:flex;align-items:flex-end;gap:12px;">
                 <span class="mono countdown-num">${daysRemaining > 0 ? daysRemaining : daysRemaining === 0 ? 'TODAY' : '0'}</span>
                 <div style="padding-bottom:4px;">
                   <div class="h3">${daysRemaining === 1 ? 'day remaining' : daysRemaining > 1 ? 'days remaining' : daysRemaining === 0 ? 'Exam Day' : `Exam passed (${-daysRemaining}d ago)`}</div>
                   <div class="muted">Target: ${formatEpochDay(examDay)}</div>
                 </div>
               </div>`
        }
      </div>
    </div>

    <div>
      <div class="h2" style="margin-bottom:8px;">Quick Actions</div>
      <div class="grid-3">
        <button class="quick-tile" id="qa-timer" type="button">
          <span class="quick-icon" style="background:rgba(91,141,239,0.16);color:#5B8DEF;">▶</span>
          <span>Start Timer</span>
        </button>
        <button class="quick-tile" id="qa-topic" type="button">
          <span class="quick-icon" style="background:rgba(139,124,246,0.16);color:#8B7CF6;">＋</span>
          <span>Add Topic</span>
        </button>
        <button class="quick-tile" id="qa-task" type="button">
          <span class="quick-icon" style="background:rgba(52,211,153,0.16);color:#34D399;">✓</span>
          <span>Add Task</span>
        </button>
      </div>
      <div class="grid-2" style="margin-top:8px;">
        <button class="quick-tile" id="qa-mcq" type="button">
          <span class="quick-icon" style="background:rgba(251,191,36,0.16);color:#FBBF24;">?</span>
          <span>Record MCQ Practice</span>
        </button>
        <button class="quick-tile" id="qa-mock" type="button">
          <span class="quick-icon" style="background:rgba(91,141,239,0.16);color:#5B8DEF;">▤</span>
          <span>Record Mock Test</span>
        </button>
      </div>
    </div>

    <div class="card">
      <div class="row-between">
        <div>
          <div class="h2">Preparation Overview</div>
          <div class="muted">Syllabus completion across 4 subjects</div>
        </div>
        <button class="btn btn-ghost btn-sm" id="btn-home-syllabus" type="button" style="color:var(--primary);">View Syllabus</button>
      </div>
      <div class="row-between" style="margin-top:12px;margin-bottom:6px;">
        <span class="muted">Overall Completion</span>
        <strong class="mono" style="color:var(--primary);">${agg.overallPct.toFixed(1)}%</strong>
      </div>
      <div class="progress-track"><div class="progress-fill" style="width:${Math.min(100, agg.overallPct).toFixed(1)}%;"></div></div>
      <div class="grid-4" style="margin-top:12px;">
        <div class="stat-box"><div class="stat-val mono">${agg.totalTopics}</div><div class="stat-lbl">Total</div></div>
        <div class="stat-box"><div class="stat-val mono" style="color:var(--success);">${agg.completedTopics}</div><div class="stat-lbl">Completed</div></div>
        <div class="stat-box"><div class="stat-val mono" style="color:var(--warning);">${agg.inProgressTopics}</div><div class="stat-lbl">In Progress</div></div>
        <div class="stat-box"><div class="stat-val mono" style="color:var(--text-muted);">${agg.pendingTopics}</div><div class="stat-lbl">Pending</div></div>
      </div>
      <div class="grid-4" style="margin-top:10px;">
        ${agg.subjectSummaries
          .map(
            (s) => `
          <div style="background:${s.subject.color}1A;border-radius:10px;padding:7px 4px;text-align:center;">
            <div style="font-size:11px;font-weight:700;color:${s.subject.color};">${s.subject.shortName}</div>
            <div class="mono" style="font-size:11.5px;">${s.completed}/${s.total}</div>
          </div>
        `
          )
          .join('')}
      </div>
    </div>

    <div class="card">
      <div class="row-between">
        <div class="h2">Study Statistics</div>
        <button class="btn btn-ghost btn-sm" id="btn-home-timer" type="button" style="color:var(--primary);">Open Timer</button>
      </div>
      <div class="grid-3" style="margin-top:10px;">
        <div class="stat-box"><div class="stat-val mono" style="color:var(--primary);">${formatHoursDecimal(agg.todayStudySeconds)}</div><div class="stat-lbl">Today</div></div>
        <div class="stat-box"><div class="stat-val mono" style="color:var(--secondary);">${formatHoursDecimal(agg.weeklyStudySeconds)}</div><div class="stat-lbl">This Week</div></div>
        <div class="stat-box"><div class="stat-val mono" style="color:var(--success);">${formatHoursDecimal(agg.totalStudySeconds)}</div><div class="stat-lbl">Total</div></div>
      </div>
      <div class="row-between" style="margin-top:12px;margin-bottom:6px;">
        <span class="muted">Daily Goal (${formatHoursCompact(prefs.dailyStudyTargetMinutes)})</span>
        <strong class="mono" style="color:${agg.dailyTargetPct >= 100 ? 'var(--success)' : 'var(--primary)'};">${agg.dailyTargetPct.toFixed(0)}%</strong>
      </div>
      <div class="progress-track"><div class="progress-fill" style="width:${Math.min(100, agg.dailyTargetPct).toFixed(1)}%;background:${agg.dailyTargetPct >= 100 ? 'var(--success)' : 'var(--secondary)'};"></div></div>
    </div>

    <div class="card">
      <div class="row-between">
        <div>
          <div class="h2">Today's Tasks</div>
          <div class="muted">${todayTasks.length ? `${doneToday} of ${todayTasks.length} completed` : 'No tasks scheduled for today'}</div>
        </div>
        <button class="btn btn-ghost btn-sm" id="btn-home-tasks" type="button" style="color:var(--primary);">Manage</button>
      </div>
      ${
        todayTasks.length === 0
          ? `<button class="btn btn-primary btn-full" id="btn-home-first-task" style="margin-top:10px;" type="button">Plan Today's First Task</button>`
          : `<div style="margin-top:10px;display:flex;flex-direction:column;gap:8px;">
              ${todayTasks
                .slice(0, 4)
                .map(
                  (t) => `
                <div class="row-between">
                  <label style="display:flex;align-items:center;gap:10px;flex:1;cursor:pointer;">
                    <input type="checkbox" data-toggle-home-task="${t.id}" ${t.isCompleted ? 'checked' : ''} style="width:20px;height:20px;accent-color:var(--success);" />
                    <span style="${t.isCompleted ? 'text-decoration:line-through;color:var(--text-muted);' : ''}">${esc(t.title)}</span>
                  </label>
                  ${t.subject ? `<span class="badge" style="color:${getSubject(t.subject).color};background:${getSubject(t.subject).color}20;">${getSubject(t.subject).shortName}</span>` : ''}
                </div>
              `
                )
                .join('')}
            </div>`
      }
    </div>
  `;

  document.getElementById('btn-set-exam-date').onclick = () => openExamDateModal();
  const banner = document.getElementById('banner-open-timer');
  if (banner) banner.onclick = () => { state.tab = 'study'; state.studySubTab = 'TIMER'; render(); };
  document.getElementById('qa-timer').onclick = () => { state.tab = 'study'; state.studySubTab = 'TIMER'; render(); };
  document.getElementById('qa-topic').onclick = () => { state.tab = 'syllabus'; render(); openTopicModal(); };
  document.getElementById('qa-task').onclick = () => { state.tab = 'study'; state.studySubTab = 'TASKS'; state.tasksSelectedDate = todayEpochDay(); render(); openTaskModal(); };
  document.getElementById('qa-mcq').onclick = () => { state.tab = 'study'; state.studySubTab = 'MCQ'; render(); openMcqModal(); };
  document.getElementById('qa-mock').onclick = () => { state.tab = 'study'; state.studySubTab = 'MOCKS'; render(); openMockModal(); };
  document.getElementById('btn-home-syllabus').onclick = () => { state.tab = 'syllabus'; render(); };
  document.getElementById('btn-home-timer').onclick = () => { state.tab = 'study'; state.studySubTab = 'TIMER'; render(); };
  document.getElementById('btn-home-tasks').onclick = () => { state.tab = 'study'; state.studySubTab = 'TASKS'; render(); };
  const firstTaskBtn = document.getElementById('btn-home-first-task');
  if (firstTaskBtn) firstTaskBtn.onclick = () => { state.tab = 'study'; state.studySubTab = 'TASKS'; render(); openTaskModal(); };
  document.querySelectorAll('[data-toggle-home-task]').forEach((cb) => {
    cb.onchange = async () => {
      const id = Number(cb.getAttribute('data-toggle-home-task'));
      const task = state.dailyTasks.find((t) => t.id === id);
      if (task) {
        await putRecord('dailyTasks', { ...task, isCompleted: !task.isCompleted });
        await reloadData();
        render();
      }
    };
  });
}

// 2. SYLLABUS SCREEN
function renderSyllabus(main, agg) {
  const filtered = state.topics.filter((t) => {
    const mSub = !state.syllabusFilterSubject || t.subject === state.syllabusFilterSubject;
    const mStat = !state.syllabusFilterStatus || t.status === state.syllabusFilterStatus;
    const mPrio = !state.syllabusFilterPriority || t.priority === state.syllabusFilterPriority;
    const q = state.syllabusSearch.trim().toLowerCase();
    const mQ = !q || t.title.toLowerCase().includes(q) || (t.notes || '').toLowerCase().includes(q);
    return mSub && mStat && mPrio && mQ;
  });

  main.innerHTML = `
    <div class="row-between">
      <div>
        <h1 class="h1">Syllabus Tracker</h1>
        <div class="muted">Editable 4-subject structure · Import official 2027 topics anytime</div>
      </div>
      <button id="btn-import-syllabus" class="btn btn-outline btn-sm" type="button">Import</button>
    </div>

    <div class="grid-2">
      ${agg.subjectSummaries
        .map((s) => {
          const active = state.syllabusFilterSubject === s.subject.key;
          return `
          <div class="card" data-filter-sub="${s.subject.key}" style="padding:12px;cursor:pointer;border-color:${active ? s.subject.color : 'var(--border)'};background:${active ? s.subject.color + '1E' : 'var(--card)'};">
            <div class="row-between">
              <strong style="color:${s.subject.color};">${s.subject.shortName}</strong>
              <span class="mono" style="font-size:12.5px;font-weight:700;">${s.pct.toFixed(0)}%</span>
            </div>
            <div class="progress-track" style="height:5px;margin:7px 0;"><div class="progress-fill" style="width:${s.pct.toFixed(1)}%;background:${s.subject.color};"></div></div>
            <div class="muted" style="font-size:11.5px;">${s.completed}/${s.total} done · ${s.pending} pending</div>
          </div>
        `;
        })
        .join('')}
    </div>

    <input id="syllabus-search" class="input" type="search" placeholder="Search topics or notes..." value="${esc(state.syllabusSearch)}" />

    <div class="row-scroll">
      ${STATUSES.map(
        (st) => `<button class="chip ${state.syllabusFilterStatus === st.key ? 'active' : ''}" data-filter-status="${st.key}" type="button">${st.label}</button>`
      ).join('')}
      ${PRIORITIES.map(
        (pr) => `<button class="chip ${state.syllabusFilterPriority === pr.key ? 'active' : ''}" data-filter-prio="${pr.key}" type="button">${pr.label} Priority</button>`
      ).join('')}
    </div>

    ${
      filtered.length === 0
        ? `<div class="card" style="text-align:center;padding:24px 16px;">
             <div class="h3">${state.topics.length ? 'No matching topics' : 'Your editable syllabus is empty'}</div>
             <p class="muted" style="margin:6px 0 14px;">${
               state.topics.length
                 ? 'Try clearing your search or status/subject filters.'
                 : "We don't invent unverified 2027 topics. Add your own topics or paste/import the official TG ECET 2027 syllabus."
             }</p>
             <button id="btn-empty-syllabus" class="btn btn-primary" type="button">${state.topics.length ? 'Clear Filters' : 'Add First Topic'}</button>
           </div>`
        : filtered
            .map((t) => {
              const sub = getSubject(t.subject);
              const prio = getPriority(t.priority);
              const stat = getStatus(t.status);
              return `
              <div class="card">
                <div class="row-between" style="align-items:flex-start;">
                  <div style="display:flex;gap:10px;flex:1;">
                    <input type="checkbox" data-check-topic="${t.id}" ${t.status === 'COMPLETED' ? 'checked' : ''} style="width:20px;height:20px;margin-top:2px;accent-color:var(--success);" />
                    <div style="flex:1;">
                      <div class="row-wrap" style="gap:6px;margin-bottom:6px;">
                        <span class="badge" style="color:${sub.color};background:${sub.color}20;border-color:${sub.color}50;"><span class="dot" style="background:${sub.color};"></span>${sub.shortName}</span>
                        <span class="badge" style="color:${prio.color};background:${prio.color}20;">${prio.label}</span>
                      </div>
                      <div class="h3">${esc(t.title)}</div>
                      ${t.notes ? `<div class="muted" style="margin-top:4px;">${esc(t.notes)}</div>` : ''}
                      ${t.targetDateEpochDay ? `<div class="muted" style="margin-top:4px;font-size:11.5px;">Target: ${formatEpochDay(t.targetDateEpochDay)}</div>` : ''}
                    </div>
                  </div>
                </div>
                <div class="row-between" style="margin-top:10px;">
                  <select class="input" data-status-select="${t.id}" style="width:auto;min-height:34px;padding:4px 10px;font-size:12px;color:${stat.color};border-color:${stat.color}66;">
                    ${STATUSES.map((s) => `<option value="${s.key}" ${t.status === s.key ? 'selected' : ''}>${s.label}</option>`).join('')}
                  </select>
                  <div>
                    <button class="icon-btn" data-edit-topic="${t.id}" type="button" aria-label="Edit">✎</button>
                    <button class="icon-btn danger" data-del-topic="${t.id}" type="button" aria-label="Delete">🗑</button>
                  </div>
                </div>
              </div>
            `;
            })
            .join('')
    }

    <button id="fab-add-topic" class="fab" type="button" aria-label="Add Topic">+</button>
  `;

  document.getElementById('btn-import-syllabus').onclick = () => openSyllabusImportModal();
  document.getElementById('fab-add-topic').onclick = () => openTopicModal();
  const searchEl = document.getElementById('syllabus-search');
  searchEl.oninput = (e) => {
    state.syllabusSearch = e.target.value;
    render();
    const el = document.getElementById('syllabus-search');
    if (el) { el.focus(); el.setSelectionRange(el.value.length, el.value.length); }
  };
  document.querySelectorAll('[data-filter-sub]').forEach((el) => {
    el.onclick = () => {
      const k = el.getAttribute('data-filter-sub');
      state.syllabusFilterSubject = state.syllabusFilterSubject === k ? null : k;
      render();
    };
  });
  document.querySelectorAll('[data-filter-status]').forEach((el) => {
    el.onclick = () => {
      const k = el.getAttribute('data-filter-status');
      state.syllabusFilterStatus = state.syllabusFilterStatus === k ? null : k;
      render();
    };
  });
  document.querySelectorAll('[data-filter-prio]').forEach((el) => {
    el.onclick = () => {
      const k = el.getAttribute('data-filter-prio');
      state.syllabusFilterPriority = state.syllabusFilterPriority === k ? null : k;
      render();
    };
  });
  const emptyBtn = document.getElementById('btn-empty-syllabus');
  if (emptyBtn) {
    emptyBtn.onclick = () => {
      if (state.topics.length) {
        state.syllabusFilterSubject = null;
        state.syllabusFilterStatus = null;
        state.syllabusFilterPriority = null;
        state.syllabusSearch = '';
        render();
      } else openTopicModal();
    };
  }
  document.querySelectorAll('[data-check-topic]').forEach((cb) => {
    cb.onchange = async () => {
      const id = Number(cb.getAttribute('data-check-topic'));
      const topic = state.topics.find((x) => x.id === id);
      if (topic) {
        await putRecord('syllabusTopics', {
          ...topic,
          status: topic.status === 'COMPLETED' ? 'NOT_STARTED' : 'COMPLETED',
          updatedAtMs: Date.now()
        });
        await reloadData();
        render();
      }
    };
  });
  document.querySelectorAll('[data-status-select]').forEach((sel) => {
    sel.onchange = async () => {
      const id = Number(sel.getAttribute('data-status-select'));
      const topic = state.topics.find((x) => x.id === id);
      if (topic) {
        await putRecord('syllabusTopics', { ...topic, status: sel.value, updatedAtMs: Date.now() });
        await reloadData();
        render();
      }
    };
  });
  document.querySelectorAll('[data-edit-topic]').forEach((btn) => {
    btn.onclick = () => openTopicModal(state.topics.find((x) => x.id === Number(btn.getAttribute('data-edit-topic'))));
  });
  document.querySelectorAll('[data-del-topic]').forEach((btn) => {
    btn.onclick = () => {
      const t = state.topics.find((x) => x.id === Number(btn.getAttribute('data-del-topic')));
      if (t) {
        confirmModal('Delete Topic', `Remove "${t.title}" from your syllabus?`, async () => {
          await deleteRecord('syllabusTopics', t.id);
          await reloadData();
          toast('Topic deleted.');
          render();
        });
      }
    };
  });
}

// 3. STUDY HUB
function renderStudyHub(main, agg) {
  const subTabs = [
    { key: 'TIMER', label: 'Timer' },
    { key: 'TASKS', label: 'Tasks' },
    { key: 'MCQ', label: 'MCQ' },
    { key: 'MOCKS', label: 'Mocks' }
  ];
  main.innerHTML = `
    <div class="segmented">
      ${subTabs.map((t) => `<button class="seg-btn ${state.studySubTab === t.key ? 'active' : ''}" data-subtab="${t.key}" type="button">${t.label}</button>`).join('')}
    </div>
    <div id="study-sub-body" style="display:flex;flex-direction:column;gap:14px;"></div>
  `;
  document.querySelectorAll('[data-subtab]').forEach((btn) => {
    btn.onclick = () => {
      state.studySubTab = btn.getAttribute('data-subtab');
      render();
    };
  });
  const body = document.getElementById('study-sub-body');
  if (state.studySubTab === 'TIMER') renderTimerSection(body, agg);
  else if (state.studySubTab === 'TASKS') renderTasksSection(body, agg);
  else if (state.studySubTab === 'MCQ') renderMcqSection(body, agg);
  else renderMocksSection(body, agg);
}

function renderTimerSection(container, agg) {
  const timer = getTimerComputed();
  const prefs = state.preferences;
  const sub = getSubject(timer.subject);
  const subjectTopics = state.topics.filter((t) => t.subject === timer.subject);
  const accent = timer.mode === 'POMODORO' && timer.phase !== 'FOCUS' ? '#34D399' : sub.color;
  const r = 92;
  const c = 2 * Math.PI * r;
  const progress =
    timer.mode === 'POMODORO' && timer.targetMs > 0
      ? Math.min(1, timer.elapsedMs / timer.targetMs)
      : (timer.elapsedMs % 3_600_000) / 3_600_000;
  const displayMs = timer.mode === 'POMODORO' ? timer.remainingMs : timer.elapsedMs;

  container.innerHTML = `
    <div class="row-between">
      <div class="row-wrap">
        <button class="chip ${timer.mode === 'STOPWATCH' ? 'active' : ''}" id="mode-stopwatch" type="button">Stopwatch</button>
        <button class="chip ${timer.mode === 'POMODORO' ? 'active' : ''}" id="mode-pomodoro" type="button">Pomodoro</button>
      </div>
      ${timer.mode === 'POMODORO' ? `<button class="btn btn-outline btn-sm" id="btn-pomo-settings" type="button">Customize</button>` : ''}
    </div>

    ${
      timer.mode === 'POMODORO'
        ? `<div class="row-scroll">
            ${PHASES.map((p) => {
              const mins = p.key === 'FOCUS' ? prefs.pomodoroFocusMinutes : p.key === 'SHORT_BREAK' ? prefs.pomodoroShortBreakMinutes : prefs.pomodoroLongBreakMinutes;
              return `<button class="chip ${timer.phase === p.key ? 'active' : ''}" data-pomo-phase="${p.key}" type="button">${p.label} (${mins}m)</button>`;
            }).join('')}
          </div>`
        : ''
    }

    <div class="card">
      <div class="muted" style="margin-bottom:8px;">Study Subject & Topic</div>
      <div class="row-scroll" style="margin-bottom:10px;">
        ${SUBJECTS.map((s) => `<button class="chip ${timer.subject === s.key ? 'active' : ''}" data-timer-sub="${s.key}" type="button">${s.shortName}</button>`).join('')}
      </div>
      <select id="timer-topic-select" class="input">
        <option value="">General ${sub.shortName} Study</option>
        ${subjectTopics.map((t) => `<option value="${t.id}" ${timer.topicId === t.id ? 'selected' : ''}>${esc(t.title)}</option>`).join('')}
      </select>
    </div>

    <div class="card">
      <div class="timer-ring-wrap">
        <svg viewBox="0 0 216 216" style="width:216px;height:216px;">
          <circle cx="108" cy="108" r="${r}" fill="none" stroke="var(--border)" stroke-width="12" />
          <circle id="live-timer-ring" cx="108" cy="108" r="${r}" fill="none" stroke="${accent}" stroke-width="12" stroke-linecap="round" stroke-dasharray="${(progress * c).toFixed(2)} ${c.toFixed(2)}" transform="rotate(-90 108 108)" />
        </svg>
        <div class="timer-ring-center">
          <div class="muted" style="font-size:11.5px;">${timer.mode === 'POMODORO' ? `${PHASES.find((p) => p.key === timer.phase)?.label} · #${timer.pomodoroRound}` : 'Focus Stopwatch'}</div>
          <div id="live-timer-readout" class="mono timer-readout">${formatDurationHms(displayMs)}</div>
          <div style="font-size:11.5px;font-weight:600;color:${accent};">${timer.isRunning ? 'Running (offline-safe)' : timer.elapsedMs > 0 ? 'Paused' : 'Ready'}</div>
        </div>
      </div>
      <div style="display:flex;gap:10px;">
        ${
          timer.isRunning
            ? `<button id="btn-timer-pause" class="btn btn-secondary" style="flex:1;" type="button">Pause</button>`
            : `<button id="btn-timer-start" class="btn btn-primary" style="flex:1;" type="button">${timer.elapsedMs > 0 ? 'Resume' : 'Start'}</button>`
        }
        <button id="btn-timer-reset" class="btn btn-outline" ${timer.elapsedMs > 0 || timer.isRunning ? '' : 'disabled'} type="button">Reset</button>
      </div>
      ${
        timer.elapsedMs >= 10_000 && (timer.mode === 'STOPWATCH' || timer.phase === 'FOCUS')
          ? `<button id="btn-timer-save" class="btn btn-tonal btn-full" style="margin-top:10px;" type="button">Finish & Log Session (${formatDurationHms(timer.elapsedMs)})</button>`
          : ''
      }
    </div>

    <div class="grid-2">
      <div class="card"><div class="muted">Today's Study</div><div class="mono h2" style="color:var(--primary);margin-top:4px;">${formatHoursDecimal(agg.todayStudySeconds)}</div></div>
      <div class="card"><div class="muted">Weekly Study</div><div class="mono h2" style="color:var(--secondary);margin-top:4px;">${formatHoursDecimal(agg.weeklyStudySeconds)}</div></div>
    </div>

    <div class="h2">Session History (${state.studySessions.length})</div>
    ${
      state.studySessions.length === 0
        ? `<div class="card" style="text-align:center;padding:24px 16px;"><div class="h3">No study sessions logged yet</div><p class="muted" style="margin-top:4px;">Start the Stopwatch or Pomodoro timer and save your session to build your study log.</p></div>`
        : state.studySessions
            .slice(0, 30)
            .map((s) => {
              const ss = getSubject(s.subject);
              return `
              <div class="card row-between">
                <div style="flex:1;min-width:0;">
                  <div class="row-wrap" style="gap:6px;margin-bottom:4px;">
                    <span class="badge" style="color:${ss.color};background:${ss.color}20;">${ss.shortName}</span>
                    <span class="muted" style="font-size:11.5px;">${s.mode === 'POMODORO' ? 'Pomodoro' : 'Stopwatch'}</span>
                  </div>
                  <div class="h3">${esc(s.topicTitle || `General ${ss.shortName} Study`)}</div>
                  <div class="muted">${formatEpochDay(s.dateEpochDay)} · ${formatTimeOfDay(s.endedAtMs)}</div>
                </div>
                <div style="display:flex;align-items:center;gap:6px;">
                  <span class="mono h3" style="color:var(--success);">${formatDurationHms(s.durationSeconds * 1000)}</span>
                  <button class="icon-btn danger" data-del-session="${s.id}" type="button">🗑</button>
                </div>
              </div>
            `;
            })
            .join('')
    }
  `;

  document.getElementById('mode-stopwatch').onclick = async () => {
    state.timerSnapshot = { ...state.timerSnapshot, isRunning: false, mode: 'STOPWATCH', phase: 'FOCUS', startWallMs: 0, accumulatedMs: 0 };
    await saveTimerSnapshot(state.timerSnapshot);
    render();
  };
  document.getElementById('mode-pomodoro').onclick = async () => {
    state.timerSnapshot = { ...state.timerSnapshot, isRunning: false, mode: 'POMODORO', phase: 'FOCUS', startWallMs: 0, accumulatedMs: 0 };
    await saveTimerSnapshot(state.timerSnapshot);
    render();
  };
  const pomoBtn = document.getElementById('btn-pomo-settings');
  if (pomoBtn) pomoBtn.onclick = () => openPomodoroModal();
  document.querySelectorAll('[data-pomo-phase]').forEach((b) => {
    b.onclick = async () => {
      state.timerSnapshot = { ...state.timerSnapshot, isRunning: false, mode: 'POMODORO', phase: b.getAttribute('data-pomo-phase'), startWallMs: 0, accumulatedMs: 0 };
      await saveTimerSnapshot(state.timerSnapshot);
      render();
    };
  });
  document.querySelectorAll('[data-timer-sub]').forEach((b) => {
    b.onclick = async () => {
      state.timerSnapshot = { ...state.timerSnapshot, subject: b.getAttribute('data-timer-sub'), topicId: null, topicTitle: null };
      await saveTimerSnapshot(state.timerSnapshot);
      render();
    };
  });
  document.getElementById('timer-topic-select').onchange = async (e) => {
    const id = e.target.value ? Number(e.target.value) : null;
    const topic = state.topics.find((t) => t.id === id);
    state.timerSnapshot = { ...state.timerSnapshot, topicId: topic ? topic.id : null, topicTitle: topic ? topic.title : null };
    await saveTimerSnapshot(state.timerSnapshot);
  };
  const startBtn = document.getElementById('btn-timer-start');
  if (startBtn) {
    startBtn.onclick = async () => {
      state.timerSnapshot = { ...state.timerSnapshot, isRunning: true, startWallMs: Date.now() };
      await saveTimerSnapshot(state.timerSnapshot);
      render();
    };
  }
  const pauseBtn = document.getElementById('btn-timer-pause');
  if (pauseBtn) {
    pauseBtn.onclick = async () => {
      const comp = getTimerComputed();
      state.timerSnapshot = { ...state.timerSnapshot, isRunning: false, startWallMs: 0, accumulatedMs: comp.elapsedMs };
      await saveTimerSnapshot(state.timerSnapshot);
      render();
    };
  }
  document.getElementById('btn-timer-reset').onclick = async () => {
    state.timerSnapshot = { ...state.timerSnapshot, isRunning: false, startWallMs: 0, accumulatedMs: 0 };
    await saveTimerSnapshot(state.timerSnapshot);
    render();
  };
  const saveBtn = document.getElementById('btn-timer-save');
  if (saveBtn) {
    saveBtn.onclick = async () => {
      const comp = getTimerComputed();
      const sec = Math.floor(comp.elapsedMs / 1000);
      if (sec < 10) return toast('Study at least 10 seconds to record a session.');
      const now = Date.now();
      await putRecord('studySessions', {
        subject: comp.subject,
        topicId: comp.topicId,
        topicTitle: comp.topicTitle,
        mode: comp.mode,
        durationSeconds: sec,
        startedAtMs: now - comp.elapsedMs,
        endedAtMs: now,
        dateEpochDay: epochMillisToLocalEpochDay(now)
      });
      state.timerSnapshot = { ...state.timerSnapshot, isRunning: false, startWallMs: 0, accumulatedMs: 0 };
      await saveTimerSnapshot(state.timerSnapshot);
      await reloadData();
      toast(`Saved ${formatDurationHms(comp.elapsedMs)} for ${getSubject(comp.subject).shortName}.`);
      render();
    };
  }
  document.querySelectorAll('[data-del-session]').forEach((b) => {
    b.onclick = () => {
      const id = Number(b.getAttribute('data-del-session'));
      confirmModal('Delete Study Session', 'Remove this study session from history?', async () => {
        await deleteRecord('studySessions', id);
        await reloadData();
        toast('Study session removed.');
        render();
      });
    };
  });
}

function renderTasksSection(container, agg) {
  const selDate = state.tasksSelectedDate;
  const tasksForDay = state.dailyTasks.filter((t) => t.dateEpochDay === selDate);
  const doneCount = tasksForDay.filter((t) => t.isCompleted).length;
  const pendingCount = tasksForDay.length - doneCount;
  const studySec = state.studySessions.filter((s) => s.dateEpochDay === selDate).reduce((a, b) => a + b.durationSeconds, 0);
  const targetPct = (studySec / Math.max(60, state.preferences.dailyStudyTargetMinutes * 60)) * 100;

  const activityDates = new Set([
    ...state.dailyTasks.map((t) => t.dateEpochDay),
    ...state.studySessions.map((s) => s.dateEpochDay)
  ]);

  let calendarHtml = '';
  if (!state.tasksShowMonth) {
    const days = [-3, -2, -1, 0, 1, 2, 3].map((o) => selDate + o);
    calendarHtml = `
      <div class="cal-grid" style="margin-top:10px;">
        ${days
          .map((d) => {
            const dt = epochDayToDate(d);
            const active = d === selDate;
            const hasDot = activityDates.has(d);
            return `
            <button class="cal-day ${active ? 'selected' : ''} ${d === agg.today ? 'today' : ''}" data-pick-day="${d}" type="button">
              <span style="font-size:10px;opacity:0.8;">${formatWeekdayShort(d).slice(0, 2)}</span>
              <strong class="mono">${dt.getDate()}</strong>
              <span class="dot" style="width:4px;height:4px;background:${hasDot ? (active ? 'var(--primary-ink)' : 'var(--success)') : 'transparent'};"></span>
            </button>
          `;
          })
          .join('')}
      </div>
    `;
  } else {
    const first = civilToEpochDay(state.calendarYear, state.calendarMonth, 1);
    const startOff = (((first + 3) % 7) + 7) % 7;
    const daysInMonth = new Date(state.calendarYear, state.calendarMonth, 0).getDate();
    const monthLabel = new Date(state.calendarYear, state.calendarMonth - 1, 1).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
    const cells = [];
    for (let i = 0; i < startOff; i++) cells.push('<div></div>');
    for (let day = 1; day <= daysInMonth; day++) {
      const ep = civilToEpochDay(state.calendarYear, state.calendarMonth, day);
      const active = ep === selDate;
      const hasDot = activityDates.has(ep);
      cells.push(`
        <button class="cal-day ${active ? 'selected' : ''} ${ep === agg.today ? 'today' : ''}" data-pick-day="${ep}" type="button">
          <strong class="mono">${day}</strong>
          <span class="dot" style="width:4px;height:4px;background:${hasDot ? (active ? 'var(--primary-ink)' : 'var(--success)') : 'transparent'};"></span>
        </button>
      `);
    }
    calendarHtml = `
      <div class="row-between" style="margin:10px 0 6px;">
        <button class="icon-btn" id="cal-prev" type="button">‹</button>
        <strong>${monthLabel}</strong>
        <button class="icon-btn" id="cal-next" type="button">›</button>
      </div>
      <div class="cal-grid" style="margin-bottom:4px;">
        ${['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((w) => `<div class="muted" style="text-align:center;font-size:11px;">${w}</div>`).join('')}
      </div>
      <div class="cal-grid">${cells.join('')}</div>
    `;
  }

  container.innerHTML = `
    <div class="card">
      <div class="row-between">
        <div>
          <div class="h2">${formatEpochDay(selDate)}</div>
          <div style="font-size:12px;color:var(--primary);">${selDate === agg.today ? 'Today' : formatWeekdayShort(selDate)}</div>
        </div>
        <div>
          ${selDate !== agg.today ? `<button class="btn btn-ghost btn-sm" id="btn-tasks-today" type="button">Today</button>` : ''}
          <button class="btn btn-tonal btn-sm" id="btn-toggle-cal" type="button">${state.tasksShowMonth ? 'Week' : 'Month'}</button>
        </div>
      </div>
      ${calendarHtml}
    </div>

    <div class="card">
      <div class="row-between">
        <div>
          <div class="h3">Daily Progress</div>
          <div class="muted">Pending: ${pendingCount} · Completed: ${doneCount}</div>
        </div>
        <span class="mono" style="color:var(--primary);font-weight:700;font-size:13px;">${formatHoursDecimal(studySec)} / ${formatHoursCompact(state.preferences.dailyStudyTargetMinutes)}</span>
      </div>
      <div class="progress-track" style="margin-top:8px;"><div class="progress-fill" style="width:${Math.min(100, targetPct).toFixed(1)}%;background:${targetPct >= 100 ? 'var(--success)' : 'var(--primary)'};"></div></div>
    </div>

    ${
      tasksForDay.length === 0
        ? `<div class="card" style="text-align:center;padding:24px 16px;">
             <div class="h3">No tasks for ${formatShortDate(selDate)}</div>
             <p class="muted" style="margin:6px 0 14px;">Add study goals, problem sets, or revision tasks for this date.</p>
             <button class="btn btn-primary" id="btn-empty-task" type="button">Add Task</button>
           </div>`
        : tasksForDay
            .map((t) => {
              const prio = getPriority(t.priority);
              const sub = t.subject ? getSubject(t.subject) : null;
              return `
              <div class="card row-between">
                <label style="display:flex;align-items:flex-start;gap:10px;flex:1;cursor:pointer;">
                  <input type="checkbox" data-toggle-task="${t.id}" ${t.isCompleted ? 'checked' : ''} style="width:20px;height:20px;margin-top:2px;accent-color:var(--success);" />
                  <div>
                    <div class="h3" style="${t.isCompleted ? 'text-decoration:line-through;color:var(--text-muted);' : ''}">${esc(t.title)}</div>
                    <div class="row-wrap" style="margin-top:4px;gap:6px;">
                      <span class="badge" style="color:${prio.color};background:${prio.color}20;">${prio.label}</span>
                      ${sub ? `<span class="badge" style="color:${sub.color};background:${sub.color}20;">${sub.shortName}</span>` : ''}
                    </div>
                  </div>
                </label>
                <div>
                  <button class="icon-btn" data-edit-task="${t.id}" type="button">✎</button>
                  <button class="icon-btn danger" data-del-task="${t.id}" type="button">🗑</button>
                </div>
              </div>
            `;
            })
            .join('')
    }

    <button id="fab-add-task" class="fab" type="button" aria-label="Add Task">+</button>
  `;

  document.getElementById('btn-toggle-cal').onclick = () => { state.tasksShowMonth = !state.tasksShowMonth; render(); };
  const todayBtn = document.getElementById('btn-tasks-today');
  if (todayBtn) todayBtn.onclick = () => { state.tasksSelectedDate = agg.today; render(); };
  const prevBtn = document.getElementById('cal-prev');
  if (prevBtn) prevBtn.onclick = () => { if (state.calendarMonth === 1) { state.calendarMonth = 12; state.calendarYear--; } else state.calendarMonth--; render(); };
  const nextBtn = document.getElementById('cal-next');
  if (nextBtn) nextBtn.onclick = () => { if (state.calendarMonth === 12) { state.calendarMonth = 1; state.calendarYear++; } else state.calendarMonth++; render(); };
  document.querySelectorAll('[data-pick-day]').forEach((b) => {
    b.onclick = () => { state.tasksSelectedDate = Number(b.getAttribute('data-pick-day')); render(); };
  });
  document.getElementById('fab-add-task').onclick = () => openTaskModal();
  const emptyBtn = document.getElementById('btn-empty-task');
  if (emptyBtn) emptyBtn.onclick = () => openTaskModal();
  document.querySelectorAll('[data-toggle-task]').forEach((cb) => {
    cb.onchange = async () => {
      const task = state.dailyTasks.find((t) => t.id === Number(cb.getAttribute('data-toggle-task')));
      if (task) {
        await putRecord('dailyTasks', { ...task, isCompleted: !task.isCompleted });
        await reloadData();
        render();
      }
    };
  });
  document.querySelectorAll('[data-edit-task]').forEach((b) => {
    b.onclick = () => openTaskModal(state.dailyTasks.find((t) => t.id === Number(b.getAttribute('data-edit-task'))));
  });
  document.querySelectorAll('[data-del-task]').forEach((b) => {
    b.onclick = () => {
      const t = state.dailyTasks.find((x) => x.id === Number(b.getAttribute('data-del-task')));
      if (t) {
        confirmModal('Delete Task', `Remove "${t.title}"?`, async () => {
          await deleteRecord('dailyTasks', t.id);
          await reloadData();
          toast('Task deleted.');
          render();
        });
      }
    };
  });
}

function renderMcqSection(container) {
  const totalAtt = state.mcqSessions.reduce((a, b) => a + b.attempted, 0);
  const totalCor = state.mcqSessions.reduce((a, b) => a + b.correct, 0);
  const overallAcc = totalAtt > 0 ? (totalCor * 100) / totalAtt : 0;
  const accColor = (v) => (v >= 75 ? 'var(--success)' : v >= 50 ? 'var(--warning)' : 'var(--danger)');

  const byTopic = {};
  state.mcqSessions.forEach((m) => {
    const k = `${m.subject}::${m.topicName}`;
    if (!byTopic[k]) byTopic[k] = { subject: m.subject, topicName: m.topicName, attempted: 0, correct: 0 };
    byTopic[k].attempted += m.attempted;
    byTopic[k].correct += m.correct;
  });
  const topicPerf = Object.values(byTopic).sort((a, b) => b.attempted - a.attempted);

  container.innerHTML = `
    <div class="card">
      <div class="row-between">
        <div>
          <div class="h2">MCQ Practice Overview</div>
          <div class="muted">Self-logged practice sessions · ${state.mcqSessions.length} logs</div>
        </div>
        <div class="mono h1" style="color:${accColor(overallAcc)};">${overallAcc.toFixed(1)}%</div>
      </div>
      <div class="grid-3" style="margin-top:12px;">
        <div class="stat-box"><div class="stat-val mono">${totalAtt}</div><div class="stat-lbl">Attempted</div></div>
        <div class="stat-box"><div class="stat-val mono" style="color:var(--success);">${totalCor}</div><div class="stat-lbl">Correct</div></div>
        <div class="stat-box"><div class="stat-val mono" style="color:var(--danger);">${Math.max(0, totalAtt - totalCor)}</div><div class="stat-lbl">Incorrect</div></div>
      </div>
    </div>

    <div class="card">
      <div class="h3" style="margin-bottom:10px;">Subject-Wise Accuracy</div>
      <div style="display:flex;flex-direction:column;gap:10px;">
        ${SUBJECTS.map((s) => {
          const list = state.mcqSessions.filter((m) => m.subject === s.key);
          const att = list.reduce((a, b) => a + b.attempted, 0);
          const cor = list.reduce((a, b) => a + b.correct, 0);
          const pct = att > 0 ? (cor * 100) / att : 0;
          return `
            <div>
              <div class="row-between" style="font-size:13px;margin-bottom:4px;">
                <span>${s.shortName} (${cor}/${att})</span>
                <strong class="mono" style="color:${att > 0 ? s.color : 'var(--text-muted)'};">${att > 0 ? pct.toFixed(1) + '%' : '—'}</strong>
              </div>
              <div class="progress-track" style="height:6px;"><div class="progress-fill" style="width:${pct.toFixed(1)}%;background:${s.color};"></div></div>
            </div>
          `;
        }).join('')}
      </div>
    </div>

    ${
      topicPerf.length
        ? `<div class="card">
             <div class="h3" style="margin-bottom:8px;">Topic-Wise Breakdown</div>
             ${topicPerf
               .slice(0, 8)
               .map((tp) => {
                 const s = getSubject(tp.subject);
                 const pct = tp.attempted > 0 ? (tp.correct * 100) / tp.attempted : 0;
                 return `
                 <div class="row-between" style="padding:5px 0;">
                   <div style="display:flex;align-items:center;gap:8px;min-width:0;flex:1;">
                     <span class="badge" style="color:${s.color};background:${s.color}20;">${s.shortName}</span>
                     <span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${esc(tp.topicName)}</span>
                   </div>
                   <strong class="mono" style="font-size:12.5px;color:${accColor(pct)};">${tp.correct}/${tp.attempted} (${pct.toFixed(0)}%)</strong>
                 </div>
               `;
               })
               .join('')}
           </div>`
        : ''
    }

    <div class="h2">Practice History</div>
    ${
      state.mcqSessions.length === 0
        ? `<div class="card" style="text-align:center;padding:24px 16px;">
             <div class="h3">No MCQ practice logged yet</div>
             <p class="muted" style="margin:6px 0 14px;">Record your question sets to automatically compute topic-wise and subject-wise accuracy.</p>
             <button class="btn btn-primary" id="btn-empty-mcq" type="button">Record MCQ Session</button>
           </div>`
        : state.mcqSessions
            .map((m) => {
              const s = getSubject(m.subject);
              return `
              <div class="card row-between" style="align-items:flex-start;">
                <div style="flex:1;">
                  <div class="row-wrap" style="gap:6px;margin-bottom:4px;">
                    <span class="badge" style="color:${s.color};background:${s.color}20;">${s.shortName}</span>
                    <span class="muted">${formatEpochDay(m.dateEpochDay)}</span>
                  </div>
                  <div class="h3">${esc(m.topicName)}</div>
                  <div class="muted" style="margin-top:3px;">Attempted: ${m.attempted} · Correct: ${m.correct} · Wrong: ${m.incorrect} · Unattempted: ${m.unattempted} · ${m.timeTakenMinutes}m</div>
                </div>
                <div style="text-align:right;">
                  <div class="mono h2" style="color:${accColor(m.accuracyPercent)};">${m.accuracyPercent.toFixed(1)}%</div>
                  <button class="icon-btn danger" data-del-mcq="${m.id}" type="button">🗑</button>
                </div>
              </div>
            `;
            })
            .join('')
    }

    <button id="fab-add-mcq" class="fab" type="button" aria-label="Record MCQ">+</button>
  `;

  document.getElementById('fab-add-mcq').onclick = () => openMcqModal();
  const emptyBtn = document.getElementById('btn-empty-mcq');
  if (emptyBtn) emptyBtn.onclick = () => openMcqModal();
  document.querySelectorAll('[data-del-mcq]').forEach((b) => {
    b.onclick = () => {
      const m = state.mcqSessions.find((x) => x.id === Number(b.getAttribute('data-del-mcq')));
      if (m) {
        confirmModal('Delete MCQ Record', `Delete the MCQ log for "${m.topicName}"?`, async () => {
          await deleteRecord('mcqSessions', m.id);
          await reloadData();
          toast('MCQ record deleted.');
          render();
        });
      }
    };
  });
}

function renderMocksSection(container, agg) {
  const sortedDesc = [...state.mockTests].sort((a, b) => b.testDateEpochDay - a.testDateEpochDay || (b.createdAtMs || 0) - (a.createdAtMs || 0));
  const bestPct = state.mockTests.length
    ? Math.max(...state.mockTests.map((m) => (m.maxTotalMarks > 0 ? (m.totalMarks / m.maxTotalMarks) * 100 : 0)))
    : 0;

  container.innerHTML = `
    <div class="card">
      <div class="row-between" style="margin-bottom:10px;">
        <div>
          <div class="h2">Mock Test Performance</div>
          <div class="muted">Score percentage across tests</div>
        </div>
        ${state.mockTests.length ? `<span class="mono" style="color:var(--success);font-weight:700;font-size:13px;">Best: ${bestPct.toFixed(1)}%</span>` : ''}
      </div>
      ${renderTrendLineSvg(agg.mockScoreSeries, '#5B8DEF', 'Log your first mock test to see score trends.')}
    </div>

    <div class="h2">Test History (${sortedDesc.length})</div>
    ${
      sortedDesc.length === 0
        ? `<div class="card" style="text-align:center;padding:24px 16px;">
             <div class="h3">No mock tests recorded yet</div>
             <p class="muted" style="margin:6px 0 14px;">Track full-length or subject mock exams with Math, Physics, Chemistry, and ECE marks.</p>
             <button class="btn btn-primary" id="btn-empty-mock" type="button">Add Mock Test</button>
           </div>`
        : sortedDesc
            .map((t) => {
              const pct = t.maxTotalMarks > 0 ? (t.totalMarks / t.maxTotalMarks) * 100 : 0;
              const acc = t.attempted > 0 ? (t.correct * 100) / t.attempted : 0;
              return `
              <div class="card">
                <div class="row-between" style="align-items:flex-start;">
                  <div>
                    <div class="h3">${esc(t.title)}</div>
                    <div class="muted">${formatEpochDay(t.testDateEpochDay)}</div>
                  </div>
                  <div style="text-align:right;">
                    <div class="mono h2" style="color:var(--primary);">${t.totalMarks} / ${t.maxTotalMarks}</div>
                    <div class="mono" style="font-size:12px;color:var(--success);">${pct.toFixed(1)}%</div>
                  </div>
                </div>
                <div class="grid-4" style="margin-top:10px;">
                  <div class="stat-box" style="padding:6px 4px;"><div style="font-size:11px;color:#5B8DEF;font-weight:700;">Math</div><div class="mono" style="font-size:12px;">${t.mathMarks}/${t.mathMaxMarks}</div></div>
                  <div class="stat-box" style="padding:6px 4px;"><div style="font-size:11px;color:#38BDF8;font-weight:700;">Physics</div><div class="mono" style="font-size:12px;">${t.physicsMarks}/${t.physicsMaxMarks}</div></div>
                  <div class="stat-box" style="padding:6px 4px;"><div style="font-size:11px;color:#34D399;font-weight:700;">Chem</div><div class="mono" style="font-size:12px;">${t.chemistryMarks}/${t.chemistryMaxMarks}</div></div>
                  <div class="stat-box" style="padding:6px 4px;"><div style="font-size:11px;color:#8B7CF6;font-weight:700;">ECE</div><div class="mono" style="font-size:12px;">${t.eceMarks}/${t.eceMaxMarks}</div></div>
                </div>
                <div class="row-between" style="margin-top:10px;">
                  <span class="muted">Attempted ${t.attempted} · Correct ${t.correct} · Wrong ${t.incorrect} · Acc ${acc.toFixed(0)}%</span>
                  <div>
                    <button class="icon-btn" data-edit-mock="${t.id}" type="button">✎</button>
                    <button class="icon-btn danger" data-del-mock="${t.id}" type="button">🗑</button>
                  </div>
                </div>
                ${t.notes ? `<div class="muted" style="margin-top:6px;">${esc(t.notes)}</div>` : ''}
              </div>
            `;
            })
            .join('')
    }

    <button id="fab-add-mock" class="fab" type="button" aria-label="Add Mock Test">+</button>
  `;

  document.getElementById('fab-add-mock').onclick = () => openMockModal();
  const emptyBtn = document.getElementById('btn-empty-mock');
  if (emptyBtn) emptyBtn.onclick = () => openMockModal();
  document.querySelectorAll('[data-edit-mock]').forEach((b) => {
    b.onclick = () => openMockModal(state.mockTests.find((x) => x.id === Number(b.getAttribute('data-edit-mock'))));
  });
  document.querySelectorAll('[data-del-mock]').forEach((b) => {
    b.onclick = () => {
      const t = state.mockTests.find((x) => x.id === Number(b.getAttribute('data-del-mock')));
      if (t) {
        confirmModal('Delete Mock Test', `Delete "${t.title}" from your mock history?`, async () => {
          await deleteRecord('mockTests', t.id);
          await reloadData();
          toast('Mock test deleted.');
          render();
        });
      }
    };
  });
}

// 4. ANALYTICS
function renderAnalytics(main, agg) {
  const targetMins = Math.max(30, state.preferences.dailyStudyTargetMinutes);
  const cols = [];
  for (let i = 0; i < agg.heatmapDays.length; i += 7) {
    cols.push(agg.heatmapDays.slice(i, i + 7));
  }

  main.innerHTML = `
    <div>
      <h1 class="h1">Analytics & Reports</h1>
      <div class="muted">Calculated strictly from your saved study sessions, topics, MCQs, and mocks</div>
    </div>

    <div class="grid-2">
      <div class="card" style="background:var(--warning-soft);border-color:rgba(251,191,36,0.38);">
        <div style="font-size:12px;font-weight:700;color:var(--warning);">🔥 Current Streak</div>
        <div class="mono h1" style="margin-top:6px;">${agg.currentStreak} days</div>
      </div>
      <div class="card">
        <div class="muted">Best Streak</div>
        <div class="mono h1" style="color:var(--primary);margin-top:4px;">${agg.bestStreak} days</div>
        <div class="muted">${agg.activeLast30}/30 active days</div>
      </div>
    </div>

    <div class="card">
      <div class="h2">Daily Consistency Tracker</div>
      <div class="muted" style="margin-bottom:10px;">Days with logged study sessions or completed tasks</div>
      <div class="heatmap-grid">
        ${cols
          .map(
            (week) => `
          <div class="heatmap-col">
            ${week
              .map((m) => {
                const bg =
                  m.studyMinutes === 0 && m.tasksCompleted === 0
                    ? 'var(--border)'
                    : m.studyMinutes >= targetMins
                    ? '#34D399'
                    : m.studyMinutes >= targetMins / 2
                    ? '#5B8DEF'
                    : 'rgba(91,141,239,0.45)';
                return `<div class="heatmap-cell" style="background:${bg};"></div>`;
              })
              .join('')}
          </div>
        `
          )
          .join('')}
      </div>
      <div class="row-between muted" style="margin-top:10px;font-size:11.5px;">
        <span>Last 12 Weeks</span>
        <span style="display:flex;align-items:center;gap:4px;">Less <span style="width:10px;height:10px;border-radius:2px;background:var(--border);display:inline-block;"></span><span style="width:10px;height:10px;border-radius:2px;background:rgba(91,141,239,0.45);display:inline-block;"></span><span style="width:10px;height:10px;border-radius:2px;background:#5B8DEF;display:inline-block;"></span><span style="width:10px;height:10px;border-radius:2px;background:#34D399;display:inline-block;"></span> Goal+</span>
      </div>
    </div>

    <div class="card">
      <div class="h2">Weekly Study Hours</div>
      <div class="muted" style="margin-bottom:10px;">Last 7 days · Dashed line shows daily goal</div>
      ${renderBarChartSvg(agg.weeklyDays, '#5B8DEF', '#8B7CF6', state.preferences.dailyStudyTargetMinutes / 60)}
    </div>

    <div class="card">
      <div class="h2">Monthly Study Hours</div>
      <div class="muted" style="margin-bottom:10px;">4-week rolling breakdown</div>
      ${renderBarChartSvg(agg.monthlyBuckets, '#8B7CF6', '#5B8DEF')}
    </div>

    <div class="card">
      <div class="h2" style="margin-bottom:10px;">Topic Completion Breakdown</div>
      ${renderDonutChartSvg(agg.completedTopics, agg.inProgressTopics, agg.notStartedTopics)}
    </div>

    <div class="card">
      <div class="h2" style="margin-bottom:12px;">Subject-Wise Completion</div>
      <div style="display:flex;flex-direction:column;gap:12px;">
        ${agg.subjectSummaries
          .map(
            (s) => `
          <div>
            <div class="row-between" style="font-size:13px;margin-bottom:4px;">
              <span>${s.subject.displayName}</span>
              <strong class="mono" style="color:${s.subject.color};">${s.completed}/${s.total} (${s.pct.toFixed(0)}%)</strong>
            </div>
            <div class="progress-track"><div class="progress-fill" style="width:${s.pct.toFixed(1)}%;background:${s.subject.color};"></div></div>
          </div>
        `
          )
          .join('')}
      </div>
    </div>

    <div class="card">
      <div class="h2">MCQ Accuracy Trend</div>
      <div class="muted" style="margin-bottom:8px;">Accuracy across your recent practice sessions</div>
      ${renderTrendLineSvg(agg.mcqAccuracySeries, '#34D399', 'Log MCQ practice sessions to plot your accuracy graph.')}
    </div>

    <div class="card">
      <div class="h2">Mock Test Performance Graph</div>
      <div class="muted" style="margin-bottom:8px;">Percentage score across recorded mock exams</div>
      ${renderTrendLineSvg(agg.mockScoreSeries, '#5B8DEF', 'Record mock tests to visualize your score progression.')}
    </div>
  `;
}

// 5. PROFILE & BACKUP
function renderProfile(main) {
  const prefs = state.preferences;
  const hrsVal = prefs.dailyStudyTargetMinutes / 60;
  const hrsStr = Number.isInteger(hrsVal) ? String(hrsVal) : hrsVal.toFixed(1);

  main.innerHTML = `
    <div>
      <h1 class="h1">Profile & Settings</h1>
      <div class="muted">100% offline IndexedDB storage · JSON backup & restore</div>
    </div>

    <div class="card" style="display:flex;flex-direction:column;gap:10px;">
      <div class="h2">Student Profile</div>
      <div class="field"><label>Student Name</label><input id="prof-name" class="input" value="${esc(prefs.studentName)}" /></div>
      <div class="field"><label>Branch</label><input id="prof-branch" class="input" value="${esc(prefs.branch)}" /></div>
      <div class="grid-2">
        <div class="field"><label>Target Exam</label><input id="prof-exam" class="input" value="${esc(prefs.targetExam)}" /></div>
        <div class="field"><label>Daily Goal (hrs)</label><input id="prof-goal" class="input" type="number" step="0.25" value="${hrsStr}" /></div>
      </div>
      <button id="btn-prof-date" class="btn btn-outline btn-full" type="button">${
        prefs.examDateEpochDay ? `Exam Date: ${formatEpochDay(prefs.examDateEpochDay)}` : 'Set Official Exam Date'
      }</button>
      <div id="prof-error" class="error-text hidden"></div>
      <button id="btn-save-profile" class="btn btn-primary btn-full" type="button">Save Profile</button>
    </div>

    <div class="card">
      <div class="h2" style="margin-bottom:10px;">Appearance</div>
      <div class="row-wrap">
        ${['DARK', 'LIGHT', 'SYSTEM']
          .map((m) => `<button class="chip ${prefs.themeMode === m ? 'active' : ''}" data-theme-mode="${m}" type="button">${m[0] + m.slice(1).toLowerCase()}</button>`)
          .join('')}
      </div>
    </div>

    <div class="card" style="display:flex;flex-direction:column;gap:12px;">
      <div class="h2">Notification Preferences</div>
      <div class="switch-row">
        <div>
          <div class="h3">Pomodoro Completion Alerts</div>
          <div class="muted">Notify when focus or break sessions end</div>
        </div>
        <input id="tog-pomo-alerts" class="toggle" type="checkbox" ${prefs.pomodoroAlertsEnabled ? 'checked' : ''} />
      </div>
      <div class="switch-row">
        <div>
          <div class="h3">Daily Study Reminder Preference</div>
          <div class="muted">Remember daily target reminder (${String(prefs.reminderHour).padStart(2, '0')}:${String(prefs.reminderMinute).padStart(2, '0')})</div>
        </div>
        <input id="tog-rem-alerts" class="toggle" type="checkbox" ${prefs.remindersEnabled ? 'checked' : ''} />
      </div>
    </div>

    <div class="card" style="display:flex;flex-direction:column;gap:10px;">
      <div class="h2">Backup & Restore (JSON)</div>
      <div class="muted">Export all syllabus topics, study logs, tasks, MCQs, mock tests, and settings to a portable JSON file (compatible with the native Android app).</div>
      <div class="grid-2">
        <button id="btn-export-json" class="btn btn-primary" type="button">Export File</button>
        <button id="btn-copy-json" class="btn btn-outline" type="button">Copy JSON</button>
      </div>
      <div class="grid-2">
        <label class="btn btn-outline" style="cursor:pointer;">
          Import File
          <input id="input-import-file" type="file" accept=".json,application/json,text/plain" style="display:none;" />
        </label>
        <button id="btn-paste-json" class="btn btn-outline" type="button">Paste JSON</button>
      </div>
    </div>

    <div class="card" style="border-color:rgba(248,113,113,0.45);">
      <div class="h3" style="color:var(--danger);">Danger Zone</div>
      <p class="muted" style="margin:4px 0 10px;">Clear all topics, study logs, tasks, MCQ sessions, mock tests, and reset preferences.</p>
      <button id="btn-reset-all" class="btn btn-danger btn-full" type="button">Reset All Data</button>
    </div>
  `;

  document.getElementById('btn-prof-date').onclick = () => openExamDateModal();
  document.getElementById('btn-save-profile').onclick = async () => {
    const name = document.getElementById('prof-name').value.trim();
    const branch = document.getElementById('prof-branch').value.trim();
    const targetExam = document.getElementById('prof-exam').value.trim();
    const hrs = Number(document.getElementById('prof-goal').value);
    const errEl = document.getElementById('prof-error');
    if (!name) {
      errEl.textContent = 'Please enter your name';
      errEl.classList.remove('hidden');
      return;
    }
    if (!Number.isFinite(hrs) || hrs < 0.25 || hrs > 16) {
      errEl.textContent = 'Daily target should be between 0.25 and 16 hours';
      errEl.classList.remove('hidden');
      return;
    }
    state.preferences = {
      ...state.preferences,
      studentName: name,
      branch: branch || DEFAULT_PREFS.branch,
      targetExam: targetExam || DEFAULT_PREFS.targetExam,
      dailyStudyTargetMinutes: Math.round(hrs * 60)
    };
    await savePreferences(state.preferences);
    toast('Profile saved.');
    render();
  };

  document.querySelectorAll('[data-theme-mode]').forEach((b) => {
    b.onclick = async () => {
      state.preferences = { ...state.preferences, themeMode: b.getAttribute('data-theme-mode') };
      await savePreferences(state.preferences);
      applyTheme(state.preferences.themeMode);
      render();
    };
  });

  document.getElementById('tog-pomo-alerts').onchange = async (e) => {
    let checked = e.target.checked;
    if (checked && 'Notification' in window && Notification.permission === 'default') {
      const perm = await Notification.requestPermission();
      if (perm !== 'granted') toast('Browser notifications blocked; in-app alerts still work.');
    }
    state.preferences = { ...state.preferences, pomodoroAlertsEnabled: checked };
    await savePreferences(state.preferences);
    toast('Notification preferences updated.');
  };

  document.getElementById('tog-rem-alerts').onchange = async (e) => {
    state.preferences = { ...state.preferences, remindersEnabled: e.target.checked };
    await savePreferences(state.preferences);
    toast('Notification preferences updated.');
  };

  document.getElementById('btn-export-json').onclick = () => {
    const json = buildExportJson();
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `ecet_tracker_backup_${todayEpochDay()}.json`;
    a.click();
    URL.revokeObjectURL(url);
    toast('Backup exported to JSON file.');
  };

  document.getElementById('btn-copy-json').onclick = async () => {
    try {
      const json = buildExportJson();
      await navigator.clipboard.writeText(json);
      toast('Backup JSON copied to clipboard.');
    } catch (_) {
      toast('Clipboard access blocked. Please use Export File instead.');
    }
  };

  document.getElementById('input-import-file').onchange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const text = await file.text();
    promptRestoreJson(text);
  };

  document.getElementById('btn-paste-json').onclick = () => {
    showModal(
      `
      <div class="h2">Paste Backup JSON</div>
      <p class="muted">Paste a previously exported ECET Tracker 2027 JSON backup below.</p>
      <div class="field"><textarea id="paste-json-area" class="input" rows="7" placeholder='{"schemaVersion":1,...}'></textarea></div>
      <div id="paste-err" class="error-text hidden"></div>
      <div class="row-between">
        <button class="btn btn-ghost" id="m-cancel" type="button">Cancel</button>
        <button class="btn btn-primary" id="m-next" type="button">Continue</button>
      </div>
    `,
      (card) => {
        card.querySelector('#m-cancel').onclick = closeModal;
        card.querySelector('#m-next').onclick = () => {
          const val = card.querySelector('#paste-json-area').value.trim();
          if (!val) {
            const err = card.querySelector('#paste-err');
            err.textContent = 'Please paste backup JSON';
            err.classList.remove('hidden');
            return;
          }
          promptRestoreJson(val);
        };
      }
    );
  };

  document.getElementById('btn-reset-all').onclick = () => {
    confirmModal(
      'Reset All Data?',
      'This permanently deletes all saved study records on this device. Export a JSON backup first if you want to keep a copy.',
      async () => {
        await resetDatabase();
        await reloadData();
        toast('All study data has been reset.');
        render();
      },
      true
    );
  };
}

// MODALS
function openExamDateModal() {
  const currentIso = state.preferences.examDateEpochDay ? epochDayToIso(state.preferences.examDateEpochDay) : '';
  showModal(
    `
    <div class="h2">Set Official TG ECET 2027 Date</div>
    <p class="muted">Leave blank or tap Clear if the official date is not yet announced.</p>
    <div class="field"><label>Examination Date</label><input id="m-exam-date" class="input" type="date" value="${currentIso}" /></div>
    <div class="row-between">
      <button class="btn btn-ghost" id="m-clear" type="button">Clear</button>
      <div style="display:flex;gap:8px;">
        <button class="btn btn-outline" id="m-cancel" type="button">Cancel</button>
        <button class="btn btn-primary" id="m-save" type="button">Confirm</button>
      </div>
    </div>
  `,
    (card) => {
      card.querySelector('#m-cancel').onclick = closeModal;
      card.querySelector('#m-clear').onclick = async () => {
        state.preferences = { ...state.preferences, examDateEpochDay: null };
        await savePreferences(state.preferences);
        closeModal();
        toast('Exam date cleared.');
        render();
      };
      card.querySelector('#m-save').onclick = async () => {
        const ep = isoToEpochDay(card.querySelector('#m-exam-date').value);
        state.preferences = { ...state.preferences, examDateEpochDay: ep };
        await savePreferences(state.preferences);
        closeModal();
        toast(ep ? `Exam date set to ${formatEpochDay(ep)}.` : 'Exam date cleared.');
        render();
      };
    }
  );
}

function openTopicModal(topic = null) {
  const targetIso = topic?.targetDateEpochDay ? epochDayToIso(topic.targetDateEpochDay) : '';
  showModal(
    `
    <div class="h2">${topic ? 'Edit Topic' : 'Add Syllabus Topic'}</div>
    <div class="field"><label>Topic Name *</label><input id="t-title" class="input" value="${esc(topic?.title || '')}" placeholder="e.g., Laplace Transforms" /></div>
    <div class="grid-2">
      <div class="field"><label>Subject</label><select id="t-sub" class="input">${SUBJECTS.map((s) => `<option value="${s.key}" ${(topic?.subject || state.syllabusFilterSubject || 'ECE') === s.key ? 'selected' : ''}>${s.shortName}</option>`).join('')}</select></div>
      <div class="field"><label>Status</label><select id="t-stat" class="input">${STATUSES.map((s) => `<option value="${s.key}" ${(topic?.status || 'NOT_STARTED') === s.key ? 'selected' : ''}>${s.label}</option>`).join('')}</select></div>
    </div>
    <div class="grid-2">
      <div class="field"><label>Priority</label><select id="t-prio" class="input">${PRIORITIES.map((p) => `<option value="${p.key}" ${(topic?.priority || 'MEDIUM') === p.key ? 'selected' : ''}>${p.label}</option>`).join('')}</select></div>
      <div class="field"><label>Target Date</label><input id="t-date" class="input" type="date" value="${targetIso}" /></div>
    </div>
    <div class="field"><label>Personal Notes</label><textarea id="t-notes" class="input" placeholder="Formulas, key concepts...">${esc(topic?.notes || '')}</textarea></div>
    <div id="t-err" class="error-text hidden"></div>
    <div class="row-between">
      <button class="btn btn-ghost" id="m-cancel" type="button">Cancel</button>
      <button class="btn btn-primary" id="m-save" type="button">Save</button>
    </div>
  `,
    (card) => {
      card.querySelector('#m-cancel').onclick = closeModal;
      card.querySelector('#m-save').onclick = async () => {
        const title = card.querySelector('#t-title').value.trim();
        if (!title) {
          const err = card.querySelector('#t-err');
          err.textContent = 'Please enter a topic name';
          err.classList.remove('hidden');
          return;
        }
        const now = Date.now();
        await putRecord('syllabusTopics', {
          id: topic?.id || undefined,
          subject: card.querySelector('#t-sub').value,
          title,
          status: card.querySelector('#t-stat').value,
          priority: card.querySelector('#t-prio').value,
          notes: card.querySelector('#t-notes').value.trim(),
          targetDateEpochDay: isoToEpochDay(card.querySelector('#t-date').value),
          createdAtMs: topic?.createdAtMs || now,
          updatedAtMs: now
        });
        await reloadData();
        closeModal();
        toast(topic ? 'Topic updated.' : 'Topic added.');
        render();
      };
    }
  );
}

function openSyllabusImportModal() {
  showModal(
    `
    <div class="h2">Import Syllabus Topics</div>
    <p class="muted">Paste official TG ECET 2027 topics (one topic per line) or a JSON array/object.</p>
    <div class="field"><label>Default Subject</label><select id="imp-sub" class="input">${SUBJECTS.map((s) => `<option value="${s.key}" ${(state.syllabusFilterSubject || 'ECE') === s.key ? 'selected' : ''}>${s.shortName}</option>`).join('')}</select></div>
    <div class="field"><label>Topics List or JSON</label><textarea id="imp-text" class="input" rows="6" placeholder="Matrices & Determinants&#10;Differential Equations&#10;Laplace Transforms"></textarea></div>
    <div id="imp-err" class="error-text hidden"></div>
    <div class="row-between">
      <button class="btn btn-ghost" id="m-cancel" type="button">Cancel</button>
      <button class="btn btn-primary" id="m-imp" type="button">Import</button>
    </div>
  `,
    (card) => {
      card.querySelector('#m-cancel').onclick = closeModal;
      card.querySelector('#m-imp').onclick = async () => {
        const raw = card.querySelector('#imp-text').value.trim();
        const fallbackSub = card.querySelector('#imp-sub').value;
        if (!raw) {
          const err = card.querySelector('#imp-err');
          err.textContent = 'Paste at least one topic line or valid JSON';
          err.classList.remove('hidden');
          return;
        }
        const parsed = parseSyllabusInput(raw, fallbackSub);
        if (!parsed.length) {
          const err = card.querySelector('#imp-err');
          err.textContent = 'No valid topics found';
          err.classList.remove('hidden');
          return;
        }
        await bulkInsert('syllabusTopics', parsed);
        await reloadData();
        closeModal();
        toast(`Imported ${parsed.length} topics.`);
        render();
      };
    }
  );
}

function parseSyllabusInput(raw, fallbackSub) {
  const now = Date.now();
  if (raw.startsWith('[') || raw.startsWith('{')) {
    try {
      const parsed = JSON.parse(raw);
      const arr = Array.isArray(parsed) ? parsed : parsed.syllabusTopics || parsed.topics || [];
      const items = arr
        .map((item) => {
          if (typeof item === 'string' && item.trim()) {
            return { subject: fallbackSub, title: item.trim(), status: 'NOT_STARTED', priority: 'MEDIUM', notes: '', targetDateEpochDay: null, createdAtMs: now, updatedAtMs: now };
          }
          if (item && typeof item === 'object' && String(item.title || '').trim()) {
            return {
              subject: getSubject(item.subject || fallbackSub).key,
              title: String(item.title).trim(),
              status: getStatus(item.status).key,
              priority: getPriority(item.priority).key,
              notes: String(item.notes || ''),
              targetDateEpochDay: item.targetDateEpochDay || null,
              createdAtMs: now,
              updatedAtMs: now
            };
          }
          return null;
        })
        .filter(Boolean);
      if (items.length) return items;
    } catch (_) {}
  }
  const seen = new Set();
  return raw
    .split(/\r?\n/)
    .map((l) => l.trim().replace(/^[-*•]+\s*/, '').replace(/^\d+[.)]\s*/, '').trim())
    .filter((l) => l && !seen.has(l) && seen.add(l))
    .map((title) => ({ subject: fallbackSub, title, status: 'NOT_STARTED', priority: 'MEDIUM', notes: '', targetDateEpochDay: null, createdAtMs: now, updatedAtMs: now }));
}

function openPomodoroModal() {
  const p = state.preferences;
  showModal(
    `
    <div class="h2">Customize Pomodoro</div>
    <div class="field"><label>Focus Duration (minutes)</label><input id="p-focus" class="input" type="number" value="${p.pomodoroFocusMinutes}" /></div>
    <div class="field"><label>Short Break (minutes)</label><input id="p-short" class="input" type="number" value="${p.pomodoroShortBreakMinutes}" /></div>
    <div class="field"><label>Long Break (minutes)</label><input id="p-long" class="input" type="number" value="${p.pomodoroLongBreakMinutes}" /></div>
    <div id="p-err" class="error-text hidden"></div>
    <div class="row-between">
      <button class="btn btn-ghost" id="m-cancel" type="button">Cancel</button>
      <button class="btn btn-primary" id="m-save" type="button">Save</button>
    </div>
  `,
    (card) => {
      card.querySelector('#m-cancel').onclick = closeModal;
      card.querySelector('#m-save').onclick = async () => {
        const f = Number(card.querySelector('#p-focus').value);
        const s = Number(card.querySelector('#p-short').value);
        const l = Number(card.querySelector('#p-long').value);
        if (!Number.isInteger(f) || f < 1 || f > 180 || !Number.isInteger(s) || s < 1 || s > 60 || !Number.isInteger(l) || l < 1 || l > 90) {
          const err = card.querySelector('#p-err');
          err.textContent = 'Enter valid durations (Focus 1-180m, Breaks 1-90m)';
          err.classList.remove('hidden');
          return;
        }
        state.preferences = { ...state.preferences, pomodoroFocusMinutes: f, pomodoroShortBreakMinutes: s, pomodoroLongBreakMinutes: l };
        await savePreferences(state.preferences);
        closeModal();
        toast('Pomodoro durations updated.');
        render();
      };
    }
  );
}

function openTaskModal(task = null) {
  showModal(
    `
    <div class="h2">${task ? 'Edit Task' : 'Add Daily Task'}</div>
    <div class="field"><label>Task Description *</label><input id="tk-title" class="input" value="${esc(task?.title || '')}" placeholder="e.g., Solve 30 Op-Amp MCQs" /></div>
    <div class="grid-2">
      <div class="field"><label>Subject Tag</label><select id="tk-sub" class="input"><option value="">General</option>${SUBJECTS.map((s) => `<option value="${s.key}" ${task?.subject === s.key ? 'selected' : ''}>${s.shortName}</option>`).join('')}</select></div>
      <div class="field"><label>Priority</label><select id="tk-prio" class="input">${PRIORITIES.map((p) => `<option value="${p.key}" ${(task?.priority || 'MEDIUM') === p.key ? 'selected' : ''}>${p.label}</option>`).join('')}</select></div>
    </div>
    <div id="tk-err" class="error-text hidden"></div>
    <div class="row-between">
      <button class="btn btn-ghost" id="m-cancel" type="button">Cancel</button>
      <button class="btn btn-primary" id="m-save" type="button">Save</button>
    </div>
  `,
    (card) => {
      card.querySelector('#m-cancel').onclick = closeModal;
      card.querySelector('#m-save').onclick = async () => {
        const title = card.querySelector('#tk-title').value.trim();
        if (!title) {
          const err = card.querySelector('#tk-err');
          err.textContent = 'Please enter a task description';
          err.classList.remove('hidden');
          return;
        }
        await putRecord('dailyTasks', {
          id: task?.id || undefined,
          title,
          subject: card.querySelector('#tk-sub').value || null,
          priority: card.querySelector('#tk-prio').value,
          dateEpochDay: task?.dateEpochDay || state.tasksSelectedDate,
          isCompleted: task?.isCompleted || false,
          createdAtMs: task?.createdAtMs || Date.now()
        });
        await reloadData();
        closeModal();
        toast(task ? 'Task updated.' : 'Task added.');
        render();
      };
    }
  );
}

function openMcqModal() {
  const todayIso = epochDayToIso(todayEpochDay());
  showModal(
    `
    <div class="h2">Record MCQ Practice</div>
    <div class="field"><label>Subject</label><select id="mq-sub" class="input">${SUBJECTS.map((s) => `<option value="${s.key}" ${s.key === 'ECE' ? 'selected' : ''}>${s.displayName}</option>`).join('')}</select></div>
    <div class="field"><label>Topic Name *</label><input id="mq-topic" class="input" list="saved-topics-list" placeholder="e.g., K-Maps" />
      <datalist id="saved-topics-list">${state.topics.map((t) => `<option value="${esc(t.title)}"></option>`).join('')}</datalist>
    </div>
    <div class="grid-2">
      <div class="field"><label>Attempted *</label><input id="mq-att" class="input" type="number" min="1" /></div>
      <div class="field"><label>Correct *</label><input id="mq-cor" class="input" type="number" min="0" /></div>
    </div>
    <div class="grid-2">
      <div class="field"><label>Incorrect</label><input id="mq-inc" class="input" type="number" min="0" /></div>
      <div class="field"><label>Unattempted</label><input id="mq-unatt" class="input" type="number" min="0" value="0" /></div>
    </div>
    <div class="grid-2">
      <div class="field"><label>Time Taken (mins)</label><input id="mq-mins" class="input" type="number" min="0" value="30" /></div>
      <div class="field"><label>Date</label><input id="mq-date" class="input" type="date" value="${todayIso}" /></div>
    </div>
    <div id="mq-preview" class="card hidden" style="padding:10px 12px;background:var(--primary-soft);border-color:var(--primary);">
      <div class="row-between"><span>Calculated Accuracy</span><strong id="mq-acc-val" class="mono"></strong></div>
    </div>
    <div id="mq-err" class="error-text hidden"></div>
    <div class="row-between">
      <button class="btn btn-ghost" id="m-cancel" type="button">Cancel</button>
      <button class="btn btn-primary" id="m-save" type="button">Save</button>
    </div>
  `,
    (card) => {
      const attEl = card.querySelector('#mq-att');
      const corEl = card.querySelector('#mq-cor');
      const incEl = card.querySelector('#mq-inc');
      const prevEl = card.querySelector('#mq-preview');
      const accVal = card.querySelector('#mq-acc-val');
      const updateCalc = () => {
        const a = Number(attEl.value);
        const c = Number(corEl.value);
        if (attEl.value && corEl.value && a > 0 && c >= 0 && c <= a) {
          incEl.value = String(a - c);
          accVal.textContent = `${((c * 100) / a).toFixed(1)}%`;
          prevEl.classList.remove('hidden');
        } else {
          prevEl.classList.add('hidden');
        }
      };
      attEl.oninput = updateCalc;
      corEl.oninput = updateCalc;
      card.querySelector('#m-cancel').onclick = closeModal;
      card.querySelector('#m-save').onclick = async () => {
        const topicName = card.querySelector('#mq-topic').value.trim();
        const att = Number(attEl.value);
        const cor = Number(corEl.value);
        const inc = incEl.value === '' ? att - cor : Number(incEl.value);
        const unatt = Number(card.querySelector('#mq-unatt').value || 0);
        const mins = Number(card.querySelector('#mq-mins').value || 0);
        const dateEp = isoToEpochDay(card.querySelector('#mq-date').value) ?? todayEpochDay();
        const err = card.querySelector('#mq-err');

        if (!topicName) return showErr(err, 'Enter a topic name');
        if (!Number.isInteger(att) || att <= 0) return showErr(err, 'Attempted questions must be > 0');
        if (!Number.isInteger(cor) || cor < 0 || cor > att) return showErr(err, `Correct answers must be between 0 and ${att}`);
        if (!Number.isInteger(inc) || inc < 0 || cor + inc !== att) return showErr(err, `Correct + Incorrect must equal Attempted (${att})`);
        if (unatt < 0 || mins < 0) return showErr(err, 'Values cannot be negative');

        const accuracyPercent = (cor * 100) / att;
        await putRecord('mcqSessions', {
          subject: card.querySelector('#mq-sub').value,
          topicName,
          attempted: att,
          correct: cor,
          incorrect: inc,
          unattempted: unatt,
          timeTakenMinutes: mins,
          accuracyPercent,
          dateEpochDay: dateEp,
          createdAtMs: Date.now()
        });
        await reloadData();
        closeModal();
        toast(`MCQ practice logged (${accuracyPercent.toFixed(1)}% accuracy).`);
        render();
      };
    }
  );
}

function openMockModal(mock = null) {
  const dateIso = epochDayToIso(mock ? mock.testDateEpochDay : todayEpochDay());
  showModal(
    `
    <div class="h2">${mock ? 'Edit Mock Test' : 'Record Mock Test'}</div>
    <div class="grid-2">
      <div class="field"><label>Mock Test Name *</label><input id="mk-title" class="input" value="${esc(mock?.title || '')}" placeholder="Full Mock #1" /></div>
      <div class="field"><label>Test Date</label><input id="mk-date" class="input" type="date" value="${dateIso}" /></div>
    </div>
    <div class="grid-2">
      <div class="field"><label>Math Marks</label><input id="mk-m" class="input" type="number" step="0.5" value="${mock?.mathMarks ?? 0}" /></div>
      <div class="field"><label>Math Max</label><input id="mk-mm" class="input" type="number" step="0.5" value="${mock?.mathMaxMarks ?? 50}" /></div>
      <div class="field"><label>Physics Marks</label><input id="mk-p" class="input" type="number" step="0.5" value="${mock?.physicsMarks ?? 0}" /></div>
      <div class="field"><label>Physics Max</label><input id="mk-pm" class="input" type="number" step="0.5" value="${mock?.physicsMaxMarks ?? 25}" /></div>
      <div class="field"><label>Chemistry Marks</label><input id="mk-c" class="input" type="number" step="0.5" value="${mock?.chemistryMarks ?? 0}" /></div>
      <div class="field"><label>Chemistry Max</label><input id="mk-cm" class="input" type="number" step="0.5" value="${mock?.chemistryMaxMarks ?? 25}" /></div>
      <div class="field"><label>ECE Marks</label><input id="mk-e" class="input" type="number" step="0.5" value="${mock?.eceMarks ?? 0}" /></div>
      <div class="field"><label>ECE Max</label><input id="mk-em" class="input" type="number" step="0.5" value="${mock?.eceMaxMarks ?? 100}" /></div>
    </div>
    <div class="grid-3">
      <div class="field"><label>Attempted</label><input id="mk-att" class="input" type="number" value="${mock?.attempted ?? 0}" /></div>
      <div class="field"><label>Correct</label><input id="mk-cor" class="input" type="number" value="${mock?.correct ?? 0}" /></div>
      <div class="field"><label>Wrong</label><input id="mk-inc" class="input" type="number" value="${mock?.incorrect ?? 0}" /></div>
    </div>
    <div class="field"><label>Analysis Notes</label><textarea id="mk-notes" class="input">${esc(mock?.notes || '')}</textarea></div>
    <div id="mk-err" class="error-text hidden"></div>
    <div class="row-between">
      <button class="btn btn-ghost" id="m-cancel" type="button">Cancel</button>
      <button class="btn btn-primary" id="m-save" type="button">Save</button>
    </div>
  `,
    (card) => {
      card.querySelector('#m-cancel').onclick = closeModal;
      card.querySelector('#m-save').onclick = async () => {
        const title = card.querySelector('#mk-title').value.trim();
        const m = Number(card.querySelector('#mk-m').value || 0);
        const mm = Number(card.querySelector('#mk-mm').value || 0);
        const p = Number(card.querySelector('#mk-p').value || 0);
        const pm = Number(card.querySelector('#mk-pm').value || 0);
        const c = Number(card.querySelector('#mk-c').value || 0);
        const cm = Number(card.querySelector('#mk-cm').value || 0);
        const e = Number(card.querySelector('#mk-e').value || 0);
        const em = Number(card.querySelector('#mk-em').value || 0);
        const att = Number(card.querySelector('#mk-att').value || 0);
        const cor = Number(card.querySelector('#mk-cor').value || 0);
        const inc = Number(card.querySelector('#mk-inc').value || 0);
        const err = card.querySelector('#mk-err');

        if (!title) return showErr(err, 'Enter a mock test title');
        if (mm <= 0 || pm <= 0 || cm <= 0 || em <= 0) return showErr(err, 'Max marks must be greater than 0');
        if (m < 0 || p < 0 || c < 0 || e < 0) return showErr(err, 'Marks cannot be negative');
        if (m > mm || p > pm || c > cm || e > em) return showErr(err, 'Subject marks cannot exceed max marks');
        if (att < 0 || cor < 0 || inc < 0 || cor + inc > att) return showErr(err, 'Correct + Wrong cannot exceed Attempted');

        await putRecord('mockTests', {
          id: mock?.id || undefined,
          title,
          testDateEpochDay: isoToEpochDay(card.querySelector('#mk-date').value) ?? todayEpochDay(),
          mathMarks: m,
          mathMaxMarks: mm,
          physicsMarks: p,
          physicsMaxMarks: pm,
          chemistryMarks: c,
          chemistryMaxMarks: cm,
          eceMarks: e,
          eceMaxMarks: em,
          totalMarks: m + p + c + e,
          maxTotalMarks: mm + pm + cm + em,
          attempted: att,
          correct: cor,
          incorrect: inc,
          notes: card.querySelector('#mk-notes').value.trim(),
          createdAtMs: mock?.createdAtMs || Date.now()
        });
        await reloadData();
        closeModal();
        toast(mock ? 'Mock test updated.' : 'Mock test saved.');
        render();
      };
    }
  );
}

function showErr(el, msg) {
  el.textContent = msg;
  el.classList.remove('hidden');
}

function confirmModal(title, message, onConfirm, destructive = true) {
  showModal(
    `
    <div class="h2">${esc(title)}</div>
    <p class="muted">${esc(message)}</p>
    <div class="row-between" style="margin-top:6px;">
      <button class="btn btn-ghost" id="m-cancel" type="button">Cancel</button>
      <button class="btn ${destructive ? 'btn-danger' : 'btn-primary'}" id="m-ok" type="button">Confirm</button>
    </div>
  `,
    (card) => {
      card.querySelector('#m-cancel').onclick = closeModal;
      card.querySelector('#m-ok').onclick = async () => {
        closeModal();
        await onConfirm();
      };
    }
  );
}

function buildExportJson() {
  return JSON.stringify(
    {
      app: 'ECET Tracker 2027',
      schemaVersion: 1,
      exportedAtMs: Date.now(),
      preferences: state.preferences,
      syllabusTopics: state.topics,
      studySessions: state.studySessions,
      dailyTasks: state.dailyTasks,
      mcqSessions: state.mcqSessions,
      mockTests: state.mockTests
    },
    null,
    2
  );
}

function promptRestoreJson(rawJson) {
  let payload;
  try {
    payload = JSON.parse(rawJson.trim());
    if (!payload || typeof payload !== 'object') throw new Error('Invalid JSON object');
  } catch (err) {
    toast(`Restore failed: ${err.message}`);
    return;
  }
  const summary = `${(payload.syllabusTopics || []).length} topics, ${(payload.studySessions || []).length} sessions, ${(payload.dailyTasks || []).length} tasks, ${(payload.mcqSessions || []).length} MCQ logs, and ${(payload.mockTests || []).length} mock tests`;
  confirmModal(
    'Restore Backup?',
    `Restoring this JSON backup (${summary}) will replace your current saved data.`,
    async () => {
      await replaceAllBackupData(payload);
      await reloadData();
      toast(`Restored ${summary}.`);
      render();
    },
    false
  );
}

// PWA Install & Service Worker Bootstrap
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  state.deferredInstallPrompt = e;
  if (!state.installDismissed) {
    document.getElementById('install-banner')?.classList.remove('hidden');
  }
});

window.addEventListener('appinstalled', () => {
  state.deferredInstallPrompt = null;
  document.getElementById('install-banner')?.classList.add('hidden');
  toast('ECET Tracker 2027 installed!');
});

async function init() {
  if ('serviceWorker' in navigator) {
    navigator.serviceWorker.register('./sw.js').catch(() => {});
  }
  document.getElementById('btn-install-pwa').onclick = async () => {
    if (!state.deferredInstallPrompt) return;
    state.deferredInstallPrompt.prompt();
    await state.deferredInstallPrompt.userChoice;
    state.deferredInstallPrompt = null;
    document.getElementById('install-banner').classList.add('hidden');
  };
  document.getElementById('btn-dismiss-install').onclick = () => {
    state.installDismissed = true;
    document.getElementById('install-banner').classList.add('hidden');
  };

  await reloadData();
  render();

  setInterval(evaluateTimerTick, 250);
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') {
      evaluateTimerTick();
    }
  });
}

init();
