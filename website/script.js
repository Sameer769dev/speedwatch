/* ===========================
   SpeedWatch – JavaScript
   =========================== */

(function () {
  'use strict';

  // ── Navbar scroll effect ──
  const navbar = document.getElementById('navbar');
  window.addEventListener('scroll', () => {
    navbar.classList.toggle('scrolled', window.scrollY > 20);
  }, { passive: true });

  // ── Mobile hamburger menu ──
  const hamburger = document.getElementById('hamburger');
  const mobileMenu = document.getElementById('mobile-menu');
  hamburger.addEventListener('click', () => {
    const open = mobileMenu.classList.toggle('open');
    hamburger.setAttribute('aria-expanded', open);
    mobileMenu.setAttribute('aria-hidden', !open);
  });
  mobileMenu.querySelectorAll('a').forEach(link => {
    link.addEventListener('click', () => {
      mobileMenu.classList.remove('open');
      hamburger.setAttribute('aria-expanded', false);
      mobileMenu.setAttribute('aria-hidden', true);
    });
  });

  // ── Intersection Observer: reveal on scroll ──
  const revealTargets = [
    '.feature-card',
    '.lab-card',
    '.pricing-card',
    '.step',
    '.section-header',
    '.privacy-card',
    '.lab-audit-note',
  ];

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.1, rootMargin: '0px 0px -40px 0px' }
  );

  revealTargets.forEach((selector) => {
    document.querySelectorAll(selector).forEach((el, i) => {
      el.classList.add('reveal');
      el.style.transitionDelay = `${i * 60}ms`;
      observer.observe(el);
    });
  });

  // ── Speedometer arc animation ──
  const arc = document.querySelector('.speed-arc');
  const needle = document.querySelector('.needle');

  function animateSpeedometer() {
    // Arc: 0 = full speed, 240 = zero speed (stroke-dasharray is 240)
    const fullDash = 240;
    // Animate through test cycle
    const phases = [
      { offset: 240, delay: 500 },   // start at 0
      { offset: 80, delay: 1500 },   // ramp up to ~67%
      { offset: 60, delay: 2500 },   // settle at ~75%
      { offset: 240, delay: 5000 },  // reset
    ];

    let current = 0;
    function nextPhase() {
      if (current >= phases.length) { current = 0; }
      const phase = phases[current];
      setTimeout(() => {
        if (arc) {
          arc.style.transition = 'stroke-dashoffset 1.4s cubic-bezier(0.4,0,0.2,1)';
          arc.style.strokeDashoffset = phase.offset;
        }
        // Rotate needle: offset 240 = 135deg start, 0 = 135+270=405deg
        if (needle) {
          const progress = 1 - phase.offset / fullDash; // 0 to 1
          const angle = 135 + progress * 270;
          const rad = angle * Math.PI / 180;
          const cx = 100, cy = 100, len = 60;
          const ex = cx + Math.cos(rad) * len;
          const ey = cy + Math.sin(rad) * len;
          needle.setAttribute('x2', ex.toFixed(1));
          needle.setAttribute('y2', ey.toFixed(1));
        }
        current++;
        nextPhase();
      }, phase.delay);
    }
    nextPhase();
  }

  // Start animation once hero is in view
  const heroEl = document.getElementById('hero');
  const heroObs = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting) {
        animateSpeedometer();
        heroObs.disconnect();
      }
    },
    { threshold: 0.3 }
  );
  if (heroEl) heroObs.observe(heroEl);

  // ── Smooth active nav link highlighting ──
  const sections = document.querySelectorAll('section[id]');
  const navLinks = document.querySelectorAll('.nav-links a, .mobile-menu a');

  const sectionObs = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          const id = entry.target.getAttribute('id');
          navLinks.forEach((link) => {
            link.style.color = link.getAttribute('href') === `#${id}`
              ? 'var(--cyan)'
              : '';
          });
        }
      });
    },
    { threshold: 0.4 }
  );
  sections.forEach((s) => sectionObs.observe(s));

})();
