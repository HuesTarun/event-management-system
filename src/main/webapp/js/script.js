// 1. Helpers & API Utilities
const BASE = (() => {
  const p = window.location.pathname.split('/').filter(Boolean);
  return (p.length && p[0] !== 'admin' && !p[0].endsWith('.html')) ? '/' + p[0] : '';
})();

const $ = id => document.getElementById(id);
const param = name => new URLSearchParams(window.location.search).get(name);

function formatDate(str) {
  if (!str) return 'TBA';
  const d = new Date(str.replace(' ', 'T'));
  return isNaN(d) ? str : d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

async function apiGet(url) {
  try {
    const res = await fetch(BASE + url);
    return await res.json();
  } catch (err) {
    console.error('GET error:', err);
    return {};
  }
}

async function apiPost(url, data) {
  try {
    const res = await fetch(BASE + url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(data)
    });
    return await res.json();
  } catch (err) {
    alert('Server connection error. Please try again.');
    return { error: 'Network error' };
  }
}

// 2. Authentication & Session
async function checkLogin() {
  const err = param('error');
  if (err && $('login-msg')) {
    $('login-msg').innerHTML = `<div class="alert alert-danger" style="margin-bottom:15px;">${err === 'auth_required' ? 'Please log in to continue.' : 'Access denied: Admin role required.'
      }</div>`;
  }

  const data = await apiGet('/api/login/status');
  const authDiv = $('nav-auth');
  if (!authDiv) return;

  if (data?.loggedIn && data.user) {
    authDiv.innerHTML = `<span style="color:#818cf8; margin-right:10px;">${data.user.name}</span>
      <button onclick="logout()" class="btn btn-secondary btn-sm">Logout</button>`;
    if (data.user.role === 'STUDENT' && $('nav-my-events')) $('nav-my-events').style.display = 'inline';
    if (data.user.role === 'ADMIN' && $('nav-admin')) $('nav-admin').style.display = 'inline';
  } else {
    authDiv.innerHTML = `<a href="${BASE}/login.html" class="btn btn-secondary btn-sm">Login</a> 
      <a href="${BASE}/register.html" class="btn btn-primary btn-sm">Register</a>`;
  }
}

async function logout() {
  await apiPost('/api/logout', {});
  alert('Logged out');
  window.location.href = BASE + '/login.html';
}

async function handleLogin(e) {
  e.preventDefault();
  const res = await apiPost('/api/login', { email: $('email').value, password: $('password').value });
  if (res.success) {
    window.location.href = res.user?.role === 'ADMIN' ? `${BASE}/admin/dashboard.html` : `${BASE}/events.html`;
  } else {
    alert(res.error || 'Login failed');
  }
}

async function handleRegister(e) {
  e.preventDefault();
  if ($('password').value !== $('confirmPassword').value) return alert('Passwords do not match');

  const res = await apiPost('/api/register', {
    name: $('name').value, email: $('email').value,
    password: $('password').value, confirmPassword: $('confirmPassword').value
  });
  if (res.success) {
    alert('Registration successful! Please login.');
    window.location.href = BASE + '/login.html';
  } else {
    alert(res.error || 'Sign up failed');
  }
}

// 3. Home Page Stats
async function loadHome() {
  const data = await apiGet('/api/events');
  const events = data.events || [];
  if ($('stat-events')) $('stat-events').innerText = events.length;
  if ($('stat-registrations')) {
    $('stat-registrations').innerText = events.reduce((sum, e) => sum + (e.registeredCount || 0), 0);
  }
}

// 4. Events Catalog
async function loadEvents() {
  const data = await apiGet('/api/events');
  const list = data.events || [];
  $('events-list').innerHTML = list.length ? list.map(e => {
    const badge = e.full ? '<span class="badge badge-danger">Full</span>'
      : (e.past ? '<span class="badge badge-warning">Past</span>' : '<span class="badge badge-success">Open</span>');
    return `
      <div class="card event-card">
        <div>
          ${badge}
          <h3 style="margin-top:8px;">${e.title}</h3>
          <p style="color:#94a3b8; font-size:0.85rem;">${formatDate(e.eventDate)} • ${e.venue}</p>
          <p style="margin:8px 0; font-size:0.9rem;">Seats: ${e.registeredCount} / ${e.capacity}</p>
        </div>
        <a href="event-details.html?id=${e.id}" class="btn btn-primary btn-sm">Details &amp; Register</a>
      </div>`;
  }).join('') : '<p style="color:#94a3b8;">No events found.</p>';
}

// 5. Event Details & Registration
async function loadEventDetails() {
  const id = param('id');
  if (!id) return;

  const e = await apiGet('/api/event-details?id=' + id);
  const box = $('event-details-box');
  if (e.error) return box.innerHTML = `<p style="color:#ef4444;">${e.error}</p>`;

  let action = `<button onclick="registerForEvent(${e.id})" class="btn btn-primary" id="reg-btn">Register Now</button>`;
  if (e.alreadyRegistered) {
    action = `<div class="alert alert-success">You are registered! <a href="${BASE}/my-events.html">View Registrations</a></div>`;
  } else if (e.full) {
    action = '<button class="btn btn-danger" disabled>Sold Out</button>';
  } else if (e.past) {
    action = '<button class="btn btn-secondary" disabled>Event Completed</button>';
  }

  box.innerHTML = `
    <h1>${e.title}</h1>
    <p style="color:#94a3b8; margin:10px 0;"><strong>Date:</strong> ${formatDate(e.eventDate)} | <strong>Venue:</strong> ${e.venue}</p>
    <p style="color:#818cf8; margin-bottom:15px;">Capacity: ${e.registeredCount} / ${e.capacity} seats filled (${e.availableSeats} left)</p>
    <hr style="border-color:#334155; margin:15px 0;">
    <p style="line-height:1.6;">${e.description || 'No description.'}</p>
    <div style="margin-top:20px;">${action}</div>`;
}

async function registerForEvent(id) {
  const btn = $('reg-btn');
  if (btn) btn.disabled = true;

  const res = await apiPost('/api/events/register', { eventId: id });
  if (res.success) {
    alert('Registered successfully! Your ticket pass is ready.');
    window.location.href = BASE + '/my-events.html';
  } else {
    alert(res.error || 'Registration failed. Please make sure you are logged in.');
    if (btn) btn.disabled = false;
  }
}

// 6. Student Registrations & Passes
async function loadMyEvents() {
  const data = await apiGet('/api/my-events');
  const list = data.registrations || [];
  $('my-events-table').innerHTML = list.length ? list.map(r => `
    <tr>
      <td><strong>${r.eventTitle}</strong></td>
      <td>${formatDate(r.eventDate)}</td>
      <td>${r.eventVenue}</td>
      <td><span class="badge badge-success">Registered</span></td>
      <td>
        <button onclick="showPassModal('${r.ticketToken}', '${encodeURIComponent(r.eventTitle)}', '${encodeURIComponent(formatDate(r.eventDate))}', '${encodeURIComponent(r.eventVenue || 'TBA')}')" class="btn btn-primary btn-sm" style="margin-right:6px;">🎟️ View QR Pass</button>
        <a href="ticket.html?token=${encodeURIComponent(r.ticketToken)}" target="_blank" class="btn btn-secondary btn-sm" title="Open full pass page">🖨️</a>
      </td>
    </tr>`).join('') : '<tr><td colspan="5" style="text-align:center; color:#94a3b8; padding:25px;">No registered events.</td></tr>';
}

function showPassModal(token, title, date, venue) {
  const modal = $('qr-modal');
  if (!modal) return;
  $('modal-event-title').innerText = decodeURIComponent(title);
  $('modal-event-meta').innerText = `${decodeURIComponent(date)} • ${decodeURIComponent(venue)}`;
  $('modal-qr-token').innerText = token;

  const qrUrl = `${BASE}/api/ticket/qr?token=${encodeURIComponent(token)}&size=240`;
  $('modal-qr-img').src = qrUrl;
  $('modal-download-btn').href = qrUrl;
  $('modal-download-btn').download = `ticket-${token}.png`;
  $('modal-view-full-btn').href = `${BASE}/ticket.html?token=${encodeURIComponent(token)}`;

  modal.style.display = 'flex';
}

function closePassModal() {
  const modal = $('qr-modal');
  if (modal) modal.style.display = 'none';
}

window.addEventListener('click', function(e) {
  const modal = $('qr-modal');
  if (modal && e.target === modal) {
    modal.style.display = 'none';
  }
});

// 7. Admin Dashboard & Event Management
async function loadAdminDashboard() {
  const data = await apiGet('/api/admin/dashboard');
  if (data.error) return;
  if ($('admin-stat-events')) $('admin-stat-events').innerText = data.totalEvents || 0;
  if ($('admin-stat-reg')) $('admin-stat-reg').innerText = data.totalRegistrations || 0;
  if ($('admin-stat-students')) $('admin-stat-students').innerText = data.totalStudents || 0;

  const list = data.recentEvents || [];
  $('dashboard-events').innerHTML = list.length ? list.map(e => `
    <tr>
      <td>#${e.id}</td>
      <td>${e.title}</td>
      <td>${formatDate(e.eventDate)}</td>
      <td>${e.registeredCount} / ${e.capacity}</td>
      <td><a href="registrations.html?eventId=${e.id}" class="btn btn-secondary btn-sm">Attendees</a></td>
    </tr>`).join('') : '<tr><td colspan="5">No events found.</td></tr>';
}

async function loadAdminEvents() {
  const data = await apiGet('/api/admin/events');
  const list = data.events || [];
  $('admin-events-body').innerHTML = list.length ? list.map(e => `
    <tr>
      <td>#${e.id}</td>
      <td><strong>${e.title}</strong></td>
      <td>${e.venue}</td>
      <td>${formatDate(e.eventDate)}</td>
      <td>${e.registeredCount} / ${e.capacity}</td>
      <td>
        <a href="registrations.html?eventId=${e.id}" class="btn btn-secondary btn-sm">Attendees</a>
        <a href="event-form.html?id=${e.id}" class="btn btn-primary btn-sm">Edit</a>
        <button onclick="deleteEvent(${e.id})" class="btn btn-danger btn-sm">Delete</button>
      </td>
    </tr>`).join('') : '<tr><td colspan="6">No events.</td></tr>';
}

async function deleteEvent(id) {
  if (!confirm('Are you sure you want to delete this event?')) return;
  await apiPost('/api/admin/events/delete', { id });
  alert('Event deleted');
  loadAdminEvents();
}

async function initEventForm() {
  const id = param('id');
  if (!id) return;
  $('form-title').innerText = 'Edit Event #' + id;
  const e = await apiGet('/api/admin/events/detail?id=' + id);
  $('event-id').value = e.id;
  $('title').value = e.title || '';
  $('venue').value = e.venue || '';
  $('capacity').value = e.capacity || 50;
  $('description').value = e.description || '';
  if (e.eventDate) $('eventDate').value = e.eventDate.replace(' ', 'T').substring(0, 16);
}

async function saveEvent(e) {
  e.preventDefault();
  const id = $('event-id').value;
  const params = {
    title: $('title').value, venue: $('venue').value,
    capacity: $('capacity').value, eventDate: $('eventDate').value,
    description: $('description').value
  };
  if (id) params.id = id;

  const res = await apiPost(id ? '/api/admin/events/edit' : '/api/admin/events/create', params);
  if (res.success) {
    alert('Event saved successfully');
    window.location.href = 'events.html';
  } else {
    alert(res.error || 'Failed to save event');
  }
}

async function loadRegistrations() {
  const eventId = param('eventId');
  if (!eventId) return;

  const data = await apiGet('/api/admin/registrations?eventId=' + eventId);
  if (data.error || !data.event) {
    if ($('event-counts')) $('event-counts').innerText = data.error || 'Failed to load attendees.';
    return;
  }
  $('event-title').innerText = `${data.event.title} (${formatDate(data.event.eventDate)})`;
  $('event-counts').innerText = `Registered Students: ${data.totalRegistered} / Capacity: ${data.event.capacity}`;

  const list = data.registrations || [];
  $('registrations-table-body').innerHTML = list.length ? list.map(r => `
    <tr>
      <td>${r.userName}</td>
      <td>${r.userEmail}</td>
      <td><code>${r.ticketToken}</code></td>
      <td>${formatDate(r.registeredAt)}</td>
      <td>
        <a href="../ticket.html?token=${encodeURIComponent(r.ticketToken)}" target="_blank" class="btn btn-secondary btn-sm" title="View Ticket & QR Pass">🎟️ View Pass</a>
      </td>
    </tr>`).join('') : '<tr><td colspan="5" style="text-align:center; color:#94a3b8; padding:20px;">No registrations for this event yet.</td></tr>';
}

// 8. Auto-initialize current page
window.onload = function () {
  checkLogin();
  if ($('stat-events')) loadHome();
  if ($('events-list')) loadEvents();
  if ($('event-details-box')) loadEventDetails();
  if ($('my-events-table')) loadMyEvents();
  if ($('dashboard-events')) loadAdminDashboard();
  if ($('admin-events-body')) loadAdminEvents();
  if ($('event-form')) initEventForm();
  if ($('registrations-table-body')) loadRegistrations();
};
