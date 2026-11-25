/**
 * Library Management System - Frontend Application Logic (Vanilla JS)
 * Communicates with Java backend via standard fetch() REST API.
 */

// Determine API Base URL (Supports opening index.html directly via file:// or via http://localhost:8080)
const API_BASE = (window.location.protocol === 'file:') ? 'http://localhost:8080' : '';

// Application State
const state = {
  resources: [],
  copies: [],
  members: [],
  borrows: [],
  fines: [],
  accessLogs: [],
  activeBorrowId: null,
  activeFine: null
};

// Formatting helpers
function formatVND(amount) {
  if (amount == null || isNaN(amount)) return "0 đ";
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function formatDate(dateStr) {
  if (!dateStr) return "-";
  return dateStr;
}

// Global Alert Helper
function showAlert(type, message) {
  const alertEl = document.getElementById('globalAlert');
  const msgEl = document.getElementById('globalAlertMsg');
  alertEl.className = `alert alert-${type}`;
  msgEl.innerHTML = message;
  alertEl.style.display = 'flex';
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

function closeAlert() {
  document.getElementById('globalAlert').style.display = 'none';
}

// Tab Switching
function switchTab(tabId, clickedBtn) {
  document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
  document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

  const btn = clickedBtn || (window.event ? (window.event.currentTarget || window.event.target) : null);
  if (btn && btn.classList) {
    btn.classList.add('active');
  }
  const target = document.getElementById(tabId);
  if (target) {
    target.classList.add('active');
  }

  // Refresh data according to tab
  if (tabId === 'tab-resources') loadResources();
  else if (tabId === 'tab-borrow') { loadMembers(); loadCopies(); loadBorrows(); }
  else if (tabId === 'tab-fines') loadFines();
  else if (tabId === 'tab-digital') { loadMembers(); loadResources(); loadAccessLogs(); loadLicenses(); loadGrantedLicenses(); }
  else if (tabId === 'tab-members') loadMembers();
}

// Check Backend Health
async function checkHealth() {
  const dot = document.getElementById('apiStatusDot');
  const text = document.getElementById('apiStatusText');
  try {
    const res = await fetch(`${API_BASE}/api/health`);
    if (res.ok) {
      dot.classList.add('active');
      text.innerText = "API: Sẵn sàng (Port 8080)";
    } else {
      dot.classList.remove('active');
      text.innerText = "API: Lỗi phản hồi (" + res.status + ")";
    }
  } catch (err) {
    dot.classList.remove('active');
    text.innerText = "API: Không thể kết nối!";
    showAlert('danger', `<strong>Không thể kết nối đến Backend API!</strong><br>
      - Đang trỏ tới: <code>${API_BASE || window.location.origin}/api/health</code><br>
      - Hãy đảm bảo bạn truy cập web tại: <a href="http://localhost:8080/" style="font-weight: bold; text-decoration: underline; color: inherit;">http://localhost:8080/</a> thay vì mở đường dẫn tệp trực tiếp.<br>
      - Kiểm tra xem tiến trình Java ApiServer có đang chạy trên cổng 8080 hay không.`);
  }
}

// =============================================================================
// TAB 1: RESOURCES CATALOG
// =============================================================================
async function loadResources() {
  const tbody = document.getElementById('resourcesBody');
  tbody.innerHTML = '<tr><td colspan="8" class="empty-state">Đang tải danh mục...</td></tr>';
  try {
    const res = await fetch(`${API_BASE}/api/resources`);
    if (!res.ok) throw new Error("HTTP " + res.status);
    state.resources = await res.json();
    renderResources(state.resources);
    populateJournalDropdown();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="empty-state" style="color: var(--danger);">Lỗi tải danh mục: ${err.message}</td></tr>`;
  }
}

function renderResources(list) {
  const tbody = document.getElementById('resourcesBody');
  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="8" class="empty-state">Không tìm thấy tài nguyên nào.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(r => {
    const isBook = r.resourceType === 'PHYSICAL_BOOK';
    const typeBadge = isBook
      ? (r.isPublic
          ? '<span class="badge badge-info">Sách in (Public)</span>'
          : '<span class="badge badge-warning">Sách in (Cần License)</span>')
      : (r.isPublic
          ? '<span class="badge badge-success">Tạp chí số (Public)</span>'
          : '<span class="badge badge-warning">Tạp chí số (Cần License)</span>');

    const extraInfo = isBook
      ? `Kệ: ${r.shelfLocation || '-'}`
      : `${r.format || 'PDF'} (${r.fileSize || 0} KB)`;

    const actionCol = isBook
      ? `<span class="badge ${r.availableCopies > 0 ? 'badge-success' : 'badge-danger'}">${r.availableCopies} / ${r.totalCopies} sẵn sàng</span>`
      : (r.isPublic
          ? `<a href="${r.url}" target="_blank" class="btn btn-success btn-sm" style="text-decoration: none;">📖 Đọc trực tuyến</a>`
          : `<a href="${r.url}" target="_blank" class="btn btn-secondary btn-sm" style="text-decoration: none;">📖 Xem trực tuyến</a>`);

    return `
      <tr>
        <td><strong>#${r.resourceId}</strong></td>
        <td><strong>${r.title}</strong></td>
        <td>${typeBadge}</td>
        <td>${r.topic || '-'}</td>
        <td>${formatDate(r.publishDate)}</td>
        <td>${extraInfo}</td>
        <td>${actionCol}</td>
      </tr>
    `;
  }).join('');
}

function filterResources() {
  const query = document.getElementById('resourceSearch').value.toLowerCase().trim();
  const typeFilter = document.getElementById('resourceTypeFilter').value;

  const filtered = state.resources.filter(r => {
    const matchType = (typeFilter === 'ALL') || (r.resourceType === typeFilter);
    const matchQuery = !query ||
      (r.title && r.title.toLowerCase().includes(query)) ||
      (r.topic && r.topic.toLowerCase().includes(query));
    return matchType && matchQuery;
  });

  renderResources(filtered);
}

// =============================================================================
// TAB 2: CIRCULATION (BORROW & RETURN)
// =============================================================================
async function loadCopies() {
  try {
    const res = await fetch(`${API_BASE}/api/copies`);
    if (!res.ok) throw new Error("HTTP " + res.status);
    state.copies = await res.json();
    populateCopyDropdown();
  } catch (err) {
    console.error("Lỗi tải danh sách bản sao:", err);
  }
}

function populateCopyDropdown() {
  const select = document.getElementById('borrowCopySelect');
  select.innerHTML = '<option value="">-- Chọn bản sao khả dụng --</option>';

  const availables = state.copies.filter(c => c.availabilityStatus === 'Available');
  if (availables.length === 0) {
    select.innerHTML += '<option value="" disabled>(Hiện không có bản sao nào sẵn sàng)</option>';
    return;
  }

  availables.forEach(c => {
    const opt = document.createElement('option');
    opt.value = c.copyId;
    opt.textContent = `[Bản sao: ${c.copyId}] ${c.bookTitle} (Trạng thái: ${c.availabilityStatus})`;
    select.appendChild(opt);
  });
}

async function loadBorrows() {
  const tbody = document.getElementById('borrowsBody');
  tbody.innerHTML = '<tr><td colspan="10" class="empty-state">Đang tải danh sách mượn sách...</td></tr>';
  try {
    const res = await fetch(`${API_BASE}/api/borrows`);
    if (!res.ok) throw new Error("HTTP " + res.status);
    state.borrows = await res.json();
    renderBorrows(state.borrows);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="10" class="empty-state" style="color: var(--danger);">Lỗi tải phiếu mượn: ${err.message}</td></tr>`;
  }
}

function renderBorrows(list) {
  const tbody = document.getElementById('borrowsBody');
  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="10" class="empty-state">Chưa có lượt mượn nào được ghi nhận.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(b => {
    let statusBadge = '';
    let actionBtn = '-';

    if (b.status === 'Returned') {
      statusBadge = '<span class="badge badge-success">Đã trả</span>';
    } else if (b.status === 'Overdue') {
      statusBadge = '<span class="badge badge-danger">Quá hạn</span>';
      actionBtn = `<button class="btn btn-danger btn-sm" onclick="openReturnModal(${b.borrowId}, '${escapeHtml(b.memberName)}', '${escapeHtml(b.bookTitle)}', '${b.dueDate}')">Trả sách (Phạt)</button>`;
    } else {
      statusBadge = '<span class="badge badge-warning">Đang mượn</span>';
      actionBtn = `<button class="btn btn-primary btn-sm" onclick="openReturnModal(${b.borrowId}, '${escapeHtml(b.memberName)}', '${escapeHtml(b.bookTitle)}', '${b.dueDate}')">Trả sách</button>`;
    }

    return `
      <tr>
        <td><strong>#${b.borrowId}</strong></td>
        <td><strong>${b.memberName}</strong> (ID: ${b.memberPersonId})</td>
        <td><span class="badge badge-secondary">${b.memberType}</span></td>
        <td>${b.bookTitle}</td>
        <td><code>${b.copyId}</code></td>
        <td>${formatDate(b.borrowDate)}</td>
        <td><strong style="color: ${b.isOverdue ? 'var(--danger)' : 'inherit'};">${formatDate(b.dueDate)}</strong></td>
        <td>${formatDate(b.returnDate)}</td>
        <td>${statusBadge}</td>
        <td>${actionBtn}</td>
      </tr>
    `;
  }).join('');
}

async function handleBorrowSubmit(e) {
  e.preventDefault();
  const memberId = parseInt(document.getElementById('borrowMemberSelect').value);
  const copyId = parseInt(document.getElementById('borrowCopySelect').value);

  if (!memberId || !copyId) {
    showAlert('danger', 'Vui lòng chọn cả thành viên và bản sao sách hợp lệ.');
    return;
  }

  const btn = document.getElementById('btnSubmitBorrow');
  btn.disabled = true;
  btn.innerText = "Đang xử lý...";

  try {
    const res = await fetch(`${API_BASE}/api/borrows`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ memberId, copyId })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showAlert('success', `<strong>Mượn sách thành công!</strong> Đã tạo phiếu mượn cho độc giả #${memberId} và gán trạng thái bản sao #${copyId} sang 'Borrowed'.`);
      document.getElementById('borrowForm').reset();
      loadBorrows();
      loadCopies();
      loadMembers();
      loadResources();
    } else {
      showAlert('danger', `<strong>Từ chối mượn sách [${data.errorCode || 'ERROR'}]:</strong> ${data.message}`);
    }
  } catch (err) {
    showAlert('danger', `Lỗi mạng khi mượn sách: ${err.message}`);
  } finally {
    btn.disabled = false;
    btn.innerText = "📤 Xác nhận mượn sách";
  }
}

// Return Modal Logic
function openReturnModal(borrowId, memberName, bookTitle, dueDate) {
  state.activeBorrowId = borrowId;
  const infoEl = document.getElementById('returnBookInfo');
  infoEl.innerHTML = `Lượt mượn <strong>#${borrowId}</strong> — Độc giả: <strong>${memberName}</strong><br>Sách: <em>${bookTitle}</em> (Hạn trả: <strong>${dueDate}</strong>)`;
  
  // Default to today
  document.getElementById('returnDateInput').value = new Date().toISOString().split('T')[0];
  document.getElementById('returnModal').classList.add('show');
}

function closeReturnModal() {
  document.getElementById('returnModal').classList.remove('show');
  state.activeBorrowId = null;
}

async function confirmReturn() {
  if (!state.activeBorrowId) return;
  const returnDate = document.getElementById('returnDateInput').value;
  const btn = document.getElementById('btnConfirmReturn');
  btn.disabled = true;
  btn.innerText = "Đang cập nhật...";

  try {
    const res = await fetch(`${API_BASE}/api/borrows/${state.activeBorrowId}/return`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ returnDate })
    });
    const data = await res.json();

    if (res.ok && data.success) {
      closeReturnModal();
      if (data.fineGenerated && data.fine) {
        showAlert('warning', `<strong>Trả sách hoàn tất nhưng bị trễ hạn!</strong> Hệ thống đã tự động phát sinh phiếu phạt <strong>Fine #${data.fine.fineId}</strong> với số tiền <strong>${formatVND(data.fine.amount)}</strong>. Vui lòng sang tab Tiền phạt để xem chi tiết.`);
      } else {
        showAlert('success', `<strong>Trả sách thành công!</strong> Bản sao sách đã được phục hồi trạng thái 'Available'.`);
      }
      loadBorrows();
      loadCopies();
      loadMembers();
      loadResources();
      loadFines();
    } else {
      showAlert('danger', `<strong>Lỗi trả sách [${data.errorCode || 'ERROR'}]:</strong> ${data.message}`);
    }
  } catch (err) {
    showAlert('danger', `Lỗi kết nối khi trả sách: ${err.message}`);
  } finally {
    btn.disabled = false;
    btn.innerText = "Xác nhận trả sách";
  }
}

// =============================================================================
// TAB 3: FINES & PAYMENTS
// =============================================================================
async function loadFines() {
  const tbody = document.getElementById('finesBody');
  tbody.innerHTML = '<tr><td colspan="7" class="empty-state">Đang tải danh sách phiếu phạt...</td></tr>';
  try {
    const res = await fetch(`${API_BASE}/api/fines`);
    if (!res.ok) throw new Error("HTTP " + res.status);
    state.fines = await res.json();
    renderFines(state.fines);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty-state" style="color: var(--danger);">Lỗi tải phiếu phạt: ${err.message}</td></tr>`;
  }
}

function renderFines(list) {
  const tbody = document.getElementById('finesBody');
  let totalCount = 0;
  let unpaidCount = 0;
  let paidCount = 0;

  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" class="empty-state">Không có phiếu phạt nào trong hệ thống.</td></tr>';
    document.getElementById('statTotalFines').innerText = '0';
    document.getElementById('statUnpaidFines').innerText = '0';
    const paidEl = document.getElementById('statPaidFines');
    if (paidEl) paidEl.innerText = '0';
    return;
  }

  tbody.innerHTML = list.map(f => {
    totalCount++;
    const isCompleted = (f.paymentStatus === 'Completed');
    if (!isCompleted) {
      unpaidCount++;
    } else {
      paidCount++;
    }

    const statusBadge = isCompleted
      ? '<span class="badge badge-success">Đã thanh toán</span>'
      : '<span class="badge badge-danger">Chưa thanh toán</span>';

    const payAction = isCompleted
      ? '<span class="badge badge-success" style="padding: 6px 14px; font-size: 0.85rem; font-weight: 600;">Hoàn tất</span>'
      : `<button class="btn btn-primary btn-sm" id="btn-pay-${f.fineId}" onclick="confirmFinePayment(${f.fineId})">Xác nhận thanh toán</button>`;

    return `
      <tr>
        <td><strong>#${f.fineId}</strong></td>
        <td>Lượt #${f.borrowId}</td>
        <td><strong>${f.memberName}</strong> (ID: ${f.memberPersonId})</td>
        <td>${f.bookTitle}</td>
        <td><strong>${formatVND(f.totalFineAmount)}</strong></td>
        <td>${statusBadge}</td>
        <td><div id="action-cell-${f.fineId}">${payAction}</div></td>
      </tr>
    `;
  }).join('');

  document.getElementById('statTotalFines').innerText = totalCount;
  document.getElementById('statUnpaidFines').innerText = unpaidCount;
  const paidEl = document.getElementById('statPaidFines');
  if (paidEl) paidEl.innerText = paidCount;
}

async function confirmFinePayment(fineId) {
  const actionCell = document.getElementById(`action-cell-${fineId}`);
  const btn = document.getElementById(`btn-pay-${fineId}`);
  if (btn) {
    btn.disabled = true;
    btn.innerText = "Đang xác nhận...";
  }

  try {
    const res = await fetch(`${API_BASE}/api/fines/${fineId}/payments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({})
    });
    const data = await res.json();

    if (res.ok && data.success) {
      showAlert('success', `<strong>Xác nhận thanh toán thành công!</strong> Thủ thư đã xác nhận nộp phạt cho phiếu phạt #${fineId}. Trạng thái cập nhật: Đã thanh toán.`);
      // Khi đã bấm thanh toán thì ô thanh toán chuyển thành màu xanh ghi chữ hoàn tất
      if (actionCell) {
        actionCell.innerHTML = '<span class="badge badge-success" style="padding: 6px 14px; font-size: 0.85rem; font-weight: 600;">Hoàn tất</span>';
      }
      loadFines();
      loadMembers();
    } else {
      showAlert('danger', `<strong>Lỗi xác nhận thanh toán [${data.errorCode || 'ERROR'}]:</strong> ${data.message}`);
      if (btn) {
        btn.disabled = false;
        btn.innerText = "Xác nhận thanh toán";
      }
    }
  } catch (err) {
    showAlert('danger', `Lỗi mạng khi xác nhận thanh toán: ${err.message}`);
    if (btn) {
      btn.disabled = false;
      btn.innerText = "Xác nhận thanh toán";
    }
  }
}

// =============================================================================
// TAB 4: DIGITAL JOURNALS & ACCESS LOGS
// =============================================================================
function populateJournalDropdown() {
  const select = document.getElementById('accessJournalSelect');
  if (!select) return;
  select.innerHTML = '<option value="">-- Chọn bài báo / tạp chí số --</option>';

  const journals = state.resources.filter(r => r.resourceType === 'DIGITAL_JOURNAL');
  journals.forEach(j => {
    const opt = document.createElement('option');
    opt.value = j.resourceId;
    const accessTag = j.isPublic ? " [🔓 Open Access / Công khai]" : " [🔒 Cần License]";
    opt.textContent = `[ID: ${j.resourceId}] ${j.title}${accessTag}`;
    select.appendChild(opt);
  });
}

async function handleJournalAccessSubmit(e) {
  e.preventDefault();
  const personSelect = document.getElementById('accessPersonSelect');
  const personId = personSelect && personSelect.value ? parseInt(personSelect.value) : null;
  const journalId = parseInt(document.getElementById('accessJournalSelect').value);
  const ipAddress = document.getElementById('accessIpInput').value.trim() || '127.0.0.1';
  const accessDate = document.getElementById('accessDateInput').value;

  if (!journalId) {
    alert("Vui lòng chọn tài liệu số.");
    return;
  }

  const resultBox = document.getElementById('accessResultBox');
  const btn = document.getElementById('btnSubmitAccess');
  btn.disabled = true;
  btn.innerText = "Đang thẩm định quyền...";

  try {
    const res = await fetch(`${API_BASE}/api/digital-journals/${journalId}/access`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ personId, ipAddress, accessDate })
    });
    const data = await res.json();

    resultBox.style.display = 'block';

    if (data.accessGranted) {
      resultBox.innerHTML = `
        <div class="alert alert-success">
          <div>
            <h4 style="font-weight: 700; margin-bottom: 4px;">✅ CẤP QUYỀN TRUY CẬP THÀNH CÔNG (BR04 / BR12 VALIDATED)</h4>
            <p>${data.message}</p>
            <p style="margin-top: 8px;">
              Tài liệu: <strong>${data.title}</strong><br>
              Định dạng: <strong>${data.format}</strong> (${data.fileSize} KB)
            </p>
            <p style="margin-top: 10px;">
              <a href="${data.url}" target="_blank" class="btn btn-success btn-sm" style="text-decoration: none;">📖 Đọc trực tuyến (Online Reader)</a>
            </p>
          </div>
        </div>
      `;
    } else {
      resultBox.innerHTML = `
        <div class="alert alert-danger">
          <div>
            <h4 style="font-weight: 700; margin-bottom: 4px;">⛔ TRUY CẬP BỊ TỪ CHỐI [${data.errorCode || 'BR04_LICENSE_EXPIRED'}]</h4>
            <p>${data.message}</p>
            <p style="margin-top: 8px; font-size: 0.85rem; color: #450a0a;">
              * Quy chế BR04 & BR12: Người dùng bắt buộc phải sở hữu gói bản quyền (GRANTED_LICENSE) còn hiệu lực cho tài liệu này vào ngày ${accessDate}.
            </p>
          </div>
        </div>
      `;
    }

    // Refresh logs to show newly created log record immediately
    loadAccessLogs();
  } catch (err) {
    resultBox.style.display = 'block';
    resultBox.innerHTML = `<div class="alert alert-danger">Lỗi kết nối kiểm tra bản quyền: ${err.message}</div>`;
  } finally {
    btn.disabled = false;
    btn.innerText = "📖 Thẩm định quyền & Đọc trực tuyến";
  }
}

async function loadAccessLogs() {
  const tbody = document.getElementById('accessLogsBody');
  tbody.innerHTML = '<tr><td colspan="3" class="empty-state">Đang tải nhật ký...</td></tr>';
  try {
    const res = await fetch(`${API_BASE}/api/access-logs`);
    if (!res.ok) throw new Error("HTTP " + res.status);
    state.accessLogs = await res.json();
    renderAccessLogs(state.accessLogs);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="3" class="empty-state" style="color: var(--danger);">Lỗi tải nhật ký: ${err.message}</td></tr>`;
  }
}

function renderAccessLogs(list) {
  const tbody = document.getElementById('accessLogsBody');
  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="3" class="empty-state">Chưa có bản ghi nhật ký truy cập nào.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(l => {
    return `
      <tr>
        <td><strong>#${l.logId}</strong></td>
        <td>${l.journalTitle ? `<strong>${l.journalTitle}</strong> (ID: ${l.journalResourceId})` : 'Tài nguyên #' + l.journalResourceId}</td>
        <td><code>${l.ipAddress || '-'}</code></td>
      </tr>
    `;
  }).join('');
}

// =============================================================================
// TAB 5: MEMBERS
// =============================================================================
async function loadMembers() {
  const tbody = document.getElementById('membersBody');
  tbody.innerHTML = '<tr><td colspan="10" class="empty-state">Đang tải danh sách độc giả...</td></tr>';
  try {
    const res = await fetch(`${API_BASE}/api/members`);
    if (!res.ok) throw new Error("HTTP " + res.status);
    state.members = await res.json();
    renderMembers(state.members);
    populateMemberDropdowns();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="10" class="empty-state" style="color: var(--danger);">Lỗi tải độc giả: ${err.message}</td></tr>`;
  }
}

function renderMembers(list) {
  const tbody = document.getElementById('membersBody');
  if (!list || list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="10" class="empty-state">Chưa có người dùng nào trong cơ sở dữ liệu.</td></tr>';
    return;
  }

  tbody.innerHTML = list.map(m => {
    let roleBadge = '';
    if (m.personType === 'STUDENT') roleBadge = '<span class="badge badge-info">Sinh viên</span>';
    else if (m.personType === 'FACULTY') roleBadge = '<span class="badge badge-primary">Giảng viên</span>';
    else if (m.personType === 'GUEST') roleBadge = '<span class="badge badge-secondary">Khách ngoài</span>';
    else roleBadge = '<span class="badge badge-warning">Tác giả ngoài</span>';

    const eligibleBadge = m.isEligibleToBorrow
      ? '<span class="badge badge-success">Đủ điều kiện</span>'
      : `<span class="badge badge-danger" title="${m.ineligibilityReason || ''}">Không đủ điều kiện</span>`;

    const overdueAlert = m.hasOverdue
      ? '<span style="color: var(--danger); font-weight: 700;">Có sách quá hạn!</span>'
      : 'Không';

    const fineAlert = m.hasUnpaidFines
      ? '<span style="color: var(--danger); font-weight: 700;">Đang nợ phạt!</span>'
      : 'Không';

    return `
      <tr>
        <td><strong>#${m.personId}</strong></td>
        <td><strong>${m.name}</strong><br><small style="color: var(--text-muted);">${m.email || '-'}</small></td>
        <td><code>${m.ssn}</code></td>
        <td>${roleBadge}</td>
        <td>${m.memberId || '-'}</td>
        <td>${m.department || '-'}</td>
        <td>${m.major || m.academicRank || '-'}</td>
        <td><strong>${m.activeBorrowsCount || 0}</strong> cuốn</td>
        <td>${fineAlert}</td>
        <td>${eligibleBadge}</td>
      </tr>
    `;
  }).join('');
}

function populateMemberDropdowns() {
  const borrowSelect = document.getElementById('borrowMemberSelect');
  const accessSelect = document.getElementById('accessPersonSelect');

  if (borrowSelect) {
    borrowSelect.innerHTML = '<option value="">-- Chọn độc giả mượn sách --</option>';
    state.members.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m.personId;
      const statusNote = m.isEligibleToBorrow ? " [Đủ điều kiện]" : " [Không đủ điều kiện]";
      opt.textContent = `[ID: ${m.personId}] ${m.name} (${m.personType})${statusNote}`;
      borrowSelect.appendChild(opt);
    });
  }

  if (accessSelect) {
    accessSelect.innerHTML = '<option value="">-- Chọn độc giả truy cập --</option>';
    state.members.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m.personId;
      opt.textContent = `[ID: ${m.personId}] ${m.name} (${m.personType})`;
      accessSelect.appendChild(opt);
    });
  }

  const grantSelect = document.getElementById('grantPersonSelect');
  if (grantSelect) {
    grantSelect.innerHTML = '<option value="">-- Chọn người dùng cần cấp --</option>';
    state.members.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m.personId;
      opt.textContent = `[ID: ${m.personId}] ${m.name} (${m.personType})`;
      grantSelect.appendChild(opt);
    });
  }
}

// =============================================================================
// LIBRARIAN MANUAL LICENSE GRANT & REVOKE
// =============================================================================
async function loadLicenses() {
  try {
    const res = await fetch(`${API_BASE}/api/licenses`);
    if (!res.ok) throw new Error("HTTP " + res.status);
    const list = await res.json();
    const select = document.getElementById('grantLicenseSelect');
    if (select) {
      select.innerHTML = '<option value="">-- Chọn gói bản quyền --</option>';
      list.forEach(lp => {
        const opt = document.createElement('option');
        opt.value = lp.licenseId;
        opt.textContent = `[ID: ${lp.licenseId}] ${lp.licenseName} (${lp.provider || '-'})`;
        select.appendChild(opt);
      });
    }
  } catch (err) {
    console.error("Lỗi tải danh sách gói License:", err);
  }
}

async function loadGrantedLicenses() {
  const tbody = document.getElementById('grantedLicensesBody');
  if (!tbody) return;
  tbody.innerHTML = '<tr><td colspan="6" class="empty-state">Đang tải danh sách cấp quyền...</td></tr>';
  try {
    const res = await fetch(`${API_BASE}/api/granted-licenses`);
    if (!res.ok) throw new Error("HTTP " + res.status);
    const list = await res.json();
    if (!list || list.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="empty-state">Chưa có bản ghi cấp quyền License nào.</td></tr>';
      return;
    }
    tbody.innerHTML = list.map(g => `
      <tr>
        <td><strong>#${g.grantId}</strong></td>
        <td><strong>${g.personName}</strong> (ID: ${g.personId})</td>
        <td><span class="badge badge-secondary">${g.personType}</span></td>
        <td><strong>${g.licenseName}</strong> (ID: ${g.licenseId})</td>
        <td>${g.provider || '-'}</td>
        <td><button class="btn btn-danger btn-sm" onclick="revokeGrantedLicense(${g.grantId})">🗑️ Thu hồi</button></td>
      </tr>
    `).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="empty-state" style="color: var(--danger);">Lỗi tải danh sách License: ${err.message}</td></tr>`;
  }
}

async function handleGrantLicenseSubmit(e) {
  e.preventDefault();
  const personId = parseInt(document.getElementById('grantPersonSelect').value);
  const licenseId = parseInt(document.getElementById('grantLicenseSelect').value);
  if (!personId || !licenseId) {
    showAlert('danger', 'Vui lòng chọn cả người dùng và gói License.');
    return;
  }
  const btn = document.getElementById('btnGrantLicense');
  btn.disabled = true;
  btn.innerText = 'Đang cấp...';
  try {
    const res = await fetch(`${API_BASE}/api/granted-licenses`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ personId, licenseId })
    });
    const data = await res.json();
    if (res.ok && data.success) {
      showAlert('success', `<strong>Cấp License thành công!</strong> Grant ID #${data.grantId} (Person #${personId}, License #${licenseId}).`);
      loadGrantedLicenses();
    } else {
      showAlert('danger', `<strong>Không thể cấp License [${data.errorCode || 'ERROR'}]:</strong> ${data.message}`);
    }
  } catch (err) {
    showAlert('danger', `Lỗi mạng khi cấp License: ${err.message}`);
  } finally {
    btn.disabled = false;
    btn.innerText = '➕ Cấp License (MANUAL)';
  }
}

async function revokeGrantedLicense(grantId) {
  try {
    const res = await fetch(`${API_BASE}/api/granted-licenses/${grantId}`, { method: 'DELETE' });
    const data = await res.json();
    if (res.ok && data.success) {
      showAlert('success', `<strong>Đã thu hồi License!</strong> ${data.message}`);
      loadGrantedLicenses();
    } else {
      showAlert('danger', `<strong>Lỗi thu hồi [${data.errorCode || 'ERROR'}]:</strong> ${data.message}`);
    }
  } catch (err) {
    showAlert('danger', `Lỗi mạng khi thu hồi License: ${err.message}`);
  }
}

// Utility: HTML Escape
function escapeHtml(text) {
  if (!text) return "";
  return text.replace(/'/g, "\\'").replace(/"/g, '&quot;');
}

// Initialize Application
function initApp() {
  console.log("[LibraryApp] Initializing with API_BASE:", API_BASE);
  // Set default access date to today
  const today = new Date().toISOString().split('T')[0];
  const accessDateInput = document.getElementById('accessDateInput');
  if (accessDateInput) accessDateInput.value = today;

  // Run initial checks & loads
  checkHealth();
  loadResources();
  loadMembers();
  loadCopies();
  loadBorrows();
  loadFines();
  loadAccessLogs();
  loadLicenses();
  loadGrantedLicenses();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initApp);
} else {
  // DOM already loaded
  initApp();
}
