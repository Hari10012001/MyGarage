/**
 * MyGarage — Custom JavaScript
 * APPJFS19 - Final Year Java Full Stack Project
 */

// ============================================================
// SIDEBAR TOGGLE (Mobile)
// ============================================================
function toggleSidebar() {
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebarOverlay");
    if (sidebar) sidebar.classList.toggle("open");
    if (overlay) overlay.classList.toggle("show");
}

function closeSidebar() {
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebarOverlay");
    if (sidebar) sidebar.classList.remove("open");
    if (overlay) overlay.classList.remove("show");
}

// ============================================================
// PASSWORD SHOW/HIDE TOGGLE
// ============================================================
function togglePassword(inputId, btn) {
    const input = document.getElementById(inputId);
    const icon = btn.querySelector("i");
    if (!input) return;
    if (input.type === "password") {
        input.type = "text";
        if (icon) { icon.classList.remove("bi-eye"); icon.classList.add("bi-eye-slash"); }
    } else {
        input.type = "password";
        if (icon) { icon.classList.remove("bi-eye-slash"); icon.classList.add("bi-eye"); }
    }
}

// ============================================================
// FUEL TOTAL COST AUTO-CALCULATION
// ============================================================
function calcTotal() {
    const qty = parseFloat(document.getElementById("quantity")?.value) || 0;
    const cpl = parseFloat(document.getElementById("costPerLitre")?.value) || 0;
    const totalEl = document.getElementById("totalDisplay");
    if (totalEl) {
        const total = (qty * cpl).toFixed(2);
        totalEl.value = total > 0 ? "₹" + total : "";
    }
}

// ============================================================
// DELETE MODAL — sets form action and vehicle name
// ============================================================
function confirmDelete(btn) {
    const action = btn.getAttribute("data-action");
    const name = btn.getAttribute("data-name");
    const form = document.getElementById("deleteForm");
    const nameEl = document.getElementById("deleteVehicleName");
    if (form) form.action = action;
    if (nameEl) nameEl.textContent = name || "this vehicle";
}

// ============================================================
// ADMIN EDIT CATEGORY MODAL
// ============================================================
function openEditModal(id, name, icon) {
    const form = document.getElementById("editCatForm");
    const nameInput = document.getElementById("editName");
    const iconInput = document.getElementById("editIcon");
    if (form) form.action = "/admin/categories/" + id + "/edit";
    if (nameInput) nameInput.value = name || "";
    if (iconInput) iconInput.value = icon || "🚗";
}

// ============================================================
// AUTO-DISMISS SUCCESS ALERTS after 4 seconds
// ============================================================
document.addEventListener("DOMContentLoaded", function () {
    const successAlerts = document.querySelectorAll(".alert-success");
    successAlerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 4000);
    });

    // Initialize fuel total on page load (for edit form)
    calcTotal();
});

// ============================================================
// PLATE NUMBER AUTO-UPPERCASE
// ============================================================
document.addEventListener("DOMContentLoaded", function () {
    const plateInput = document.querySelector("input.text-uppercase");
    if (plateInput) {
        plateInput.addEventListener("input", function () {
            this.value = this.value.toUpperCase();
        });
    }
});
