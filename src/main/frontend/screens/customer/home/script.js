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
