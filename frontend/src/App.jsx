import React, { useState, useEffect, useRef } from 'react';
import Keycloak from 'keycloak-js';
import { 
  Dumbbell, 
  Clock, 
  Flame, 
  Plus, 
  Brain, 
  AlertTriangle, 
  FileText, 
  ChevronLeft, 
  ChevronRight, 
  X, 
  LogOut, 
  Key, 
  Settings, 
  Trash2, 
  Shield, 
  User, 
  FilePlus, 
  Save, 
  Send, 
  Sparkles, 
  Loader2 
} from 'lucide-react';

const API_BASE = ''; // Vite proxies /api/** to http://localhost:8080

let keycloakInstance = null;
let keycloakPromise = null;

export default function App() {
  // Keycloak & Auth states
  const [keycloak, setKeycloak] = useState(null);
  const [authenticated, setAuthenticated] = useState(false);
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [userRoles, setUserRoles] = useState([]);
  const [loading, setLoading] = useState(true);

  // Layout & UI states
  const [activeTab, setActiveTab] = useState('dashboard');
  const [sessionApiKey, setSessionApiKey] = useState(
    localStorage.getItem('gemini_api_key') || ''
  );
  
  // Dashboard & Activities states
  const [activities, setActivities] = useState([]);
  const [stats, setStats] = useState({ totalWorkouts: 0, totalDuration: 0, totalCalories: 0, workoutsByType: {} });
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  
  // Modals & Details states
  const [showAddModal, setShowAddModal] = useState(false);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [showKeyModal, setShowKeyModal] = useState(false);
  const [activeActivity, setActiveActivity] = useState(null);
  const [activeRecommendation, setActiveRecommendation] = useState(null);
  const [recLoading, setRecLoading] = useState(false);
  const [recError, setRecError] = useState(null);

  // Forms states
  const [activityType, setActivityType] = useState('RUNNING');
  const [startTime, setStartTime] = useState('');
  const [duration, setDuration] = useState('');
  const [calories, setCalories] = useState('');
  const [customMetrics, setCustomMetrics] = useState([]);
  const [userCommentary, setUserCommentary] = useState('');

  // Personal Notepad states
  const [notesDates, setNotesDates] = useState([]);
  const [activeNoteDate, setActiveNoteDate] = useState('');
  const [noteText, setNoteText] = useState('');
  const [selectedNoteContent, setSelectedNoteContent] = useState('');

  // Recommendation Notes state
  const [recNoteDate, setRecNoteDate] = useState('');
  const [recNoteText, setRecNoteText] = useState('');

  // Chatbots states
  const [userChatMessages, setUserChatMessages] = useState([
    { sender: 'system', text: 'Hello! I am your AuraAI Coach. Ask me anything about fitness, meal plans, or targets, and I will reference your physical activities history.' }
  ]);
  const [userChatInput, setUserChatInput] = useState('');
  const [userChatLoading, setUserChatLoading] = useState(false);

  const [recChatMessages, setRecChatMessages] = useState([]);
  const [recChatInput, setRecChatInput] = useState('');
  const [recChatLoading, setRecChatLoading] = useState(false);

  // Admin states
  const [usersList, setUsersList] = useState([]);

  // Auto-scroll chat boxes
  const userChatEndRef = useRef(null);
  const recChatEndRef = useRef(null);

  useEffect(() => {
    if (userChatEndRef.current) userChatEndRef.current.scrollIntoView({ behavior: 'smooth' });
  }, [userChatMessages]);

  useEffect(() => {
    if (recChatEndRef.current) recChatEndRef.current.scrollIntoView({ behavior: 'smooth' });
  }, [recChatMessages]);

  // Shared fetch helper — attaches all headers the services expect
  // (normally injected by Gateway, but here we send them directly since proxy bypasses Gateway)
  const authFetch = (url, options = {}) => {
    const currentUser = user;
    const currentToken = token;
    return fetch(url, {
      ...options,
      headers: {
        ...(options.headers || {}),
        'Authorization': `Bearer ${currentToken}`,
        'X-User-Id': currentUser?.id || '',
        'X-User-Email': currentUser?.email || 'no-email@provided.com'
      }
    });
  };

  // API Call: Sync User
  const syncUser = async (authToken, profile) => {
    try {
      const url = `${API_BASE}/api/user/sync?firstName=${encodeURIComponent(profile.firstName || '')}&lastName=${encodeURIComponent(profile.lastName || '')}`;
      await fetch(url, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'X-User-Id': profile.id || '',
          'X-User-Email': profile.email || 'no-email@provided.com'
        }
      });
    } catch (e) {
      console.error("User profile sync failed", e);
    }
  };

  // Helper: Setup authenticated user state from Keycloak
  const handleAuthenticatedUser = (kc) => {
    setAuthenticated(true);
    setToken(kc.token);
    if (kc.realmAccess && kc.realmAccess.roles) {
      setUserRoles(kc.realmAccess.roles);
    }

    kc.loadUserProfile().then(profile => {
      const userProfile = {
        id: profile.id || kc.subject,
        firstName: profile.firstName || kc.tokenParsed?.given_name || kc.tokenParsed?.preferred_username || 'User',
        lastName: profile.lastName || kc.tokenParsed?.family_name || '',
        email: profile.email || kc.tokenParsed?.email || ''
      };
      setUser(userProfile);
      syncUser(kc.token, userProfile);
      setLoading(false);
    }).catch(err => {
      console.error("Failed to load user profile, using fallback claims", err);
      const fallbackProfile = {
        id: kc.subject,
        firstName: kc.tokenParsed?.given_name || kc.tokenParsed?.preferred_username || 'User',
        lastName: kc.tokenParsed?.family_name || '',
        email: kc.tokenParsed?.email || ''
      };
      setUser(fallbackProfile);
      syncUser(kc.token, fallbackProfile);
      setLoading(false);
    });
  };

  // 1. Initialize Keycloak
  useEffect(() => {
    if (keycloakPromise) {
      keycloakPromise.then(() => {
        if (keycloakInstance) {
          setKeycloak(keycloakInstance);
          if (keycloakInstance.authenticated) {
            handleAuthenticatedUser(keycloakInstance);
          } else {
            setLoading(false);
          }
        }
      });
      return;
    }

    const kc = new Keycloak({
      url: 'http://localhost:8180',
      realm: 'fitness-realm',
      clientId: 'fitness-app'
    });
    keycloakInstance = kc;

    const hasError = window.location.hash.includes('error=') || window.location.search.includes('error=');
    const checkedSso = sessionStorage.getItem('checked_sso');

    const initOptions = {
      checkLoginIframe: false,
      pkceMethod: 'S256'
    };

    if (!checkedSso && !hasError) {
      initOptions.onLoad = 'check-sso';
      sessionStorage.setItem('checked_sso', 'true');
    }

    keycloakPromise = kc.init(initOptions);
    keycloakPromise.then(auth => {
      setKeycloak(kc);
      if (auth) {
        handleAuthenticatedUser(kc);
      } else {
        setLoading(false);
      }
    }).catch(err => {
      console.error("Keycloak initialization failure", err);
      setLoading(false);
    });
  }, []);

  // 2. Fetch Dashboard Data
  useEffect(() => {
    if (authenticated && user) {
      fetchActivities();
      fetchStats();
      fetchNotesDates();
    }
  }, [authenticated, user, page]);

  // Fetch Users List (Admin Only)
  useEffect(() => {
    if (authenticated && activeTab === 'admin' && userRoles.includes('ADMIN')) {
      fetchAdminUsers();
    }
  }, [authenticated, activeTab, userRoles]);

  // API Call: Fetch Activities
  const fetchActivities = async () => {
    if (!user?.id) return;
    try {
      const res = await authFetch(`${API_BASE}/api/activities/user/${user.id}?page=${page}&size=5`);
      if (res.ok) {
        const data = await res.json();
        setActivities(data.content || []);
        setTotalPages(data.totalPages || 1);
        setTotalElements(data.totalElements || 0);
      }
    } catch (e) {
      console.error("Failed to fetch activities", e);
    }
  };

  // API Call: Fetch Stats
  const fetchStats = async () => {
    if (!user?.id) return;
    try {
      const res = await authFetch(`${API_BASE}/api/activities/stats/${user.id}`);
      if (res.ok) {
        const data = await res.json();
        setStats(data);
      }
    } catch (e) {
      console.error("Failed to fetch stats", e);
    }
  };

  // API Call: Log New Activity
  const handleAddActivitySubmit = async (e) => {
    e.preventDefault();
    if (!window.confirm("Are you sure you want to add this activity? (Activity logs are permanent and cannot be modified later).")) {
      return;
    }

    const metricsMap = {};
    customMetrics.forEach(m => {
      if (m.key.trim() && m.value) {
        metricsMap[m.key.trim()] = parseFloat(m.value);
      }
    });

    if (userCommentary.trim()) {
      metricsMap['app_activity_user_note'] = userCommentary.trim();
    }

    const body = {
      userId: user.id,
      activityType,
      startTime: new Date(startTime).toISOString().slice(0, 19),
      duration: parseInt(duration, 10),
      caloriesBurned: parseInt(calories, 10),
      additionalMetrics: metricsMap
    };

    try {
      const res = await authFetch(`${API_BASE}/api/activities`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });

      if (res.ok) {
        setShowAddModal(false);
        setPage(0);
        fetchActivities();
        fetchStats();
        // Reset form
        setDuration('');
        setCalories('');
        setStartTime('');
        setCustomMetrics([]);
        setUserCommentary('');
      } else {
        alert("Failed to save activity details.");
      }
    } catch (err) {
      console.error("Error creating activity", err);
    }
  };

  // API Call: View AI Recommendation (with on-the-fly generation spinner)
  const openActivityDetails = async (activity) => {
    setActiveActivity(activity);
    setActiveRecommendation(null);
    setRecLoading(true);
    setRecError(null);
    setShowDetailModal(true);
    
    // Clear chat history for specific recommendation
    setRecChatMessages([
      { sender: 'system', text: `Hi! Ask me any questions regarding your ${activity.activityType} workout recommendation and analysis.` }
    ]);

    // Format local date for notepad
    const activityDate = new Date(activity.startTime);
    const day = String(activityDate.getDate()).padStart(2, '0');
    const month = String(activityDate.getMonth() + 1).padStart(2, '0');
    const year = activityDate.getFullYear();
    const formattedDate = `${day}/${month}/${year}`;
    setRecNoteDate(formattedDate);
    
    // Fetch existing recommendation specific note
    fetchRecNote(activity.id, formattedDate);

    try {
      const res = await authFetch(`${API_BASE}/api/recommendations/activity/${activity.id}`, {
        headers: { 'X-Gemini-API-Key': sessionApiKey }
      });

      if (res.ok) {
        const data = await res.json();
        setActiveRecommendation(data);
      } else {
        setRecError("AI Recommendation service is currently offline or taking longer than usual. Please check back in a few seconds.");
      }
    } catch (err) {
      setRecError("Error requesting AI recommendations. Please retry in a few moments.");
    } finally {
      setRecLoading(false);
    }
  };

  // API Call: User Page Chatbot
  const handleUserChatSubmit = async (e) => {
    e.preventDefault();
    if (!userChatInput.trim()) return;

    const userMessage = userChatInput.trim();
    setUserChatInput('');
    setUserChatMessages(prev => [...prev, { sender: 'user', text: userMessage }]);
    setUserChatLoading(true);

    try {
      const res = await authFetch(`${API_BASE}/api/recommendations/chat/user`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Gemini-API-Key': sessionApiKey
        },
        body: JSON.stringify({ message: userMessage })
      });

      if (res.ok) {
        const data = await res.json();
        setUserChatMessages(prev => [...prev, { sender: 'ai', text: data.response }]);
      } else {
        setUserChatMessages(prev => [...prev, { sender: 'ai', text: 'cant help with that try aqsking differnt' }]);
      }
    } catch (err) {
      setUserChatMessages(prev => [...prev, { sender: 'ai', text: 'Connection issue. Please retry.' }]);
    } finally {
      setUserChatLoading(false);
    }
  };

  // API Call: Recommendation Page Chatbot
  const handleRecChatSubmit = async (e) => {
    e.preventDefault();
    if (!recChatInput.trim() || !activeRecommendation) return;

    const userMessage = recChatInput.trim();
    setRecChatInput('');
    setRecChatMessages(prev => [...prev, { sender: 'user', text: userMessage }]);
    setRecChatLoading(true);

    try {
      const res = await authFetch(`${API_BASE}/api/recommendations/chat/recommendation`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Gemini-API-Key': sessionApiKey
        },
        body: JSON.stringify({
          recommendationId: activeRecommendation.id,
          message: userMessage
        })
      });

      if (res.ok) {
        const data = await res.json();
        setRecChatMessages(prev => [...prev, { sender: 'ai', text: data.response }]);
      } else {
        setRecChatMessages(prev => [...prev, { sender: 'ai', text: 'cant help with that try aqsking differnt' }]);
      }
    } catch (err) {
      setRecChatMessages(prev => [...prev, { sender: 'ai', text: 'Connection issue. Please retry.' }]);
    } finally {
      setRecChatLoading(false);
    }
  };

  // API Call: Fetch Notepad Dates
  const fetchNotesDates = async () => {
    if (!user?.id) return;
    try {
      const res = await authFetch(`${API_BASE}/api/user/notes/dates`);
      if (res.ok) {
        const data = await res.json();
        setNotesDates(data);
      }
    } catch (e) {
      console.error(e);
    }
  };

  // API Call: Fetch Note content for activeNoteDate
  const handleSelectNoteDate = async (date) => {
    setActiveNoteDate(date);
    setNoteText('');
    try {
      const res = await authFetch(`${API_BASE}/api/user/notes?targetId=general`);
      if (res.ok) {
        const data = await res.json();
        const found = data.find(n => n.noteDate === date);
        if (found) {
          setNoteText(found.content);
        }
      }
    } catch (e) {
      console.error(e);
    }
  };

  // API Call: Save Personal Note
  const handleSavePersonalNote = async () => {
    if (!activeNoteDate) return;
    try {
      const res = await authFetch(`${API_BASE}/api/user/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          targetId: 'general',
          noteDate: activeNoteDate,
          content: noteText
        })
      });
      if (res.ok) {
        alert("Personal Note successfully saved.");
        fetchNotesDates();
      }
    } catch (e) {
      console.error(e);
    }
  };

  // API Call: Fetch Note for Recommendation
  const fetchRecNote = async (activityId, date) => {
    setRecNoteText('');
    try {
      const res = await authFetch(`${API_BASE}/api/user/notes?targetId=${activityId}`);
      if (res.ok) {
        const data = await res.json();
        const found = data.find(n => n.noteDate === date);
        if (found) {
          setRecNoteText(found.content);
        }
      }
    } catch (e) {
      console.error(e);
    }
  };

  // API Call: Save Recommendation Note
  const handleSaveRecNote = async () => {
    if (!activeActivity) return;
    try {
      const res = await authFetch(`${API_BASE}/api/user/notes`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          targetId: activeActivity.id,
          noteDate: recNoteDate,
          content: recNoteText
        })
      });
      if (res.ok) {
        alert("Recommendation Note successfully saved.");
      }
    } catch (e) {
      console.error(e);
    }
  };

  // API Call: Fetch Admin Users list
  const fetchAdminUsers = async () => {
    try {
      const res = await authFetch(`${API_BASE}/api/user/admin/users`);
      if (res.ok) {
        const data = await res.json();
        setUsersList(data);
      }
    } catch (e) {
      console.error(e);
    }
  };

  // API Call: Update User Role (Admin)
  const handleUpdateRole = async (userId, currentRole) => {
    const newRole = currentRole === 'USER' ? 'ADMIN' : 'USER';
    if (!window.confirm(`Are you sure you want to change user role to ${newRole}?`)) return;
    
    try {
      const res = await authFetch(`${API_BASE}/api/user/admin/users/${userId}/role?role=${newRole}`, {
        method: 'PUT'
      });
      if (res.ok) {
        fetchAdminUsers();
      }
    } catch (e) {
      console.error(e);
    }
  };

  // API Call: Delete User (Admin)
  const handleDeleteUser = async (userId) => {
    if (!window.confirm("Are you sure you want to permanently delete this user profile?")) return;
    
    try {
      const res = await authFetch(`${API_BASE}/api/user/admin/users/${userId}`, {
        method: 'DELETE'
      });
      if (res.ok) {
        fetchAdminUsers();
      }
    } catch (e) {
      console.error(e);
    }
  };

  // Save Dynamic API key
  const saveApiKey = () => {
    localStorage.setItem('gemini_api_key', sessionApiKey);
    setShowKeyModal(false);
  };

  // Clear Dynamic API Key
  const clearApiKey = () => {
    localStorage.removeItem('gemini_api_key');
    setSessionApiKey('');
    setShowKeyModal(false);
  };

  // Add Dynamic Metric Key-Value pair row (forcing integers)
  const addMetricRow = () => {
    setCustomMetrics(prev => [...prev, { key: '', value: '' }]);
  };

  const updateMetricKey = (index, key) => {
    const updated = [...customMetrics];
    updated[index].key = key;
    setCustomMetrics(updated);
  };

  const updateMetricValue = (index, val) => {
    // Force numeric/decimal check in frontend
    const cleanVal = val.replace(/[^0-9.]/g, '');
    const updated = [...customMetrics];
    updated[index].value = cleanVal;
    setCustomMetrics(updated);
  };

  const removeMetricRow = (index) => {
    setCustomMetrics(prev => prev.filter((_, i) => i !== index));
  };

  // Loading Screen OIDC state
  if (loading) {
    return (
      <div className="flex h-screen flex-col items-center justify-center bg-[#070b13] text-gray-300">
        <Loader2 className="h-10 w-10 animate-spin text-orange-500" />
        <p className="mt-4 font-medium tracking-wide">Connecting to AuraFit Network...</p>
      </div>
    );
  }

  // Render Login page if not authenticated
  if (!authenticated) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center bg-[#070b13] text-white p-4 font-sans selection:bg-orange-500 selection:text-white">
        <div className="max-w-2xl text-center flex flex-col items-center">
          <div className="flex items-center space-x-2 text-orange-500 text-3xl font-bold tracking-wider mb-6">
            <span className="bg-gradient-to-r from-orange-500 to-red-600 p-2 rounded-lg text-white">
              <Sparkles className="h-8 w-8" />
            </span>
            <span className="font-outfit text-white">AURAFIT</span>
          </div>
          
          <h1 className="font-outfit text-5xl font-extrabold tracking-tight mb-4 leading-tight bg-gradient-to-r from-white via-gray-200 to-gray-500 bg-clip-text text-transparent">
            Awaken Your Potential.
          </h1>
          
          <p className="text-gray-400 text-lg leading-relaxed max-w-xl mb-8 font-light">
            Every drop of sweat is a transaction with your future self. Log physical training logs, analyze biometrics via advanced AI coaching, and guard-rail your progress with robust secure systems.
          </p>
          
          <div className="flex flex-col items-center space-y-4 w-full max-w-sm">
            <button 
              onClick={() => keycloak.login()} 
              className="w-full py-4 rounded-xl font-bold text-white bg-gradient-to-r from-orange-500 to-red-600 hover:from-orange-600 hover:to-red-700 shadow-lg shadow-orange-500/20 hover:shadow-orange-600/30 transition duration-200 flex items-center justify-center space-x-2 cursor-pointer"
            >
              <span>Enter the Aura (OIDC Login)</span>
            </button>
          </div>

          <div className="mt-8 border border-white/5 bg-white/[0.02] backdrop-blur-md rounded-xl p-4 w-full max-w-md">
            <label className="block text-gray-500 text-xs font-semibold uppercase tracking-wider mb-2 text-left">
              Add your own Gemini API Key
            </label>
            <div className="flex items-center space-x-2">
              <Key className="h-4 w-4 text-gray-500 shrink-0" />
              <input 
                type="password"
                placeholder="Enter your Gemini API key"
                value={sessionApiKey}
                onChange={(e) => {
                  setSessionApiKey(e.target.value);
                  localStorage.setItem('gemini_api_key', e.target.value);
                }}
                className="w-full bg-white/[0.04] border border-white/5 rounded-lg px-3 py-2 text-sm text-gray-300 focus:outline-none focus:border-orange-500/50"
              />
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Dashboard Tab Panel Render
  return (
    <div className="min-h-screen bg-[#070b13] text-gray-200 flex font-sans">
      
      {/* Sidebar Menu */}
      <aside className="w-64 bg-[#0c0f1b] border-r border-white/5 flex flex-col justify-between p-6 shrink-0">
        <div className="flex flex-col space-y-8">
          <div className="flex items-center space-x-2 text-orange-500 text-xl font-bold tracking-wider">
            <Sparkles className="h-6 w-6" />
            <span className="font-outfit text-white">AURAFIT</span>
          </div>
          
          <nav className="flex flex-col space-y-2">
            <button 
              onClick={() => setActiveTab('dashboard')} 
              className={`flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-semibold transition ${activeTab === 'dashboard' ? 'bg-orange-500/10 text-orange-400 border-l-4 border-orange-500' : 'text-gray-400 hover:text-gray-200 hover:bg-white/[0.02]'}`}
            >
              <Dumbbell className="h-4 w-4" />
              <span>Dashboard</span>
            </button>
            
            <button 
              onClick={() => {
                setActiveTab('notes');
                fetchNotesDates();
              }} 
              className={`flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-semibold transition ${activeTab === 'notes' ? 'bg-orange-500/10 text-orange-400 border-l-4 border-orange-500' : 'text-gray-400 hover:text-gray-200 hover:bg-white/[0.02]'}`}
            >
              <FileText className="h-4 w-4" />
              <span>Personal Notes</span>
            </button>

            {userRoles.includes('ADMIN') && (
              <button 
                onClick={() => setActiveTab('admin')} 
                className={`flex items-center space-x-3 px-4 py-3 rounded-xl text-sm font-semibold transition ${activeTab === 'admin' ? 'bg-orange-500/10 text-orange-400 border-l-4 border-orange-500' : 'text-gray-400 hover:text-gray-200 hover:bg-white/[0.02]'}`}
              >
                <Shield className="h-4 w-4" />
                <span>Admin Panel</span>
              </button>
            )}
          </nav>
        </div>

        <div className="flex flex-col space-y-4 pt-6 border-t border-white/5">
          <div className="flex items-center space-x-3">
            <div className="h-10 w-10 rounded-xl bg-orange-500/10 flex items-center justify-center font-bold text-orange-400">
              {user?.firstName ? user.firstName.charAt(0) : 'U'}
            </div>
            <div className="flex flex-col min-w-0">
              <span className="text-sm font-bold text-gray-200 truncate">{user?.firstName} {user?.lastName}</span>
              <span className="text-xs text-gray-500 truncate">{user?.email}</span>
            </div>
          </div>
          <button 
            onClick={() => {
              sessionStorage.removeItem('checked_sso');
              keycloakInstance = null;
              keycloakPromise = null;
              keycloak.logout();
            }}
            className="w-full py-2.5 rounded-xl text-sm font-bold text-gray-400 hover:text-white hover:bg-red-500/10 border border-white/5 hover:border-red-500/20 transition flex items-center justify-center space-x-2 cursor-pointer"
          >
            <LogOut className="h-4 w-4" />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {/* Main Workspace */}
      <main className="flex-1 flex flex-col min-w-0 overflow-y-auto">
        <header className="px-8 py-5 border-b border-white/5 flex items-center justify-between bg-[#080b13]/80 backdrop-blur-md sticky top-0 z-30">
          <h2 className="text-xl font-bold tracking-tight text-white font-outfit">
            {activeTab === 'dashboard' && 'User Dashboard'}
            {activeTab === 'notes' && 'Personal Notepad'}
            {activeTab === 'admin' && 'System Admin Controls'}
          </h2>
          
          <div className="flex items-center space-x-4">
            <div className="text-xs font-semibold px-3 py-1.5 rounded-lg border border-white/5 bg-white/[0.02] text-gray-400 flex items-center space-x-1">
              <Key className="h-3 w-3 text-orange-500" />
              <span>API Key: {sessionApiKey ? 'Active' : 'Default'}</span>
            </div>
            <button 
              onClick={() => setShowKeyModal(true)}
              className="p-2 rounded-lg border border-white/5 hover:bg-white/[0.03] text-gray-400 hover:text-white transition cursor-pointer"
              title="Config Gemini Key"
            >
              <Settings className="h-4 w-4" />
            </button>
          </div>
        </header>

        <div className="p-8">
          
          {/* TAB 1: DASHBOARD PANEL */}
          {activeTab === 'dashboard' && (
            <div className="grid grid-cols-1 xl:grid-cols-3 gap-8">
              
              {/* Left & Middle Column (Stats & Training Logs) */}
              <div className="xl:col-span-2 flex flex-col space-y-8">
                
                {/* Stats Widgets */}
                <section className="glass-card rounded-2xl p-6 glow-orange">
                  <h3 className="text-lg font-bold text-white mb-4">Weekly Metrics</h3>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="flex items-center space-x-4 p-4 rounded-xl bg-blue-500/5 border border-blue-500/10">
                      <div className="p-3 bg-blue-500/10 rounded-xl text-blue-400">
                        <Dumbbell className="h-6 w-6" />
                      </div>
                      <div className="flex flex-col">
                        <span className="text-2xl font-bold text-white">{stats.totalWorkouts || 0}</span>
                        <span className="text-xs text-gray-500">Log Entries</span>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-4 p-4 rounded-xl bg-orange-500/5 border border-orange-500/10">
                      <div className="p-3 bg-orange-500/10 rounded-xl text-orange-400">
                        <Clock className="h-6 w-6" />
                      </div>
                      <div className="flex flex-col">
                        <span className="text-2xl font-bold text-white">{stats.totalDuration || 0} min</span>
                        <span className="text-xs text-gray-500">Duration Session</span>
                      </div>
                    </div>

                    <div className="flex items-center space-x-4 p-4 rounded-xl bg-red-500/5 border border-red-500/10">
                      <div className="p-3 bg-red-500/10 rounded-xl text-red-400">
                        <Flame className="h-6 w-6" />
                      </div>
                      <div className="flex flex-col">
                        <span className="text-2xl font-bold text-white">{stats.totalCalories || 0} kcal</span>
                        <span className="text-xs text-gray-500">Caloric Burned</span>
                      </div>
                    </div>
                  </div>
                </section>

                {/* Training Logs Section */}
                <section className="glass-card rounded-2xl p-6 flex flex-col">
                  <div className="flex items-center justify-between mb-6">
                    <h3 className="text-lg font-bold text-white">Physical Training Logs</h3>
                    <button 
                      onClick={() => setShowAddModal(true)} 
                      className="px-4 py-2 bg-gradient-to-r from-orange-500 to-red-600 hover:from-orange-600 hover:to-red-700 text-white font-bold rounded-xl text-sm transition flex items-center space-x-1 cursor-pointer"
                    >
                      <Plus className="h-4 w-4" />
                      <span>Track Activity</span>
                    </button>
                  </div>

                  <div className="overflow-x-auto min-h-[300px]">
                    {activities.length === 0 ? (
                      <div className="flex flex-col items-center justify-center h-64 text-gray-500 border border-dashed border-white/5 rounded-xl">
                        <Dumbbell className="h-10 w-10 text-gray-600 mb-2" />
                        <p>No activity logged yet. Take the initiative!</p>
                      </div>
                    ) : (
                      <table className="w-full text-left text-sm border-collapse">
                        <thead>
                          <tr className="border-b border-white/5 text-gray-400 font-medium">
                            <th className="py-3 px-4">Activity</th>
                            <th className="py-3 px-4">Date & Time</th>
                            <th className="py-3 px-4">Duration</th>
                            <th className="py-3 px-4">Calories</th>
                            <th className="py-3 px-4">Note Preview</th>
                            <th className="py-3 px-4 text-right">Insights</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-white/5">
                          {activities.map(a => (
                            <tr key={a.id} className="hover:bg-white/[0.01] transition-colors">
                              <td className="py-4 px-4 font-semibold text-white">{a.activityType}</td>
                              <td className="py-4 px-4 text-gray-400">{new Date(a.startTime).toLocaleString()}</td>
                              <td className="py-4 px-4 text-gray-300">{a.duration} mins</td>
                              <td className="py-4 px-4 text-gray-300">{a.caloriesBurned} kcal</td>
                              <td className="py-4 px-4 text-gray-400 max-w-xs truncate">
                                {a.additionalMetrics?.app_activity_user_note || <span className="text-gray-700 italic">None</span>}
                              </td>
                              <td className="py-4 px-4 text-right">
                                <button 
                                  onClick={() => openActivityDetails(a)}
                                  className="px-3 py-1.5 rounded-lg text-xs font-bold text-orange-400 hover:text-white bg-orange-500/10 hover:bg-orange-500 border border-orange-500/20 transition cursor-pointer"
                                >
                                  Open
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    )}
                  </div>

                  {/* Pagination Controls */}
                  {activities.length > 0 && (
                    <div className="flex items-center justify-between mt-6 pt-4 border-t border-white/5">
                      <button 
                        onClick={() => setPage(p => Math.max(0, p - 1))}
                        disabled={page === 0}
                        className="flex items-center space-x-1 px-3.5 py-2 rounded-xl border border-white/5 hover:bg-white/[0.02] text-sm text-gray-300 disabled:opacity-30 disabled:hover:bg-transparent cursor-pointer"
                      >
                        <ChevronLeft className="h-4 w-4" />
                        <span>Previous</span>
                      </button>
                      <span className="text-sm text-gray-400 font-medium">
                        Page {page + 1} of {totalPages}
                      </span>
                      <button 
                        onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                        disabled={page >= totalPages - 1}
                        className="flex items-center space-x-1 px-3.5 py-2 rounded-xl border border-white/5 hover:bg-white/[0.02] text-sm text-gray-300 disabled:opacity-30 disabled:hover:bg-transparent cursor-pointer"
                      >
                        <span>Next</span>
                        <ChevronRight className="h-4 w-4" />
                      </button>
                    </div>
                  )}
                </section>
              </div>

              {/* Right Side Column (AuraAI Guard-Railed Assistant) */}
              <div className="xl:col-span-1 flex flex-col h-[650px] sticky top-[90px]">
                <section className="glass-card rounded-2xl p-6 flex flex-col h-full glow-green">
                  <div className="flex items-center justify-between pb-4 border-b border-white/5 shrink-0">
                    <div className="flex items-center space-x-3">
                      <div className="p-2.5 bg-emerald-500/10 text-emerald-400 rounded-xl">
                        <Brain className="h-5 w-5" />
                      </div>
                      <div>
                        <h4 className="text-sm font-bold text-white">AuraAI Assistant</h4>
                        <p className="text-[10px] text-gray-500">Guard-Railed Fitness Coach</p>
                      </div>
                    </div>
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/15 text-emerald-400 border border-emerald-500/20">ONLINE</span>
                  </div>

                  {/* Messages container */}
                  <div className="flex-1 overflow-y-auto py-4 space-y-4 custom-scrollbar text-sm">
                    {userChatMessages.map((msg, i) => (
                      <div 
                        key={i} 
                        className={`flex flex-col max-w-[85%] rounded-2xl p-3.5 ${msg.sender === 'user' ? 'ml-auto bg-orange-500 text-white rounded-br-none' : 'bg-white/[0.03] text-gray-300 rounded-bl-none border border-white/5'}`}
                      >
                        <p className="leading-relaxed">{msg.text}</p>
                      </div>
                    ))}
                    {userChatLoading && (
                      <div className="flex items-center space-x-2 text-gray-500 italic p-2.5 bg-white/[0.01] border border-white/5 rounded-xl w-[120px]">
                        <Loader2 className="h-4 w-4 animate-spin text-orange-500" />
                        <span>Analysing...</span>
                      </div>
                    )}
                    <div ref={userChatEndRef} />
                  </div>

                  {/* Input Form */}
                  <form onSubmit={handleUserChatSubmit} className="mt-4 pt-3 border-t border-white/5 flex items-center space-x-2 shrink-0">
                    <input 
                      type="text" 
                      placeholder="Ask health/diet tips..."
                      value={userChatInput}
                      onChange={(e) => setUserChatInput(e.target.value)}
                      required
                      className="flex-1 bg-white/[0.04] border border-white/5 rounded-xl px-4 py-3 text-sm text-gray-300 focus:outline-none focus:border-emerald-500/50"
                    />
                    <button 
                      type="submit" 
                      className="p-3 rounded-xl bg-emerald-500 hover:bg-emerald-600 text-white transition shrink-0 cursor-pointer"
                    >
                      <Send className="h-4 w-4" />
                    </button>
                  </form>
                </section>
              </div>

            </div>
          )}

          {/* TAB 2: PERSONAL NOTEPAD PANEL */}
          {activeTab === 'notes' && (
            <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
              
              {/* Note Dates Sidebar */}
              <div className="md:col-span-1 glass-card rounded-2xl p-6 flex flex-col h-[500px]">
                <div className="flex items-center justify-between pb-4 border-b border-white/5 mb-4">
                  <h4 className="text-sm font-bold text-white flex items-center space-x-1">
                    <FileText className="h-4 w-4 text-orange-400" />
                    <span>Logged Dates</span>
                  </h4>
                  <button 
                    onClick={() => {
                      const d = window.prompt("Enter Date (dd/mm/yyyy):", new Date().toLocaleDateString('en-GB'));
                      if (d && d.match(/^\d{2}\/\d{2}\/\d{4}$/)) {
                        setNotesDates(prev => {
                          if (prev.includes(d)) return prev;
                          return [d, ...prev];
                        });
                        handleSelectNoteDate(d);
                      } else if (d) {
                        alert("Invalid format! Use dd/mm/yyyy");
                      }
                    }}
                    className="p-1.5 hover:bg-white/[0.04] rounded-lg text-gray-400 hover:text-white transition cursor-pointer"
                    title="Add Date Note"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                </div>
                
                <div className="flex-1 overflow-y-auto custom-scrollbar">
                  {notesDates.length === 0 ? (
                    <p className="text-gray-500 text-xs italic text-center mt-8">No date folders found.</p>
                  ) : (
                    <ul className="space-y-1">
                      {notesDates.map(d => (
                        <li key={d}>
                          <button 
                            onClick={() => handleSelectNoteDate(d)}
                            className={`w-full text-left px-3.5 py-2.5 rounded-lg text-sm transition font-medium ${activeNoteDate === d ? 'bg-orange-500/10 text-orange-400' : 'text-gray-400 hover:text-gray-200 hover:bg-white/[0.01]'}`}
                          >
                            {d}
                          </button>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>
              </div>

              {/* Notes Text Editor */}
              <div className="md:col-span-3 glass-card rounded-2xl p-6 flex flex-col h-[500px]">
                <div className="flex items-center justify-between pb-4 border-b border-white/5 mb-4">
                  <div className="flex items-center space-x-2 text-white">
                    <FileText className="h-5 w-5 text-orange-500" />
                    <span className="font-bold text-sm">{activeNoteDate ? `Note for Date: ${activeNoteDate}` : 'Select a Date Note'}</span>
                  </div>
                  <button 
                    onClick={handleSavePersonalNote}
                    disabled={!activeNoteDate}
                    className="px-4 py-2 bg-gradient-to-r from-orange-500 to-red-600 hover:from-orange-600 hover:to-red-700 text-white font-bold rounded-xl text-xs transition flex items-center space-x-1.5 disabled:opacity-30 cursor-pointer"
                  >
                    <Save className="h-3.5 w-3.5" />
                    <span>Save Note</span>
                  </button>
                </div>
                
                <textarea 
                  placeholder="Write notes regarding meals, goals, or metrics on this day..."
                  disabled={!activeNoteDate}
                  value={noteText}
                  onChange={(e) => setNoteText(e.target.value)}
                  className="flex-1 w-full bg-white/[0.01] rounded-xl p-4 text-sm text-gray-300 border border-white/5 focus:outline-none focus:border-orange-500/30 resize-none font-sans leading-relaxed"
                />
              </div>

            </div>
          )}

          {/* TAB 3: ADMIN PANEL */}
          {activeTab === 'admin' && userRoles.includes('ADMIN') && (
            <div className="glass-card rounded-2xl p-6">
              <h3 className="text-lg font-bold text-white mb-2">System Users Directory</h3>
              <p className="text-xs text-gray-500 mb-6">Manage system users, adjust access roles, or perform cleanup.</p>
              
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm border-collapse">
                  <thead>
                    <tr className="border-b border-white/5 text-gray-400 font-medium">
                      <th className="py-3 px-4">UUID</th>
                      <th className="py-3 px-4">Name</th>
                      <th className="py-3 px-4">Email</th>
                      <th className="py-3 px-4">Registered At</th>
                      <th className="py-3 px-4">System Role</th>
                      <th className="py-3 px-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {usersList.map(u => (
                      <tr key={u.id} className="hover:bg-white/[0.01]">
                        <td className="py-4 px-4 font-mono text-xs text-gray-500">{u.id}</td>
                        <td className="py-4 px-4 font-semibold text-white">{u.firstName} {u.lastName}</td>
                        <td className="py-4 px-4 text-gray-300">{u.email}</td>
                        <td className="py-4 px-4 text-gray-400">{new Date(u.createdAt).toLocaleString()}</td>
                        <td className="py-4 px-4">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${u.password === 'OIDC_USER' ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20' : 'bg-orange-500/10 text-orange-400 border border-orange-500/20'}`}>
                            {u.password === 'OIDC_USER' ? 'OIDC' : 'LOCAL'}
                          </span>
                        </td>
                        <td className="py-4 px-4 text-right flex items-center justify-end space-x-2">
                          <button 
                            onClick={() => handleUpdateRole(u.id, u.id === user.id ? 'ADMIN' : 'USER')}
                            className="px-2.5 py-1.5 rounded-lg text-xs font-bold bg-white/[0.03] hover:bg-white/[0.08] text-gray-300 border border-white/5 cursor-pointer"
                          >
                            Toggle Role
                          </button>
                          <button 
                            onClick={() => handleDeleteUser(u.id)}
                            disabled={u.id === user.id}
                            className="p-1.5 rounded-lg bg-red-500/10 hover:bg-red-500 text-red-400 hover:text-white transition disabled:opacity-10 cursor-pointer"
                            title="Delete User"
                          >
                            <Trash2 className="h-4 w-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

        </div>
      </main>

      {/* MODAL: ADD ACTIVITY */}
      {showAddModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setShowAddModal(false)} className="absolute inset-0 bg-[#070b13]/80 backdrop-blur-sm" />
          
          <div className="glass-card w-full max-w-lg rounded-2xl z-10 overflow-hidden relative glow-orange animate-in fade-in zoom-in duration-200">
            <header className="px-6 py-4 border-b border-white/5 flex items-center justify-between">
              <h3 className="font-bold text-white font-outfit text-base">Log Physical Training</h3>
              <button onClick={() => setShowAddModal(false)} className="p-1 text-gray-500 hover:text-white transition cursor-pointer">
                <X className="h-5 w-5" />
              </button>
            </header>
            
            <form onSubmit={handleAddActivitySubmit}>
              <div className="p-6 space-y-4 max-h-[480px] overflow-y-auto custom-scrollbar">
                
                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col space-y-1.5">
                    <label className="text-xs text-gray-400 font-semibold">Activity Type</label>
                    <select 
                      value={activityType} 
                      onChange={(e) => setActivityType(e.target.value)}
                      className="bg-white/[0.04] border border-white/5 rounded-xl px-3 py-2.5 text-sm text-gray-300 focus:outline-none focus:border-orange-500/30"
                    >
                      <option value="RUNNING">Running</option>
                      <option value="WALKING">Walking</option>
                      <option value="CYCLING">Cycling</option>
                      <option value="SWIMMING">Swimming</option>
                      <option value="WEIGHT_TRAINING">Weight Training</option>
                      <option value="YOGA">Yoga</option>
                      <option value="HIIT">HIIT</option>
                      <option value="CARDIO">Cardio</option>
                      <option value="STRETCHING">Stretching</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                  
                  <div className="flex flex-col space-y-1.5">
                    <label className="text-xs text-gray-400 font-semibold">Start Date & Time</label>
                    <input 
                      type="datetime-local" 
                      required 
                      value={startTime}
                      onChange={(e) => setStartTime(e.target.value)}
                      className="bg-white/[0.04] border border-white/5 rounded-xl px-3 py-2.5 text-sm text-gray-300 focus:outline-none"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="flex flex-col space-y-1.5">
                    <label className="text-xs text-gray-400 font-semibold">Duration (Minutes)</label>
                    <input 
                      type="number" 
                      min="1" 
                      required 
                      placeholder="e.g. 45"
                      value={duration}
                      onChange={(e) => setDuration(e.target.value)}
                      className="bg-white/[0.04] border border-white/5 rounded-xl px-3 py-2.5 text-sm text-gray-300 focus:outline-none"
                    />
                  </div>
                  
                  <div className="flex flex-col space-y-1.5">
                    <label className="text-xs text-gray-400 font-semibold">Calories Burned (kcal)</label>
                    <input 
                      type="number" 
                      min="1" 
                      required 
                      placeholder="e.g. 350"
                      value={calories}
                      onChange={(e) => setCalories(e.target.value)}
                      className="bg-white/[0.04] border border-white/5 rounded-xl px-3 py-2.5 text-sm text-gray-300 focus:outline-none"
                    />
                  </div>
                </div>

                {/* Additional Metrics */}
                <div className="pt-2">
                  <div className="flex items-center justify-between pb-2 border-b border-white/5 mb-3">
                    <span className="text-xs font-semibold text-gray-400">Additional Biometrics (e.g. avgHeartRate)</span>
                    <button 
                      type="button" 
                      onClick={addMetricRow}
                      className="text-xs font-bold text-orange-400 hover:text-orange-300 transition flex items-center space-x-1 cursor-pointer"
                    >
                      <Plus className="h-3.5 w-3.5" />
                      <span>Add Biometric</span>
                    </button>
                  </div>

                  <div className="space-y-2">
                    {customMetrics.map((item, idx) => (
                      <div key={idx} className="flex items-center space-x-2">
                        <input 
                          type="text" 
                          placeholder="Key (e.g. cadence)" 
                          value={item.key}
                          onChange={(e) => updateMetricKey(idx, e.target.value)}
                          required
                          className="flex-1 bg-white/[0.04] border border-white/5 rounded-xl px-3 py-2 text-xs text-gray-300 focus:outline-none"
                        />
                        <input 
                          type="number" 
                          placeholder="Value (e.g. 5.5)" 
                          step="any"
                          min="0"
                          value={item.value}
                          onChange={(e) => updateMetricValue(idx, e.target.value)}
                          required
                          className="w-1/3 bg-white/[0.04] border border-white/5 rounded-xl px-3 py-2 text-xs text-gray-300 focus:outline-none"
                        />
                        <button 
                          type="button" 
                          onClick={() => removeMetricRow(idx)}
                          className="p-1.5 bg-red-500/10 hover:bg-red-500 text-red-400 hover:text-white rounded-lg cursor-pointer"
                        >
                          <X className="h-3.5 w-3.5" />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Training Commentary */}
                <div className="flex flex-col space-y-1.5">
                  <label className="text-xs text-gray-400 font-semibold">Training Commentary (User Note)</label>
                  <textarea 
                    rows="3" 
                    placeholder="Describe how your body felt or special conditions..."
                    value={userCommentary}
                    onChange={(e) => setUserCommentary(e.target.value)}
                    className="bg-white/[0.04] border border-white/5 rounded-xl p-3 text-sm text-gray-300 focus:outline-none focus:border-orange-500/30 resize-none"
                  />
                </div>

              </div>
              
              <footer className="px-6 py-4 border-t border-white/5 flex items-center justify-end space-x-2">
                <button 
                  type="button" 
                  onClick={() => setShowAddModal(false)}
                  className="px-4 py-2 border border-white/5 hover:bg-white/[0.02] text-sm text-gray-400 rounded-xl cursor-pointer"
                >
                  Cancel
                </button>
                <button 
                  type="submit"
                  className="px-5 py-2 bg-gradient-to-r from-orange-500 to-red-600 hover:from-orange-600 hover:to-red-700 font-bold text-white rounded-xl text-sm transition shadow-lg shadow-orange-500/10 cursor-pointer"
                >
                  Log Activity
                </button>
              </footer>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: ACTIVITY DETAIL & RECOMMENDATION */}
      {showDetailModal && activeActivity && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setShowDetailModal(false)} className="absolute inset-0 bg-[#070b13]/80 backdrop-blur-sm" />
          
          <div className="glass-card w-full max-w-5xl rounded-2xl z-10 overflow-hidden relative flex flex-col h-[90vh] glow-orange animate-in fade-in zoom-in duration-200">
            <header className="px-8 py-5 border-b border-white/5 flex items-center justify-between shrink-0">
              <h3 className="font-bold text-white font-outfit text-base">
                Workout Analysis Insights: {activeActivity.activityType}
              </h3>
              <button onClick={() => setShowDetailModal(false)} className="p-1 text-gray-500 hover:text-white transition cursor-pointer">
                <X className="h-5 w-5" />
              </button>
            </header>

            <div className="flex-1 overflow-hidden flex flex-col md:flex-row">
              {/* Left Col (Scrollable Analysis & Notepad) */}
              <div className="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar">
                
                <section className="bg-white/[0.02] border border-white/5 rounded-2xl p-5">
                  <h4 className="text-sm font-bold text-white mb-4">AI Coach Analysis</h4>
                  
                  {recLoading && (
                    <div className="flex flex-col items-center justify-center py-12 text-gray-400">
                      <Loader2 className="h-8 w-8 animate-spin text-orange-500 mb-3" />
                      <p className="text-sm tracking-wide">Generating AI Coaching Insights... please wait a moment</p>
                    </div>
                  )}

                  {recError && (
                    <div className="flex flex-col items-center text-center py-8 px-4 bg-orange-500/5 border border-orange-500/10 rounded-xl">
                      <AlertTriangle className="h-8 w-8 text-orange-400 mb-2" />
                      <p className="text-sm text-orange-300 leading-relaxed">{recError}</p>
                    </div>
                  )}

                  {!recLoading && !recError && activeRecommendation && (
                    <div className="space-y-4">
                      {/* Analysis Block */}
                      <div className="grid grid-cols-2 gap-4">
                        <div className="p-3 bg-white/[0.02] rounded-xl border border-white/5">
                          <span className="text-[10px] text-gray-500 font-bold uppercase tracking-wider">Overall Analysis</span>
                          <p className="text-xs text-gray-300 mt-1 leading-relaxed">{activeRecommendation.analysis?.overall}</p>
                        </div>
                        
                        <div className="p-3 bg-white/[0.02] rounded-xl border border-white/5">
                          <span className="text-[10px] text-gray-500 font-bold uppercase tracking-wider">Pacing Analysis</span>
                          <p className="text-xs text-gray-300 mt-1 leading-relaxed">{activeRecommendation.analysis?.pace}</p>
                        </div>
                        
                        <div className="p-3 bg-white/[0.02] rounded-xl border border-white/5">
                          <span className="text-[10px] text-gray-500 font-bold uppercase tracking-wider">Heart Rate Analysis</span>
                          <p className="text-xs text-gray-300 mt-1 leading-relaxed">{activeRecommendation.analysis?.heartRate}</p>
                        </div>

                        <div className="p-3 bg-white/[0.02] rounded-xl border border-white/5">
                          <span className="text-[10px] text-gray-500 font-bold uppercase tracking-wider">Efficiency Analysis</span>
                          <p className="text-xs text-gray-300 mt-1 leading-relaxed">{activeRecommendation.analysis?.caloriesBurned}</p>
                        </div>
                      </div>

                      {/* Verdict Text */}
                      <div className="p-4 bg-orange-500/5 border border-orange-500/10 rounded-xl">
                        <span className="text-[10px] text-orange-400 font-bold uppercase tracking-wider flex items-center space-x-1 mb-1">
                          <Sparkles className="h-3 w-3" />
                          <span>AI Coach Verdict</span>
                        </span>
                        <p className="text-sm font-semibold text-gray-200 leading-relaxed">
                          {activeRecommendation.recommendation}
                        </p>
                      </div>

                      {/* Lists grid */}
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
                        <div className="p-4.5 rounded-xl bg-emerald-500/5 border border-emerald-500/10 text-gray-300">
                          <h5 className="font-bold text-emerald-400 mb-2">Improvements</h5>
                          <ul className="list-disc pl-4 space-y-1.5">
                            {activeRecommendation.improvements?.map((item, idx) => (
                              <li key={idx}>{item}</li>
                            ))}
                          </ul>
                        </div>
                        
                        <div className="p-4.5 rounded-xl bg-red-500/5 border border-red-500/10 text-gray-300">
                          <h5 className="font-bold text-red-400 mb-2">Safety Advisories</h5>
                          <ul className="list-disc pl-4 space-y-1.5">
                            {activeRecommendation.safety?.map((item, idx) => (
                              <li key={idx}>{item}</li>
                            ))}
                          </ul>
                        </div>

                        <div className="p-4.5 rounded-xl bg-blue-500/5 border border-blue-500/10 text-gray-300">
                          <h5 className="font-bold text-blue-400 mb-2">Suggestions</h5>
                          <ul className="list-disc pl-4 space-y-1.5">
                            {activeRecommendation.suggestions?.map((item, idx) => (
                              <li key={idx}>{item}</li>
                            ))}
                          </ul>
                        </div>
                      </div>

                    </div>
                  )}
                </section>

                {/* Specific notes for recommendation */}
                <section className="bg-white/[0.02] border border-white/5 rounded-2xl p-5">
                  <div className="flex items-center justify-between pb-3 border-b border-white/5 mb-4">
                    <h4 className="text-sm font-bold text-white flex items-center space-x-1.5">
                      <FileText className="h-4 w-4 text-orange-400" />
                      <span>Recommendation Notes Diary</span>
                    </h4>
                    <div className="flex items-center space-x-2 text-xs">
                      <span className="text-gray-500">Date Group:</span>
                      <span className="font-bold text-gray-300">{recNoteDate}</span>
                    </div>
                  </div>
                  
                  <textarea 
                    value={recNoteText}
                    onChange={(e) => setRecNoteText(e.target.value)}
                    placeholder="Jot down notes specific to this workout session recommendation or personal goals..."
                    className="w-full bg-white/[0.01] rounded-xl border border-white/5 p-4 text-sm text-gray-300 focus:outline-none focus:border-orange-500/30 resize-none h-[120px]"
                  />
                  <div className="flex justify-end mt-3">
                    <button 
                      onClick={handleSaveRecNote}
                      className="px-3.5 py-1.5 bg-gradient-to-r from-orange-500 to-red-600 hover:from-orange-600 hover:to-red-700 text-white font-bold rounded-lg text-xs transition flex items-center space-x-1 cursor-pointer"
                    >
                      <Save className="h-3 w-3" />
                      <span>Save Note</span>
                    </button>
                  </div>
                </section>
              </div>

              {/* Right Col (Chatbot specific to recommendation) */}
              <div className="w-full md:w-80 border-t md:border-t-0 md:border-l border-white/5 flex flex-col p-6 shrink-0 h-full">
                <section className="flex flex-col h-full">
                  <div className="flex items-center space-x-3 pb-4 border-b border-white/5 shrink-0">
                    <div className="p-2.5 bg-orange-500/10 text-orange-400 rounded-xl">
                      <Brain className="h-5 w-5" />
                    </div>
                    <div>
                      <h4 className="text-sm font-bold text-white">Coach assistant</h4>
                      <p className="text-[10px] text-gray-500">Answering recommendation queries</p>
                    </div>
                  </div>

                  {/* Messages */}
                  <div className="flex-1 overflow-y-auto py-4 space-y-4 custom-scrollbar text-xs">
                    {recChatMessages.map((msg, i) => (
                      <div 
                        key={i}
                        className={`flex flex-col max-w-[85%] rounded-2xl p-3 ${msg.sender === 'user' ? 'ml-auto bg-orange-500 text-white rounded-br-none' : 'bg-white/[0.03] text-gray-300 rounded-bl-none border border-white/5'}`}
                      >
                        <p className="leading-relaxed">{msg.text}</p>
                      </div>
                    ))}
                    {recChatLoading && (
                      <div className="flex items-center space-x-2 text-gray-500 italic p-2 bg-white/[0.01] border border-white/5 rounded-xl w-[100px]">
                        <Loader2 className="h-3.5 w-3.5 animate-spin text-orange-500" />
                        <span>Typing...</span>
                      </div>
                    )}
                    <div ref={recChatEndRef} />
                  </div>

                  {/* Input Form */}
                  <form onSubmit={handleRecChatSubmit} className="mt-4 pt-3 border-t border-white/5 flex items-center space-x-2 shrink-0">
                    <input 
                      type="text" 
                      placeholder="Ask about this analysis..."
                      value={recChatInput}
                      onChange={(e) => setRecChatInput(e.target.value)}
                      required
                      disabled={!activeRecommendation}
                      className="flex-1 bg-white/[0.04] border border-white/5 rounded-xl px-3.5 py-2 text-xs text-gray-300 focus:outline-none focus:border-orange-500/30"
                    />
                    <button 
                      type="submit" 
                      disabled={!activeRecommendation}
                      className="p-2.5 rounded-xl bg-orange-500 hover:bg-orange-600 text-white transition shrink-0 disabled:opacity-30 cursor-pointer"
                    >
                      <Send className="h-3.5 w-3.5" />
                    </button>
                  </form>
                </section>
              </div>
            </div>
          </div>
        </div>
      )}




      {/* API Configuration Setting Trigger Modal shortcut */}
      {showKeyModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div onClick={() => setShowKeyModal(false)} className="absolute inset-0 bg-[#070b13]/80 backdrop-blur-sm" />
          
          <div className="glass-card w-full max-w-sm rounded-2xl z-10 overflow-hidden relative glow-orange animate-in fade-in zoom-in duration-200">
            <header className="px-6 py-4 border-b border-white/5 flex items-center justify-between">
              <h3 className="font-bold text-white font-outfit text-sm">Configure API Settings</h3>
              <button onClick={() => setShowKeyModal(false)} className="p-1 text-gray-500 hover:text-white transition cursor-pointer">
                <X className="h-4 w-4" />
              </button>
            </header>

            <div className="p-6 space-y-4">
              <div className="flex flex-col space-y-1.5">
                <label className="text-xs text-gray-400 font-semibold">Gemini API Key</label>
                <input 
                  type="password"
                  value={sessionApiKey}
                  onChange={(e) => setSessionApiKey(e.target.value)}
                  placeholder="Enter your Gemini API key"
                  className="bg-white/[0.04] border border-white/5 rounded-xl px-3.5 py-2.5 text-sm text-gray-300 focus:outline-none"
                />
                <p className="text-[10px] text-gray-500 leading-normal mt-1">Your API key is saved locally in this browser session.</p>
              </div>
            </div>

            <footer className="px-6 py-4 border-t border-white/5 flex items-center justify-end space-x-2">
              <button 
                onClick={clearApiKey}
                className="px-3.5 py-2 border border-white/5 hover:bg-white/[0.02] text-xs text-red-400 rounded-xl cursor-pointer"
              >
                Clear Key
              </button>
              <button 
                onClick={saveApiKey}
                className="px-4 py-2 bg-gradient-to-r from-orange-500 to-red-600 hover:from-orange-600 hover:to-red-700 font-bold text-white rounded-xl text-xs transition cursor-pointer"
              >
                Apply Settings
              </button>
            </footer>
          </div>
        </div>
      )}

    </div>
  );
}
