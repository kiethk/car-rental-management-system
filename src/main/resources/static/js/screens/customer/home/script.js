'use strict';

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-fallback-src]').forEach((image) => {
        image.addEventListener('error', () => {
            image.src = image.dataset.fallbackSrc;
            image.removeAttribute('data-fallback-src');
        }, { once: true });
    });
});
(() => {
        const carousel = document.getElementById('hero-carousel');
        if (!carousel) return;
        const slides = Array.from(carousel.querySelectorAll('.promotion-slide'));
        let current = 0;
        let animating = false;
        // direction 1 = next: the new slide enters from the right; -1 = previous: it enters from the left.
        const showSlide = (index, direction) => {
            if (animating || slides.length < 2) return;
            const next = (index + slides.length) % slides.length;
            if (next === current) return;
            animating = true;
            const from = slides[current];
            const to = slides[next];

            // Park the incoming slide just outside the frame without animating, then slide both.
            to.style.transition = 'none';
            to.style.transform = `translateX(${direction * 100}%)`;
            to.style.visibility = 'visible';
            void to.offsetWidth;
            to.style.transition = '';
            to.style.transform = 'translateX(0)';
            from.style.transform = `translateX(${-direction * 100}%)`;

            let finished = false;
            const finish = () => {
                if (finished) return;
                finished = true;
                from.classList.remove('active');
                from.setAttribute('aria-hidden', 'true');
                from.style.visibility = '';
                from.style.transform = '';
                to.classList.add('active');
                to.setAttribute('aria-hidden', 'false');
                to.style.visibility = '';
                to.style.transform = '';
                current = next;
                animating = false;
            };
            to.addEventListener('transitionend', finish, { once: true });
            window.setTimeout(finish, 700); // also finishes when transitions are off (reduced motion)
        };

        let timer = null;
        let paused = false;
        const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
        const startAutoplay = () => {
            if (slides.length > 1 && !paused && !reduceMotion.matches && !document.hidden && !timer) {
                timer = window.setInterval(() => showSlide(current + 1, 1), 5000);
            }
        };
        const stopAutoplay = () => {
            if (timer) window.clearInterval(timer);
            timer = null;
        };

        carousel.querySelectorAll('[data-promotion-direction]').forEach((button) => {
            button.addEventListener('click', () => {
                const direction = Number(button.dataset.promotionDirection);
                showSlide(current + direction, direction);
                stopAutoplay();
                startAutoplay();
            });
        });
        // Pause while the visitor is looking at a slide.
        carousel.addEventListener('mouseenter', () => { paused = true; stopAutoplay(); });
        carousel.addEventListener('mouseleave', () => { paused = false; startAutoplay(); });
        carousel.addEventListener('focusin', () => { paused = true; stopAutoplay(); });
        carousel.addEventListener('focusout', (event) => {
            if (!carousel.contains(event.relatedTarget)) { paused = false; startAutoplay(); }
        });
        document.addEventListener('visibilitychange', () => document.hidden ? stopAutoplay() : startAutoplay());
        reduceMotion.addEventListener?.('change', () => reduceMotion.matches ? stopAutoplay() : startAutoplay());
        startAutoplay();
    })();

// Horizontal promotion gallery: arrow controls + mouse drag.
(() => {
    const grid = document.querySelector('.home-promo-grid');
    const controls = document.querySelector('.home-promo-controls');
    if (!grid || !controls) return;

    const previous = controls.querySelector('[data-promo-direction="-1"]');
    const next = controls.querySelector('[data-promo-direction="1"]');
    const buttons = Array.from(controls.querySelectorAll('[data-promo-direction]'));
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const maxScroll = () => Math.max(0, grid.scrollWidth - grid.clientWidth);

    const stepSize = () => {
        const card = grid.querySelector('.home-promo-card');
        if (!card) return grid.clientWidth;
        const gap = parseFloat(getComputedStyle(grid).columnGap) || 0;
        return card.getBoundingClientRect().width + gap;
    };

    const updateControls = () => {
        const max = maxScroll();
        const hasOverflow = max > 1;
        controls.hidden = !hasOverflow;
        if (previous) previous.disabled = !hasOverflow || grid.scrollLeft <= 1;
        if (next) next.disabled = !hasOverflow || grid.scrollLeft >= max - 1;
    };

    buttons.forEach((button) => {
        button.addEventListener('click', () => {
            const direction = Number(button.dataset.promoDirection) || 0;
            if (!direction) return;
            grid.scrollBy({
                left: direction * stepSize(),
                behavior: reduceMotion.matches ? 'auto' : 'smooth'
            });
        });
    });

    grid.addEventListener('scroll', updateControls, { passive: true });
    grid.addEventListener('dragstart', (event) => event.preventDefault());
    if ('ResizeObserver' in window) {
        new ResizeObserver(updateControls).observe(grid);
    } else {
        window.addEventListener('resize', updateControls);
    }

    let drag = null;
    grid.addEventListener('pointerdown', (event) => {
        if (event.pointerType !== 'mouse' || event.button !== 0) return;
        if (event.target.closest('a, button')) return;
        drag = { id: event.pointerId, startX: event.clientX, startLeft: grid.scrollLeft, moved: false };
        grid.classList.add('is-dragging');
        try { grid.setPointerCapture(event.pointerId); } catch (error) { /* pointer capture is optional */ }
    });
    grid.addEventListener('pointermove', (event) => {
        if (!drag || event.pointerId !== drag.id) return;
        const distance = event.clientX - drag.startX;
        if (Math.abs(distance) > 3) drag.moved = true;
        if (!drag.moved) return;
        event.preventDefault();
        grid.scrollLeft = drag.startLeft - distance;
    });

    const endDrag = (event) => {
        if (!drag || (event && event.pointerId !== drag.id)) return;
        const pointerId = drag.id;
        drag = null;
        grid.classList.remove('is-dragging');
        try { grid.releasePointerCapture(pointerId); } catch (error) { /* already released */ }
        updateControls();
    };
    grid.addEventListener('pointerup', endDrag);
    grid.addEventListener('pointercancel', endDrag);
    grid.addEventListener('lostpointercapture', endDrag);

    updateControls();
})();
