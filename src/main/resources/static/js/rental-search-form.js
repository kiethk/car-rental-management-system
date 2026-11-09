/*
 * Date and time pickers of the rental search form (fragments/rental-search-form.html).
 * Reads data-open-hour / data-close-hour / data-server-error from the form and keeps the hidden
 * startDate/startTime/endDate/endTime inputs in step with what the customer picks.
 */
(function () {
    function initRentalSchedule() {
        const startDateInput = document.getElementById("rental-start-date");
        const startTimeInput = document.getElementById("rental-start-time");
        const endDateInput = document.getElementById("rental-end-date");
        const endTimeInput = document.getElementById("rental-end-time");
        const itineraryText = document.getElementById("rental-itinerary");

        if (!startDateInput || !startTimeInput || !endDateInput || !endTimeInput) return;

        const pickupDateTrigger = document.getElementById("pickup-date-trigger");
        const pickupDateDisplay = document.getElementById("pickup-date-display");
        const pickupDatePopover = document.getElementById("pickup-date-popover");
        const pickupCalPrev = document.getElementById("pickup-cal-prev");
        const pickupCalNext = document.getElementById("pickup-cal-next");
        const pickupCalMonthYear = document.getElementById("pickup-cal-month-year");
        const pickupCalGrid = document.getElementById("pickup-cal-grid");

        const pickupHourTrigger = document.getElementById("pickup-hour-trigger");
        const pickupHourDisplay = document.getElementById("pickup-hour-display");
        const pickupHourPopover = document.getElementById("pickup-hour-popover");

        const returnDateTrigger = document.getElementById("return-date-trigger");
        const returnDateDisplay = document.getElementById("return-date-display");
        const returnDatePopover = document.getElementById("return-date-popover");
        const returnCalPrev = document.getElementById("return-cal-prev");
        const returnCalNext = document.getElementById("return-cal-next");
        const returnCalMonthYear = document.getElementById("return-cal-month-year");
        const returnCalGrid = document.getElementById("return-cal-grid");

        const returnHourTrigger = document.getElementById("return-hour-trigger");
        const returnHourDisplay = document.getElementById("return-hour-display");
        const returnHourPopover = document.getElementById("return-hour-popover");

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
        const hoursHost = document.querySelector("[data-open-hour]");
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
            pickupDatePopover.classList.add("hidden");
            pickupHourPopover.classList.add("hidden");
            returnDatePopover.classList.add("hidden");
            returnHourPopover.classList.add("hidden");
        }

        // Render Hour Popovers (Dark theme)
        function renderHourOptions(popoverEl, currentVal, onSelect, type) {
            popoverEl.innerHTML = "";
            const now = new Date();
            const todayStr = toLocalDateString(now);
            const isPickup = (type === "pickup");
            hoursList.forEach((h) => {
                const item = document.createElement("div");
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
                    item.className = "hour-option-dark disabled";
                    item.style.opacity = "0.35";
                    item.style.cursor = "not-allowed";
                    item.style.pointerEvents = "none";
                    item.style.color = "#94a3b8";
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
                document.querySelectorAll(".pickup-preset-btn").forEach((btn) => {
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
                document.querySelectorAll(".return-preset-btn").forEach((btn) => {
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

        const scheduleForm = startDateInput.closest("form");
        const scheduleErrorBox = document.getElementById("rental-schedule-error");
        const scheduleSubmitBtn = document.getElementById("rental-search-btn");
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
            }
        });

        // Preset Buttons inside Pickup Calendar Popover
        document.querySelectorAll(".pickup-preset-btn").forEach((btn) => {
            const addDays = parseInt(btn.getAttribute("data-days"), 10);
            if (addDays === 0 && new Date().getHours() >= CLOSE_HOUR) {
                btn.classList.add("disabled");
                btn.style.opacity = "0.35";
                btn.style.pointerEvents = "none";
                btn.style.cursor = "not-allowed";
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
        document.querySelectorAll(".return-preset-btn").forEach((btn) => {
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
            if (
                !e.target.closest("#pickup-date-wrapper") &&
                !e.target.closest("#pickup-hour-wrapper") &&
                !e.target.closest("#return-date-wrapper") &&
                !e.target.closest("#return-hour-wrapper")
            ) {
                closeAllPopovers();
            }
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

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initRentalSchedule);
    } else {
        initRentalSchedule();
    }
})();
