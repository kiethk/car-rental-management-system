/*
 * Date and time pickers of the rental search form (fragments/rental-search-form.html).
 * Reads data-open-hour / data-close-hour / data-server-error from the form and keeps the hidden
 * startDate/startTime/endDate/endTime inputs in step with what the customer picks.
 */
(function () {
    let instanceNumber = 0;

    function initRentalSchedule(form) {
        const get = (role) => form.querySelector(`[data-rental-role="${role}"]`);
        const instanceId = `rental-search-${++instanceNumber}`;
        const startDateInput = get("rental-start-date");
        const startTimeInput = get("rental-start-time");
        const endDateInput = get("rental-end-date");
        const endTimeInput = get("rental-end-time");
        const itineraryText = get("rental-itinerary");

        if (!startDateInput || !startTimeInput || !endDateInput || !endTimeInput) return;

        const pickupDateTrigger = get("pickup-date-trigger");
        const pickupDateDisplay = get("pickup-date-display");
        const pickupDatePopover = get("pickup-date-popover");
        const pickupCalPrev = get("pickup-cal-prev");
        const pickupCalNext = get("pickup-cal-next");
        const pickupCalMonthYear = get("pickup-cal-month-year");
        const pickupCalGrid = get("pickup-cal-grid");

        const pickupHourTrigger = get("pickup-hour-trigger");
        const pickupHourDisplay = get("pickup-hour-display");
        const pickupHourPopover = get("pickup-hour-popover");

        const returnDateTrigger = get("return-date-trigger");
        const returnDateDisplay = get("return-date-display");
        const returnDatePopover = get("return-date-popover");
        const returnCalPrev = get("return-cal-prev");
        const returnCalNext = get("return-cal-next");
        const returnCalMonthYear = get("return-cal-month-year");
        const returnCalGrid = get("return-cal-grid");

        const returnHourTrigger = get("return-hour-trigger");
        const returnHourDisplay = get("return-hour-display");
        const returnHourPopover = get("return-hour-popover");

        const monthNames = [
            "Tháng 1",
            "Tháng 2",
            "Tháng 3",
            "Tháng 4",
            "Tháng 5",
            "Tháng 6",
            "Tháng 7",
            "Tháng 8",
            "Tháng 9",
            "Tháng 10",
            "Tháng 11",
            "Tháng 12",
        ];
        const dayNames = ["CN", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7"];

        // Depot opening hours from crms.opening-hours.* (pickup and return both inside, whole hours).
        const hoursHost = form;
        const OPEN_HOUR = parseInt((hoursHost && hoursHost.getAttribute("data-open-hour")) || "0", 10);
        const CLOSE_HOUR = parseInt((hoursHost && hoursHost.getAttribute("data-close-hour")) || "23", 10);
        const hoursList = [];
        for (let h = OPEN_HOUR; h <= CLOSE_HOUR; h++) hoursList.push(String(h).padStart(2, "0") + ":00");

        function toLocalDateString(d) {
            const year = d.getFullYear();
            const month = String(d.getMonth() + 1).padStart(2, "0");
            const day = String(d.getDate()).padStart(2, "0");
            return year + "-" + month + "-" + day;
        }

        function getMinAllowedPickupDateStr() {
            const now = new Date();
            if (now.getHours() >= CLOSE_HOUR) {
                const tmr = new Date(now);
                tmr.setDate(tmr.getDate() + 1);
                return toLocalDateString(tmr);
            }
            return toLocalDateString(now);
        }

        function getMinAllowedPickupHour() {
            const now = new Date();
            return now.getHours() + 1;
        }

        function parseDate(str) {
            if (!str) return new Date();
            const parts = str.split("-");
            return new Date(parseInt(parts[0], 10), parseInt(parts[1], 10) - 1, parseInt(parts[2], 10));
        }

        // "Thứ 7, 07/11/2026": weekday first, then day/month/year as numbers.
        function formatNumericDate(d) {
            return (
                dayNames[d.getDay()] +
                ", " +
                String(d.getDate()).padStart(2, "0") +
                "/" +
                String(d.getMonth() + 1).padStart(2, "0") +
                "/" +
                d.getFullYear()
            );
        }

        function formatDisplayDate(dateStr) {
            if (!dateStr) return "";
            return formatNumericDate(parseDate(dateStr));
        }

        function formatHourDisplay(timeStr) {
            return timeStr || "";
        }

        // Calendar Navigation State
        let pickupViewDate = parseDate(startDateInput.value || toLocalDateString(new Date()));
        let returnViewDate = parseDate(endDateInput.value || toLocalDateString(new Date()));

        function closeAllPopovers() {
            const active = document.activeElement;
            let returnTo = null;
            [[pickupDatePopover, pickupDateTrigger], [pickupHourPopover, pickupHourTrigger], [returnDatePopover, returnDateTrigger], [returnHourPopover, returnHourTrigger]].forEach(([popover, trigger]) => {
                if (popover.contains(active)) returnTo = trigger;
                popover.classList.add("hidden");
                trigger.setAttribute("aria-expanded", "false");
            });
            returnTo?.focus();
        }

        [["pickup-date", pickupDatePopover, pickupDateTrigger], ["pickup-hour", pickupHourPopover, pickupHourTrigger], ["return-date", returnDatePopover, returnDateTrigger], ["return-hour", returnHourPopover, returnHourTrigger]].forEach(([name, popover, trigger]) => {
            popover.id = `${instanceId}-${name}-popover`;
            trigger.setAttribute("aria-controls", popover.id);
            trigger.setAttribute("aria-haspopup", name.endsWith("date") ? "dialog" : "listbox");
        });

        form.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && [pickupDatePopover, pickupHourPopover, returnDatePopover, returnHourPopover].some((popover) => !popover.classList.contains("hidden"))) {
                event.preventDefault();
                closeAllPopovers();
            }
        });

        // Render Hour Popovers (Dark theme)
        function renderHourOptions(popoverEl, currentVal, onSelect, type) {
            popoverEl.innerHTML = "";
            const now = new Date();
            const todayStr = toLocalDateString(now);
            const isPickup = (type === "pickup");
            hoursList.forEach((h) => {
                const item = document.createElement("button");
                item.type = "button";
                item.setAttribute("role", "option");
                item.setAttribute("aria-selected", String(h === currentVal));
                const hVal = parseInt(h.split(":")[0], 10);
                let isDisabled = false;

                if (isPickup) {
                    const sDate = startDateInput.value || todayStr;
                    if (sDate === todayStr) {
                        const minH = getMinAllowedPickupHour();
                        if (hVal < minH) {
                            isDisabled = true;
                        }
                    }
                } else {
                    const eDate = endDateInput.value || startDateInput.value || todayStr;
                    const sDate = startDateInput.value || todayStr;
                    if (eDate === sDate) {
                        const sTimeH = parseInt((startTimeInput.value || "09:00").split(":")[0], 10);
                        if (hVal <= sTimeH) {
                            isDisabled = true;
                        }
                    } else if (eDate === todayStr) {
                        const minH = getMinAllowedPickupHour();
                        if (hVal < minH) {
                            isDisabled = true;
                        }
                    }
                }

                if (isDisabled) {
                    item.className = "hour-option-dark";
                    item.disabled = true;
                    item.innerHTML = "<span>" + formatHourDisplay(h) + "</span>";
                    popoverEl.appendChild(item);
                    return;
                }
                item.className = "hour-option-dark" + (h === currentVal ? " selected" : "");
                item.innerHTML =
                    "<span>" +
                    formatHourDisplay(h) +
                    "</span>" +
                    (h === currentVal
                        ? '<svg class="w-3.5 h-3.5 text-slate-900" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>'
                        : "");
                item.addEventListener("click", (e) => {
                    e.stopPropagation();
                    onSelect(h);
                    closeAllPopovers();
                });
                popoverEl.appendChild(item);
            });
        }

        // Render 6-Week Calendar Grid (42 cells matching shadcn CalendarWithPresets)
        function renderCalendar(type) {
            const isPickup = type === "pickup";
            const viewDate = isPickup ? pickupViewDate : returnViewDate;
            const monthYearEl = isPickup ? pickupCalMonthYear : returnCalMonthYear;
            const gridEl = isPickup ? pickupCalGrid : returnCalGrid;
            const selectedDateStr = isPickup ? startDateInput.value : endDateInput.value;
            const todayStr = toLocalDateString(new Date());
            const minAllowedStr = isPickup ? getMinAllowedPickupDateStr() : (startDateInput.value || getMinAllowedPickupDateStr());

            const year = viewDate.getFullYear();
            const month = viewDate.getMonth();

            monthYearEl.textContent = monthNames[month] + " " + year;
            gridEl.innerHTML = "";

            // Day 1 of month weekday (0=Sun, 1=Mon, ..., 6=Sat)
            const firstDayIndex = new Date(year, month, 1).getDay();
            const daysInCurrentMonth = new Date(year, month + 1, 0).getDate();
            const daysInPrevMonth = new Date(year, month, 0).getDate();

            const totalCells = 42; // Exactly 6 weeks, as in shadcn example
            const cells = [];

            // 1. Preceding days from previous month
            for (let i = firstDayIndex - 1; i >= 0; i--) {
                const dayNum = daysInPrevMonth - i;
                const d = new Date(year, month - 1, dayNum);
                cells.push({ date: d, isOtherMonth: true });
            }

            // 2. Days of current month
            for (let day = 1; day <= daysInCurrentMonth; day++) {
                const d = new Date(year, month, day);
                cells.push({ date: d, isOtherMonth: false });
            }

            // 3. Trailing days of next month to fill exactly 42 cells (6 rows)
            const remaining = totalCells - cells.length;
            for (let day = 1; day <= remaining; day++) {
                const d = new Date(year, month + 1, day);
                cells.push({ date: d, isOtherMonth: true });
            }

            // Render all 42 day cells
            cells.forEach((item) => {
                const d = item.date;
                const dStr = toLocalDateString(d);
                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = "cal-dark-day-btn";
                btn.textContent = d.getDate();
                btn.setAttribute("aria-label", formatNumericDate(d));
                btn.setAttribute("aria-pressed", String(dStr === selectedDateStr));

                if (item.isOtherMonth) {
                    btn.classList.add("other-month");
                }

                if (dStr === todayStr) {
                    btn.classList.add("today");
                }

                if (dStr === selectedDateStr) {
                    btn.classList.add("selected");
                }

                if (dStr < minAllowedStr) {
                    btn.classList.add("disabled");
                    btn.disabled = true;
                } else {
                    btn.addEventListener("click", (e) => {
                        e.stopPropagation();
                        if (isPickup) {
                            startDateInput.value = dStr;
                        } else {
                            endDateInput.value = dStr;
                        }
                        // Keep view date synced
                        if (isPickup) pickupViewDate = parseDate(dStr);
                        else returnViewDate = parseDate(dStr);

                        updateAll();
                        closeAllPopovers();
                    });
                }

                gridEl.appendChild(btn);
            });

            // Update Preset buttons active state inside this calendar
            updatePresetButtonsActiveState(type, selectedDateStr);
        }

        function updatePresetButtonsActiveState(type, selectedDateStr) {
            if (type === "pickup") {
                form.querySelectorAll(".pickup-preset-btn").forEach((btn) => {
                    const addDays = parseInt(btn.getAttribute("data-days"), 10);
                    const target = new Date();
                    target.setDate(target.getDate() + addDays);
                    if (toLocalDateString(target) === selectedDateStr) {
                        btn.classList.add("active");
                    } else {
                        btn.classList.remove("active");
                    }
                });
            } else {
                const sDateStr = startDateInput.value || toLocalDateString(new Date());
                form.querySelectorAll(".return-preset-btn").forEach((btn) => {
                    const addDays = parseInt(btn.getAttribute("data-add-days"), 10);
                    const target = parseDate(sDateStr);
                    target.setDate(target.getDate() + addDays);
                    if (toLocalDateString(target) === selectedDateStr) {
                        btn.classList.add("active");
                    } else {
                        btn.classList.remove("active");
                    }
                });
            }
        }

        const scheduleForm = form;
        const scheduleErrorBox = get("rental-schedule-error");
        const scheduleSubmitBtn = get("rental-search-btn");
        const hasServerError = !!(scheduleForm && scheduleForm.getAttribute("data-server-error"));
        let isScheduleInitialized = false;

        function toDateTime(dateStr, timeStr) {
            const d = parseDate(dateStr);
            d.setHours(parseInt(timeStr.split(":")[0], 10), 0, 0, 0);
            return d;
        }

        // Runs once, only to turn the default values of a fresh page into a bookable schedule.
        // After that the customer's choices are never changed silently.
        function normalizeInitialSchedule() {
            const now = new Date();
            const minPickupDateStr = getMinAllowedPickupDateStr();

            let sDate = startDateInput.value || minPickupDateStr;
            let sTime = startTimeInput.value || "09:00";
            if (sDate < minPickupDateStr) {
                sDate = minPickupDateStr;
            }
            if (sDate === toLocalDateString(now)) {
                const minH = getMinAllowedPickupHour();
                if (parseInt(sTime.split(":")[0], 10) < minH) {
                    const nextValid = hoursList.find((h) => parseInt(h.split(":")[0], 10) >= minH);
                    if (nextValid) {
                        sTime = nextValid;
                    } else {
                        const tmr = new Date(now);
                        tmr.setDate(tmr.getDate() + 1);
                        sDate = toLocalDateString(tmr);
                        sTime = "09:00";
                    }
                }
            }

            let eDate = endDateInput.value || sDate;
            let eTime = endTimeInput.value || "18:00";
            if (eDate < sDate) {
                eDate = sDate;
            }
            if (eDate === sDate) {
                const sh = parseInt(sTime.split(":")[0], 10);
                const eh = parseInt(eTime.split(":")[0], 10);
                if (eh <= sh) {
                    if (sh + 4 <= 22) {
                        eTime = String(sh + 4).padStart(2, "0") + ":00";
                    } else {
                        const nextDay = parseDate(sDate);
                        nextDay.setDate(nextDay.getDate() + 1);
                        eDate = toLocalDateString(nextDay);
                        eTime = "09:00";
                    }
                }
            }

            startDateInput.value = sDate;
            startTimeInput.value = sTime;
            endDateInput.value = eDate;
            endTimeInput.value = eTime;
        }

        // Returns an error message, or null when the schedule can be searched.
        function validateSchedule() {
            if (!startDateInput.value || !startTimeInput.value) {
                return "Vui l\u00F2ng ch\u1ECDn th\u1EDDi gian nh\u1EADn xe.";
            }
            if (!endDateInput.value || !endTimeInput.value) {
                return "Vui l\u00F2ng ch\u1ECDn th\u1EDDi gian tr\u1EA3 xe.";
            }
            const start = toDateTime(startDateInput.value, startTimeInput.value);
            const end = toDateTime(endDateInput.value, endTimeInput.value);
            if (start < new Date()) {
                return "Th\u1EDDi gian nh\u1EADn xe kh\u00F4ng \u0111\u01B0\u1EE3c \u1EDF trong qu\u00E1 kh\u1EE9.";
            }
            if (end <= start) {
                return "Th\u1EDDi gian tr\u1EA3 xe ph\u1EA3i sau th\u1EDDi gian nh\u1EADn xe.";
            }
            return null;
        }

        function renderScheduleValidation() {
            const message = validateSchedule();
            if (scheduleErrorBox) {
                scheduleErrorBox.textContent = message || "";
                scheduleErrorBox.classList.toggle("hidden", !message);
            }
            if (scheduleSubmitBtn) {
                scheduleSubmitBtn.disabled = !!message;
                scheduleSubmitBtn.classList.toggle("opacity-50", !!message);
                scheduleSubmitBtn.classList.toggle("cursor-not-allowed", !!message);
            }
            return message;
        }

        function updateAll() {
            if (!isScheduleInitialized) {
                isScheduleInitialized = true;
                if (!hasServerError) {
                    normalizeInitialSchedule();
                }
            }

            const sDate = startDateInput.value || getMinAllowedPickupDateStr();
            const sTime = startTimeInput.value || "09:00";
            const eDate = endDateInput.value || sDate;
            const eTime = endTimeInput.value || "18:00";

            pickupDateDisplay.textContent = formatDisplayDate(sDate);
            pickupHourDisplay.textContent = formatHourDisplay(sTime);
            returnDateDisplay.textContent = formatDisplayDate(eDate);
            returnHourDisplay.textContent = formatHourDisplay(eTime);

            // Calculate duration
            const sP = sDate.split("-");
            const eP = eDate.split("-");
            const startDt = new Date(
                parseInt(sP[0], 10),
                parseInt(sP[1], 10) - 1,
                parseInt(sP[2], 10),
                parseInt(sTime.split(":")[0], 10),
                0,
            );
            const endDt = new Date(
                parseInt(eP[0], 10),
                parseInt(eP[1], 10) - 1,
                parseInt(eP[2], 10),
                parseInt(eTime.split(":")[0], 10),
                0,
            );
            let diffMs = endDt - startDt;
            if (diffMs <= 0) diffMs = 3600000;
            const totalHours = Math.max(1, Math.round(diffMs / (1000 * 60 * 60)));

            if (itineraryText) {
                const sParts = sDate.split("-");
                const eParts = eDate.split("-");
                const sD = new Date(
                    parseInt(sParts[0], 10),
                    parseInt(sParts[1], 10) - 1,
                    parseInt(sParts[2], 10),
                );
                const eD = new Date(
                    parseInt(eParts[0], 10),
                    parseInt(eParts[1], 10) - 1,
                    parseInt(eParts[2], 10),
                );
                const sTxt = formatNumericDate(sD) + " " + sTime;
                const eTxt = formatNumericDate(eD) + " " + eTime;
                itineraryText.textContent = sTxt + " → " + eTxt + " (" + totalHours + " giờ)";
            }

            // Update preset buttons state in both calendars
            updatePresetButtonsActiveState("pickup", sDate);
            updatePresetButtonsActiveState("return", eDate);
            renderScheduleValidation();
        }

        // Pickup Date Trigger
        pickupDateTrigger.addEventListener("click", function (e) {
            e.stopPropagation();
            const isOpen = !pickupDatePopover.classList.contains("hidden");
            closeAllPopovers();
            if (!isOpen) {
                pickupViewDate = parseDate(startDateInput.value || toLocalDateString(new Date()));
                renderCalendar("pickup");
                pickupDatePopover.classList.remove("hidden");
                pickupDateTrigger.setAttribute("aria-expanded", "true");
                pickupCalGrid.querySelector(".selected:not(:disabled)")?.focus();
            }
        });

        pickupCalPrev.addEventListener("click", function (e) {
            e.stopPropagation();
            pickupViewDate.setMonth(pickupViewDate.getMonth() - 1);
            renderCalendar("pickup");
        });

        pickupCalNext.addEventListener("click", function (e) {
            e.stopPropagation();
            pickupViewDate.setMonth(pickupViewDate.getMonth() + 1);
            renderCalendar("pickup");
        });

        // Pickup Hour Trigger
        pickupHourTrigger.addEventListener("click", function (e) {
            e.stopPropagation();
            const isOpen = !pickupHourPopover.classList.contains("hidden");
            closeAllPopovers();
            if (!isOpen) {
                renderHourOptions(pickupHourPopover, startTimeInput.value, (newHour) => {
                    startTimeInput.value = newHour;
                    updateAll();
                }, "pickup");
                pickupHourPopover.classList.remove("hidden");
                pickupHourTrigger.setAttribute("aria-expanded", "true");
                pickupHourPopover.querySelector("button:not(:disabled)")?.focus();
            }
        });

        // Return Date Trigger
        returnDateTrigger.addEventListener("click", function (e) {
            e.stopPropagation();
            const isOpen = !returnDatePopover.classList.contains("hidden");
            closeAllPopovers();
            if (!isOpen) {
                returnViewDate = parseDate(endDateInput.value || toLocalDateString(new Date()));
                renderCalendar("return");
                returnDatePopover.classList.remove("hidden");
                returnDateTrigger.setAttribute("aria-expanded", "true");
                returnCalGrid.querySelector(".selected:not(:disabled)")?.focus();
            }
        });

        returnCalPrev.addEventListener("click", function (e) {
            e.stopPropagation();
            returnViewDate.setMonth(returnViewDate.getMonth() - 1);
            renderCalendar("return");
        });

        returnCalNext.addEventListener("click", function (e) {
            e.stopPropagation();
            returnViewDate.setMonth(returnViewDate.getMonth() + 1);
            renderCalendar("return");
        });

        // Return Hour Trigger
        returnHourTrigger.addEventListener("click", function (e) {
            e.stopPropagation();
            const isOpen = !returnHourPopover.classList.contains("hidden");
            closeAllPopovers();
            if (!isOpen) {
                renderHourOptions(returnHourPopover, endTimeInput.value, (newHour) => {
                    endTimeInput.value = newHour;
                    updateAll();
                }, "return");
                returnHourPopover.classList.remove("hidden");
                returnHourTrigger.setAttribute("aria-expanded", "true");
                returnHourPopover.querySelector("button:not(:disabled)")?.focus();
            }
        });

        // Preset Buttons inside Pickup Calendar Popover
        form.querySelectorAll(".pickup-preset-btn").forEach((btn) => {
            const addDays = parseInt(btn.getAttribute("data-days"), 10);
            if (addDays === 0 && new Date().getHours() >= CLOSE_HOUR) {
                btn.classList.add("disabled");
                btn.disabled = true;
            }
            btn.addEventListener("click", function (e) {
                if (addDays === 0 && new Date().getHours() >= CLOSE_HOUR) return;
                e.stopPropagation();
                const d = new Date();
                d.setDate(d.getDate() + addDays);
                const dStr = toLocalDateString(d);
                startDateInput.value = dStr;
                pickupViewDate = d;
                updateAll();
                renderCalendar("pickup");
                closeAllPopovers();
            });
        });

        // Preset Buttons inside Return Calendar Popover
        form.querySelectorAll(".return-preset-btn").forEach((btn) => {
            btn.addEventListener("click", function (e) {
                e.stopPropagation();
                const addDays = parseInt(this.getAttribute("data-add-days"), 10);
                const sD = parseDate(startDateInput.value || toLocalDateString(new Date()));
                sD.setDate(sD.getDate() + addDays);
                const dStr = toLocalDateString(sD);
                endDateInput.value = dStr;
                returnViewDate = sD;
                updateAll();
                renderCalendar("return");
                closeAllPopovers();
            });
        });

        // Close popovers on click outside
        document.addEventListener("click", function (e) {
            if (!form.contains(e.target)) closeAllPopovers();
        });

        if (scheduleForm) {
            scheduleForm.addEventListener("submit", function (e) {
                if (renderScheduleValidation()) {
                    e.preventDefault();
                }
            });
        }

        // Initialize on start
        updateAll();
    }

    function initAllRentalSchedules() {
        document.querySelectorAll("form[data-rental-search]").forEach(initRentalSchedule);
    }

    if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", initAllRentalSchedules, { once: true });
    else initAllRentalSchedules();
})();
