'use strict';

document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('[data-checkout-form]');
    if (!form) return;

    const promoInput = document.getElementById('promoInput');
    const feedback = document.getElementById('promoFeedback');
    const discountRow = document.getElementById('discountRow');
    const basePriceRow = document.getElementById('basePriceRow');
    const discountLabel = document.getElementById('discountLabel');
    const values = {
        totalPrice: document.getElementById('totalRentalFeeDisplay'),
        basePrice: document.getElementById('baseRentalFeeDisplay'),
        discount: document.getElementById('discountAmountDisplay'),
        depositAmount: document.getElementById('depositDueDisplay'),
        depositPercent: document.getElementById('depositPercentDisplay')
    };
    const remainingBalance = document.getElementById('remainingBalanceDisplay');
    let quoteRequest;
    const originalValues = Object.fromEntries(Object.entries(values).map(([key, element]) => [key, element?.textContent || '']));
    originalValues.remainingAmount = remainingBalance?.textContent || '';

    const formatMoney = (amount) => Math.round(Number(amount || 0)).toLocaleString('en-US');
    const showFeedback = (message, success) => {
        if (!feedback) return;
        feedback.textContent = message || '';
        feedback.classList.toggle('hidden', !message);
        feedback.classList.toggle('text-success', Boolean(success));
        feedback.classList.toggle('text-danger', Boolean(message) && !success);
    };

    async function previewQuote() {
        if (!promoInput) return;
        const code = promoInput.value.trim();

        if (quoteRequest) quoteRequest.abort();
        quoteRequest = new AbortController();
        const params = new URLSearchParams({
            vehicleTypeId: form.querySelector('[name="vehicleTypeId"]').value,
            startTime: form.querySelector('[name="startTime"]').value,
            endTime: form.querySelector('[name="endTime"]').value,
            contractTemplateId: form.querySelector('[name="contractTemplateId"]').value,
            promoCode: code
        });
        showFeedback('Đang kiểm tra mã ưu đãi…', true);
        try {
            const response = await fetch(`${form.dataset.quoteUrl}?${params}`, {
                method: 'GET',
                headers: { Accept: 'application/json' },
                cache: 'no-store',
                signal: quoteRequest.signal
            });
            const quote = await response.json();
            if (!response.ok && quote.valid !== false) throw new Error('Quote request failed');
            if (quote.valid === false) {
                for (const [key, element] of Object.entries(values)) if (element) element.textContent = originalValues[key];
                if (remainingBalance) remainingBalance.textContent = originalValues.remainingAmount;
                if (discountRow) discountRow.classList.add('hidden');
                if (basePriceRow) basePriceRow.classList.add('hidden');
                showFeedback(quote.message || 'Mã ưu đãi chưa được áp dụng.', false);
                return;
            }
            for (const [key, element] of Object.entries(values)) {
                if (element && quote[key] !== undefined && quote[key] !== null) {
                    element.textContent = key === 'depositPercent' ? `${Number(quote[key]).toLocaleString('vi-VN', { maximumFractionDigits: 2 })}%` : formatMoney(quote[key]);
                }
            }
            if (remainingBalance && quote.remainingAmount != null) {
                remainingBalance.textContent = formatMoney(quote.remainingAmount);
            }
            const showDiscount = quote.valid && Number(quote.discount) > 0;
            if (discountRow) discountRow.classList.toggle('hidden', !showDiscount);
            if (basePriceRow) basePriceRow.classList.toggle('hidden', !showDiscount);
            if (discountLabel && quote.valid && code) discountLabel.textContent = `Ưu đãi (${code})`;
            showFeedback(quote.message || (code ? (quote.valid ? 'Đã cập nhật báo giá.' : 'Mã ưu đãi chưa được áp dụng.') : ''), quote.valid);
        } catch (error) {
            if (error.name !== 'AbortError') {
                for (const [key, element] of Object.entries(values)) if (element) element.textContent = originalValues[key];
                if (remainingBalance) remainingBalance.textContent = originalValues.remainingAmount;
                if (discountRow) discountRow.classList.add('hidden');
                if (basePriceRow) basePriceRow.classList.add('hidden');
                showFeedback('Chưa thể kiểm tra mã lúc này. Bạn vẫn có thể gửi yêu cầu đặt xe.', false);
            }
        }
    }

    document.addEventListener('click', (event) => {
        const action = event.target.closest('[data-action]')?.dataset.action;
        if (action === 'apply-promo') previewQuote();
        if (action === 'toggle-plate-picker') {
            const button = event.target.closest('[data-action]');
            const drawer = document.getElementById(button.getAttribute('aria-controls'));
            if (!drawer) return;
            const expanded = button.getAttribute('aria-expanded') === 'true';
            button.setAttribute('aria-expanded', String(!expanded));
            drawer.hidden = expanded;
            drawer.classList.toggle('hidden', expanded);
            document.getElementById('plateChevron')?.classList.toggle('customer-rotate-180', !expanded);
        }
        if (action === 'accept-terms') {
            const checkbox = document.getElementById('termsCheckbox');
            if (checkbox) checkbox.checked = true;
            const modal = document.getElementById('rentalTermsModal');
            window.bootstrap?.Modal.getOrCreateInstance(modal)?.hide();
        }
    });

    promoInput?.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            event.preventDefault();
            previewQuote();
        }
    });
    if (promoInput?.value.trim()) previewQuote();
});
