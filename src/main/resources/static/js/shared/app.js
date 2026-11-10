(() => {
    const closeHeaderMenus = (except = null, restoreFocus = false) => {
        const activeElement = document.activeElement;
        let returnTo = null;
        document.querySelectorAll(".crms-dropdown.open, .user-dropdown-container.open").forEach((menu) => {
            if (menu === except) return;
            if (restoreFocus && menu.contains(activeElement)) returnTo = menu.querySelector(".crms-dropdown-toggle");
            menu.classList.remove("open");
            menu.querySelector(".crms-dropdown-toggle")?.setAttribute("aria-expanded", "false");
            menu.querySelector(".dropdown-chevron")?.classList.remove("rotate-180");
        });
        returnTo?.focus();
    };

    const setSidebarState = (collapsed) => {
        document.body.classList.toggle("sidebar-collapsed", collapsed);
        const button = document.querySelector("[data-admin-sidebar-toggle]");
        if (button) {
            button.setAttribute("aria-expanded", String(!collapsed));
            button.setAttribute("title", collapsed ? "Mở rộng menu" : "Thu gọn menu (mở rộng bảng)");
        }
        document.querySelector(".admin-toggle-icon.icon-collapse")?.classList.toggle("hidden", collapsed);
        document.querySelector(".admin-toggle-icon.icon-expand")?.classList.toggle("hidden", !collapsed);
        try { localStorage.setItem("crms_admin_sidebar_collapsed", String(collapsed)); } catch (_) { /* Storage may be disabled. */ }
    };

    const isMobileAdmin = () => window.matchMedia("(max-width: 767.98px)").matches;
    const setMobileSidebar = (open) => {
        document.body.classList.toggle("sidebar-mobile-open", open);
        document.querySelector("[data-admin-sidebar-mobile-toggle]")?.setAttribute("aria-expanded", String(open));
    };

    const respectMotionPreference = () => {
        if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
        document.querySelectorAll(".carousel").forEach((element) => {
            const carousel = window.bootstrap?.Carousel?.getOrCreateInstance(element, { ride: false, interval: false });
            carousel?.pause();
        });
    };

    const initialize = () => {
        respectMotionPreference();
        try {
            if (localStorage.getItem("crms_admin_sidebar_collapsed") === "true") {
                document.body.classList.add("sidebar-collapsed");
                setSidebarState(true);
            }
        } catch (_) { /* Storage may be disabled. */ }

        document.querySelectorAll(".crms-dropdown-toggle").forEach((button) => {
            const menu = button.closest(".crms-dropdown, .user-dropdown-container");
            if (!menu) return;
            button.setAttribute("aria-expanded", "false");
            button.addEventListener("click", () => {
                const open = !menu.classList.contains("open");
                closeHeaderMenus(menu);
                menu.classList.toggle("open", open);
                button.setAttribute("aria-expanded", String(open));
                menu.querySelector(".dropdown-chevron")?.classList.toggle("rotate-180", open);
            });
        });

        document.querySelectorAll("[data-admin-sidebar-toggle]").forEach((button) => {
            button.addEventListener("click", () => {
                if (isMobileAdmin()) setMobileSidebar(false);
                else setSidebarState(!document.body.classList.contains("sidebar-collapsed"));
            });
            button.setAttribute("aria-controls", "admin-sidebar-content");
            button.setAttribute("aria-expanded", String(!document.body.classList.contains("sidebar-collapsed")));
        });

        document.querySelectorAll("[data-admin-sidebar-mobile-toggle]").forEach((button) => {
            button.addEventListener("click", () => setMobileSidebar(!document.body.classList.contains("sidebar-mobile-open")));
        });
        window.matchMedia("(max-width: 767.98px)").addEventListener?.("change", (event) => {
            if (!event.matches) setMobileSidebar(false);
        });

        document.querySelectorAll("[data-admin-profile-toggle]").forEach((button) => {
            const menu = document.getElementById("adminProfileMenu");
            if (!menu) return;
            button.setAttribute("aria-controls", "adminProfileMenu");
            button.setAttribute("aria-expanded", "false");
            button.addEventListener("click", () => {
                const open = menu.classList.contains("hidden");
                menu.classList.toggle("hidden", !open);
                button.setAttribute("aria-expanded", String(open));
            });
        });

        document.addEventListener("click", (event) => {
            if (!event.target.closest(".crms-dropdown, .user-dropdown-container")) closeHeaderMenus();
            if (document.body.classList.contains("sidebar-mobile-open")
                && !event.target.closest(".admin-sidebar-column")
                && !event.target.closest("[data-admin-sidebar-mobile-toggle]")) {
                setMobileSidebar(false);
            }
            const wrap = event.target.closest(".admin-sidebar-profile-wrap");
            const menu = document.getElementById("adminProfileMenu");
            const button = document.getElementById("adminProfileDropdownToggle");
            if (!wrap && menu && !menu.classList.contains("hidden")) {
                menu.classList.add("hidden");
                button?.setAttribute("aria-expanded", "false");
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key !== "Escape") return;
            closeHeaderMenus(null, true);
            if (document.body.classList.contains("sidebar-mobile-open")) setMobileSidebar(false);
            const menu = document.getElementById("adminProfileMenu");
            if (menu && !menu.classList.contains("hidden")) {
                const returnFocus = menu.contains(document.activeElement);
                menu.classList.add("hidden");
                const button = document.getElementById("adminProfileDropdownToggle");
                button?.setAttribute("aria-expanded", "false");
                if (returnFocus) button?.focus();
            }
        });

        const path = window.location.pathname.replace(/\/$/, "") || "/";
        document.querySelectorAll(".header-nav-link[href]").forEach((link) => {
            const href = link.getAttribute("href")?.split("?")[0].replace(/\/$/, "") || "/";
            const active = href === path || (href !== "/" && path.startsWith(href));
            link.classList.toggle("active", active);
            if (active) link.setAttribute("aria-current", "page");
            else link.removeAttribute("aria-current");
        });
    };

    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", initialize, { once: true });
    else initialize();
})();
