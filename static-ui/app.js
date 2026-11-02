const STORAGE_KEY = "coworking-static-ui-state";

const seedState = {
    role: "ADMIN",
    activeView: "bookings",
    workspaces: [
        { id: 1, name: "Focus Desk A1", type: "DESK", capacity: 1, hourlyRate: 120, active: true },
        { id: 2, name: "Meeting Room Blue", type: "ROOM", capacity: 6, hourlyRate: 450, active: true },
        { id: 3, name: "Podcast Studio", type: "STUDIO", capacity: 3, hourlyRate: 700, active: true }
    ],
    bookings: [
        {
            id: 1,
            workspaceId: 1,
            customerName: "Demo User",
            customerEmail: "demo@coworking.local",
            startsAt: "2026-09-18T10:00",
            endsAt: "2026-09-18T12:00",
            status: "CONFIRMED",
            totalPrice: 240
        }
    ]
};

let state = loadState();
let editing = null;

const elements = {
    roleSelect: document.querySelector("#roleSelect"),
    roleHint: document.querySelector("#roleHint"),
    searchInput: document.querySelector("#searchInput"),
    pageTitle: document.querySelector("#pageTitle"),
    primaryActionBtn: document.querySelector("#primaryActionBtn"),
    resetDemoBtn: document.querySelector("#resetDemoBtn"),
    notice: document.querySelector("#notice"),
    bookingsView: document.querySelector("#bookingsView"),
    workspacesView: document.querySelector("#workspacesView"),
    adminView: document.querySelector("#adminView"),
    bookingsTable: document.querySelector("#bookingsTable"),
    workspacesTable: document.querySelector("#workspacesTable"),
    bookingCount: document.querySelector("#bookingCount"),
    workspaceCount: document.querySelector("#workspaceCount"),
    statWorkspaces: document.querySelector("#statWorkspaces"),
    statBookings: document.querySelector("#statBookings"),
    statRevenue: document.querySelector("#statRevenue"),
    recordDialog: document.querySelector("#recordDialog"),
    recordForm: document.querySelector("#recordForm"),
    dialogTitle: document.querySelector("#dialogTitle"),
    formFields: document.querySelector("#formFields"),
    closeDialogBtn: document.querySelector("#closeDialogBtn"),
    cancelDialogBtn: document.querySelector("#cancelDialogBtn"),
    checkAdminBtn: document.querySelector("#checkAdminBtn"),
    checkDeleteBtn: document.querySelector("#checkDeleteBtn"),
    securityResult: document.querySelector("#securityResult")
};

document.querySelectorAll("[data-view-link]").forEach((button) => {
    button.addEventListener("click", () => setView(button.dataset.viewLink));
});

elements.roleSelect.addEventListener("change", () => {
    state.role = elements.roleSelect.value;
    saveState();
    render();
});

elements.searchInput.addEventListener("input", render);
elements.primaryActionBtn.addEventListener("click", () => {
    if (state.activeView === "bookings") {
        if (!canCreateBooking()) return showNotice("USER can create bookings, but this action still requires login in the real app.");
        openBookingDialog();
    }
    if (state.activeView === "workspaces") {
        if (!canManageWorkspaces()) return showNotice("Only ADMIN or MANAGER can create workspaces.", true);
        openWorkspaceDialog();
    }
});

elements.resetDemoBtn.addEventListener("click", () => {
    state = structuredClone(seedState);
    elements.searchInput.value = "";
    saveState();
    render();
    showNotice("Demo data has been reset.");
});

elements.closeDialogBtn.addEventListener("click", () => elements.recordDialog.close());
elements.cancelDialogBtn.addEventListener("click", () => elements.recordDialog.close());
elements.recordForm.addEventListener("submit", (event) => {
    event.preventDefault();
    if (editing?.type === "booking") saveBooking();
    if (editing?.type === "workspace") saveWorkspace();
});

elements.checkAdminBtn.addEventListener("click", () => {
    elements.securityResult.textContent = state.role === "ADMIN"
        ? "200 OK: ADMIN can open /admin and /api/admin/status."
        : "403 Forbidden: SecurityFilterChain blocks this role from admin resources.";
});

elements.checkDeleteBtn.addEventListener("click", () => {
    elements.securityResult.textContent = state.role === "ADMIN"
        ? "204 No Content: @PreAuthorize allows delete for ADMIN."
        : "403 Forbidden: @PreAuthorize blocks delete without ADMIN role.";
});

render();

function loadState() {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return structuredClone(seedState);
    try {
        return JSON.parse(raw);
    } catch {
        return structuredClone(seedState);
    }
}

function saveState() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function setView(view) {
    state.activeView = view;
    elements.searchInput.value = "";
    saveState();
    render();
}

function render() {
    elements.roleSelect.value = state.role;
    elements.roleHint.textContent = roleHint(state.role);
    document.querySelectorAll(".nav-item").forEach((button) => {
        button.classList.toggle("active", button.dataset.viewLink === state.activeView);
    });

    elements.bookingsView.classList.toggle("hidden", state.activeView !== "bookings");
    elements.workspacesView.classList.toggle("hidden", state.activeView !== "workspaces");
    elements.adminView.classList.toggle("hidden", state.activeView !== "admin");

    elements.pageTitle.textContent = titleForView(state.activeView);
    elements.primaryActionBtn.hidden = state.activeView === "admin";
    elements.primaryActionBtn.textContent = state.activeView === "workspaces" ? "New workspace" : "New booking";

    renderStats();
    renderBookings();
    renderWorkspaces();
}

function roleHint(role) {
    if (role === "ADMIN") return "Full access";
    if (role === "MANAGER") return "Create and update access";
    return "Read data and create bookings";
}

function titleForView(view) {
    if (view === "workspaces") return "Workspaces";
    if (view === "admin") return "Admin";
    return "Bookings";
}

function renderStats() {
    const activeWorkspaces = state.workspaces.filter((workspace) => workspace.active).length;
    const confirmedBookings = state.bookings.filter((booking) => booking.status === "CONFIRMED").length;
    const revenue = state.bookings.reduce((sum, booking) => sum + Number(booking.totalPrice || 0), 0);

    elements.statWorkspaces.textContent = String(activeWorkspaces);
    elements.statBookings.textContent = String(confirmedBookings);
    elements.statRevenue.textContent = `${revenue.toFixed(2)}`;
}

function renderBookings() {
    const search = elements.searchInput.value.trim().toLowerCase();
    const bookings = state.bookings.filter((booking) => {
        const workspace = findWorkspace(booking.workspaceId);
        return [
            booking.id,
            workspace?.name,
            booking.customerName,
            booking.customerEmail,
            booking.status
        ].join(" ").toLowerCase().includes(search);
    });

    elements.bookingCount.textContent = recordCount(bookings.length);
    elements.bookingsTable.innerHTML = bookings.length
        ? bookings.map((booking) => bookingRow(booking)).join("")
        : `<tr><td colspan="7" class="empty-state">No bookings found</td></tr>`;

    elements.bookingsTable.querySelectorAll("[data-edit-booking]").forEach((button) => {
        button.addEventListener("click", () => openBookingDialog(Number(button.dataset.editBooking)));
    });
    elements.bookingsTable.querySelectorAll("[data-delete-booking]").forEach((button) => {
        button.addEventListener("click", () => deleteBooking(Number(button.dataset.deleteBooking)));
    });
}

function renderWorkspaces() {
    const search = elements.searchInput.value.trim().toLowerCase();
    const workspaces = state.workspaces.filter((workspace) => [
        workspace.id,
        workspace.name,
        workspace.type,
        workspace.capacity,
        workspace.hourlyRate,
        workspace.active ? "active" : "inactive"
    ].join(" ").toLowerCase().includes(search));

    elements.workspaceCount.textContent = recordCount(workspaces.length);
    elements.workspacesTable.innerHTML = workspaces.length
        ? workspaces.map((workspace) => workspaceRow(workspace)).join("")
        : `<tr><td colspan="7" class="empty-state">No workspaces found</td></tr>`;

    elements.workspacesTable.querySelectorAll("[data-edit-workspace]").forEach((button) => {
        button.addEventListener("click", () => openWorkspaceDialog(Number(button.dataset.editWorkspace)));
    });
    elements.workspacesTable.querySelectorAll("[data-delete-workspace]").forEach((button) => {
        button.addEventListener("click", () => deleteWorkspace(Number(button.dataset.deleteWorkspace)));
    });
}

function bookingRow(booking) {
    const workspace = findWorkspace(booking.workspaceId);
    const canEdit = canManageBookings();
    const canDelete = canDeleteRecords();
    return `
        <tr>
            <td>${booking.id}</td>
            <td>${escapeHtml(workspace?.name || "Unknown")}</td>
            <td>
                <strong>${escapeHtml(booking.customerName)}</strong>
                <span class="cell-muted">${escapeHtml(booking.customerEmail)}</span>
            </td>
            <td>
                ${formatDateTime(booking.startsAt)}
                <span class="cell-muted">${formatDateTime(booking.endsAt)}</span>
            </td>
            <td><span class="badge ${booking.status === "CANCELLED" ? "cancelled" : ""}">${booking.status}</span></td>
            <td>${Number(booking.totalPrice).toFixed(2)}</td>
            <td class="actions">
                <span class="action-row">
                    <button class="link-button" type="button" data-edit-booking="${booking.id}" ${canEdit ? "" : "disabled"}>Edit</button>
                    <button class="link-button danger" type="button" data-delete-booking="${booking.id}" ${canDelete ? "" : "disabled"}>Delete</button>
                </span>
            </td>
        </tr>
    `;
}

function workspaceRow(workspace) {
    const canEdit = canManageWorkspaces();
    const canDelete = canDeleteRecords();
    return `
        <tr>
            <td>${workspace.id}</td>
            <td>${escapeHtml(workspace.name)}</td>
            <td>${escapeHtml(workspace.type)}</td>
            <td>${workspace.capacity}</td>
            <td>${Number(workspace.hourlyRate).toFixed(2)}</td>
            <td><span class="badge ${workspace.active ? "" : "cancelled"}">${workspace.active ? "ACTIVE" : "INACTIVE"}</span></td>
            <td class="actions">
                <span class="action-row">
                    <button class="link-button" type="button" data-edit-workspace="${workspace.id}" ${canEdit ? "" : "disabled"}>Edit</button>
                    <button class="link-button danger" type="button" data-delete-workspace="${workspace.id}" ${canDelete ? "" : "disabled"}>Delete</button>
                </span>
            </td>
        </tr>
    `;
}

function openBookingDialog(id) {
    const booking = id
        ? state.bookings.find((item) => item.id === id)
        : {
            id: nextId(state.bookings),
            workspaceId: state.workspaces[0]?.id || 1,
            customerName: "",
            customerEmail: "",
            startsAt: "2026-09-19T09:00",
            endsAt: "2026-09-19T11:00",
            status: "CONFIRMED",
            totalPrice: 0
        };

    editing = { type: "booking", id: id || null };
    elements.dialogTitle.textContent = id ? "Edit booking" : "New booking";
    elements.formFields.innerHTML = `
        <label class="full">
            Workspace
            <select name="workspaceId" required>
                ${state.workspaces.map((workspace) => `
                    <option value="${workspace.id}" ${workspace.id === booking.workspaceId ? "selected" : ""}>
                        ${escapeHtml(workspace.name)} / ${escapeHtml(workspace.type)} / ${Number(workspace.hourlyRate).toFixed(2)}
                    </option>
                `).join("")}
            </select>
        </label>
        <label>
            Customer name
            <input name="customerName" required value="${escapeAttribute(booking.customerName)}">
        </label>
        <label>
            Customer email
            <input name="customerEmail" type="email" required value="${escapeAttribute(booking.customerEmail)}">
        </label>
        <label>
            Starts at
            <input name="startsAt" type="datetime-local" required value="${booking.startsAt}">
        </label>
        <label>
            Ends at
            <input name="endsAt" type="datetime-local" required value="${booking.endsAt}">
        </label>
        <label class="full">
            Status
            <select name="status">
                <option value="CONFIRMED" ${booking.status === "CONFIRMED" ? "selected" : ""}>CONFIRMED</option>
                <option value="CANCELLED" ${booking.status === "CANCELLED" ? "selected" : ""}>CANCELLED</option>
            </select>
        </label>
    `;
    elements.recordDialog.showModal();
}

function openWorkspaceDialog(id) {
    const workspace = id
        ? state.workspaces.find((item) => item.id === id)
        : { id: nextId(state.workspaces), name: "", type: "DESK", capacity: 1, hourlyRate: 100, active: true };

    editing = { type: "workspace", id: id || null };
    elements.dialogTitle.textContent = id ? "Edit workspace" : "New workspace";
    elements.formFields.innerHTML = `
        <label class="full">
            Name
            <input name="name" required value="${escapeAttribute(workspace.name)}">
        </label>
        <label>
            Type
            <select name="type">
                ${["DESK", "ROOM", "STUDIO", "PRIVATE_OFFICE"].map((type) => `
                    <option value="${type}" ${workspace.type === type ? "selected" : ""}>${type}</option>
                `).join("")}
            </select>
        </label>
        <label>
            Capacity
            <input name="capacity" type="number" min="1" required value="${workspace.capacity}">
        </label>
        <label>
            Hourly rate
            <input name="hourlyRate" type="number" min="0.01" step="0.01" required value="${workspace.hourlyRate}">
        </label>
        <label>
            Status
            <select name="active">
                <option value="true" ${workspace.active ? "selected" : ""}>ACTIVE</option>
                <option value="false" ${!workspace.active ? "selected" : ""}>INACTIVE</option>
            </select>
        </label>
    `;
    elements.recordDialog.showModal();
}

function saveBooking() {
    const form = new FormData(elements.recordForm);
    const booking = {
        id: editing.id || nextId(state.bookings),
        workspaceId: Number(form.get("workspaceId")),
        customerName: String(form.get("customerName")).trim(),
        customerEmail: String(form.get("customerEmail")).trim(),
        startsAt: String(form.get("startsAt")),
        endsAt: String(form.get("endsAt")),
        status: String(form.get("status")),
        totalPrice: 0
    };

    if (new Date(booking.endsAt) <= new Date(booking.startsAt)) {
        showNotice("End time must be after start time.", true);
        return;
    }

    booking.totalPrice = calculateTotal(booking);

    if (editing.id) {
        state.bookings = state.bookings.map((item) => item.id === editing.id ? booking : item);
        showNotice("Booking updated.");
    } else {
        state.bookings.push(booking);
        showNotice("Booking created.");
    }
    elements.recordDialog.close();
    saveState();
    render();
}

function saveWorkspace() {
    const form = new FormData(elements.recordForm);
    const workspace = {
        id: editing.id || nextId(state.workspaces),
        name: String(form.get("name")).trim(),
        type: String(form.get("type")),
        capacity: Number(form.get("capacity")),
        hourlyRate: Number(form.get("hourlyRate")),
        active: form.get("active") === "true"
    };

    if (editing.id) {
        state.workspaces = state.workspaces.map((item) => item.id === editing.id ? workspace : item);
        showNotice("Workspace updated.");
    } else {
        state.workspaces.push(workspace);
        showNotice("Workspace created.");
    }
    elements.recordDialog.close();
    saveState();
    render();
}

function deleteBooking(id) {
    if (!canDeleteRecords()) {
        showNotice("403 Forbidden: only ADMIN can delete bookings.", true);
        return;
    }
    state.bookings = state.bookings.filter((booking) => booking.id !== id);
    saveState();
    render();
    showNotice("Booking deleted.");
}

function deleteWorkspace(id) {
    if (!canDeleteRecords()) {
        showNotice("403 Forbidden: only ADMIN can delete workspaces.", true);
        return;
    }
    if (state.bookings.some((booking) => booking.workspaceId === id)) {
        showNotice("Workspace has bookings and cannot be deleted.", true);
        return;
    }
    state.workspaces = state.workspaces.filter((workspace) => workspace.id !== id);
    saveState();
    render();
    showNotice("Workspace deleted.");
}

function canCreateBooking() {
    return ["ADMIN", "MANAGER", "USER"].includes(state.role);
}

function canManageBookings() {
    return ["ADMIN", "MANAGER"].includes(state.role);
}

function canManageWorkspaces() {
    return ["ADMIN", "MANAGER"].includes(state.role);
}

function canDeleteRecords() {
    return state.role === "ADMIN";
}

function calculateTotal(booking) {
    const workspace = findWorkspace(booking.workspaceId);
    const minutes = (new Date(booking.endsAt) - new Date(booking.startsAt)) / 60000;
    return Math.round((workspace.hourlyRate * minutes / 60) * 100) / 100;
}

function findWorkspace(id) {
    return state.workspaces.find((workspace) => workspace.id === Number(id));
}

function nextId(items) {
    return items.length ? Math.max(...items.map((item) => item.id)) + 1 : 1;
}

function recordCount(count) {
    return `${count} ${count === 1 ? "record" : "records"}`;
}

function formatDateTime(value) {
    const date = new Date(value);
    return date.toLocaleString("uk-UA", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function showNotice(message, isError = false) {
    elements.notice.textContent = message;
    elements.notice.classList.toggle("error", isError);
    elements.notice.hidden = false;
    window.clearTimeout(showNotice.timeoutId);
    showNotice.timeoutId = window.setTimeout(() => {
        elements.notice.hidden = true;
    }, 3600);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function escapeAttribute(value) {
    return escapeHtml(value);
}
