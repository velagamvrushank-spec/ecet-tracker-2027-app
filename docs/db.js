const DB_NAME = 'ecet_tracker_2027_db';
const DB_VERSION = 1;

export const DEFAULT_PREFS = {
  studentName: 'ECET Aspirant',
  branch: 'ECE (Electronics & Communication)',
  targetExam: 'TG ECET 2027',
  examDateEpochDay: null,
  dailyStudyTargetMinutes: 240,
  themeMode: 'DARK',
  remindersEnabled: false,
  pomodoroAlertsEnabled: true,
  reminderHour: 19,
  reminderMinute: 0,
  pomodoroFocusMinutes: 25,
  pomodoroShortBreakMinutes: 5,
  pomodoroLongBreakMinutes: 15
};

export const DEFAULT_TIMER = {
  isRunning: false,
  mode: 'STOPWATCH',
  phase: 'FOCUS',
  startWallMs: 0,
  accumulatedMs: 0,
  subject: 'ECE',
  topicId: null,
  topicTitle: null,
  pomodoroRound: 1
};

function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = (event) => {
      const db = event.target.result;
      if (!db.objectStoreNames.contains('meta')) {
        db.createObjectStore('meta', { keyPath: 'key' });
      }
      if (!db.objectStoreNames.contains('syllabusTopics')) {
        db.createObjectStore('syllabusTopics', { keyPath: 'id', autoIncrement: true });
      }
      if (!db.objectStoreNames.contains('studySessions')) {
        db.createObjectStore('studySessions', { keyPath: 'id', autoIncrement: true });
      }
      if (!db.objectStoreNames.contains('dailyTasks')) {
        db.createObjectStore('dailyTasks', { keyPath: 'id', autoIncrement: true });
      }
      if (!db.objectStoreNames.contains('mcqSessions')) {
        db.createObjectStore('mcqSessions', { keyPath: 'id', autoIncrement: true });
      }
      if (!db.objectStoreNames.contains('mockTests')) {
        db.createObjectStore('mockTests', { keyPath: 'id', autoIncrement: true });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function getAll(storeName) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readonly');
    const req = tx.objectStore(storeName).getAll();
    req.onsuccess = () => resolve(req.result || []);
    req.onerror = () => reject(req.error);
  });
}

async function getMeta(key, fallback) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction('meta', 'readonly');
    const req = tx.objectStore('meta').get(key);
    req.onsuccess = () => resolve(req.result ? { ...fallback, ...req.result.value } : { ...fallback });
    req.onerror = () => reject(req.error);
  });
}

async function setMeta(key, value) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction('meta', 'readwrite');
    tx.objectStore('meta').put({ key, value });
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function loadAllData() {
  const [preferences, timerSnapshot, topics, studySessions, dailyTasks, mcqSessions, mockTests] =
    await Promise.all([
      getMeta('preferences', DEFAULT_PREFS),
      getMeta('timer', DEFAULT_TIMER),
      getAll('syllabusTopics'),
      getAll('studySessions'),
      getAll('dailyTasks'),
      getAll('mcqSessions'),
      getAll('mockTests')
    ]);

  topics.sort((a, b) => (b.createdAtMs || 0) - (a.createdAtMs || 0) || b.id - a.id);
  studySessions.sort((a, b) => (b.endedAtMs || 0) - (a.endedAtMs || 0) || b.id - a.id);
  dailyTasks.sort((a, b) => Number(a.isCompleted) - Number(b.isCompleted) || (b.createdAtMs || 0) - (a.createdAtMs || 0));
  mcqSessions.sort((a, b) => (b.dateEpochDay || 0) - (a.dateEpochDay || 0) || (b.createdAtMs || 0) - (a.createdAtMs || 0));
  mockTests.sort((a, b) => (a.testDateEpochDay || 0) - (b.testDateEpochDay || 0) || (a.createdAtMs || 0) - (b.createdAtMs || 0));

  return { preferences, timerSnapshot, topics, studySessions, dailyTasks, mcqSessions, mockTests };
}

export async function savePreferences(prefs) {
  await setMeta('preferences', prefs);
}

export async function saveTimerSnapshot(snap) {
  await setMeta('timer', snap);
}

export async function putRecord(storeName, record) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite');
    const store = tx.objectStore(storeName);
    const clean = { ...record };
    if (!clean.id) delete clean.id;
    const req = store.put(clean);
    req.onsuccess = () => resolve(req.result);
    tx.onerror = () => reject(tx.error);
  });
}

export async function deleteRecord(storeName, id) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite');
    tx.objectStore(storeName).delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function bulkInsert(storeName, records) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(storeName, 'readwrite');
    const store = tx.objectStore(storeName);
    records.forEach((rec) => {
      const clean = { ...rec };
      if (!clean.id) delete clean.id;
      store.put(clean);
    });
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function replaceAllBackupData(payload) {
  const db = await openDb();
  const stores = ['meta', 'syllabusTopics', 'studySessions', 'dailyTasks', 'mcqSessions', 'mockTests'];
  const sanitize = (item) => {
    const clean = { ...item };
    if (!clean.id || clean.id <= 0) delete clean.id;
    return clean;
  };
  return new Promise((resolve, reject) => {
    const tx = db.transaction(stores, 'readwrite');
    stores.forEach((s) => tx.objectStore(s).clear());
    tx.objectStore('meta').put({ key: 'preferences', value: { ...DEFAULT_PREFS, ...(payload.preferences || {}) } });
    tx.objectStore('meta').put({ key: 'timer', value: { ...DEFAULT_TIMER } });
    (payload.syllabusTopics || []).forEach((item) => tx.objectStore('syllabusTopics').put(sanitize(item)));
    (payload.studySessions || []).forEach((item) => tx.objectStore('studySessions').put(sanitize(item)));
    (payload.dailyTasks || []).forEach((item) => tx.objectStore('dailyTasks').put(sanitize(item)));
    (payload.mcqSessions || []).forEach((item) => tx.objectStore('mcqSessions').put(sanitize(item)));
    (payload.mockTests || []).forEach((item) => tx.objectStore('mockTests').put(sanitize(item)));
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function resetDatabase() {
  await replaceAllBackupData({
    preferences: { ...DEFAULT_PREFS },
    syllabusTopics: [],
    studySessions: [],
    dailyTasks: [],
    mcqSessions: [],
    mockTests: []
  });
}
