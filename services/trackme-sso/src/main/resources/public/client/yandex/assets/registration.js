(() => {
    const form = document.getElementById('registration-form');
    if (!form) return;
    const button = form.querySelector('button[type=submit]');
    const username = form.elements.username;
    const role = form.elements.role;
    const checks = ['licenseAccepted', 'policyAccepted', 'consent'];
    const touched = new Set();
    let submitting = false;
    const messages = () => ({
        username: /^[A-Za-z0-9._-]{6,100}$/.test(username.value) ? '' : 'Введите от 6 до 100 символов: латинские буквы, цифры, _, . или -.',
        role: ['TRACKER', 'ADMIN'].includes(role.value) ? '' : 'Выберите роль.',
        licenseAccepted: form.elements.licenseAccepted.checked ? '' : 'Примите условия лицензионного соглашения.',
        policyAccepted: form.elements.policyAccepted.checked ? '' : 'Подтвердите ознакомление с политикой.',
        consent: form.elements.consent.checked ? '' : 'Необходимо согласие на обработку персональных данных.',
    });
    function update(showAll = false) {
        const errors = messages();
        for (const [name, message] of Object.entries(errors)) {
            if (showAll || touched.has(name)) {
                document.getElementById(`${name}-error`).textContent = message;
                form.elements[name].setAttribute('aria-invalid', String(Boolean(message)));
            }
        }
        button.disabled = submitting || Object.values(errors).some(Boolean);
        return !Object.values(errors).some(Boolean);
    }
    username.addEventListener('input', () => {
        username.value = username.value.replace(/^@/, '');
        touched.add('username');
        update();
    });
    [username, role, ...checks.map(name => form.elements[name])].forEach(input => {
        input.addEventListener('blur', () => { touched.add(input.name); update(); });
        input.addEventListener('change', () => { touched.add(input.name); update(); });
    });
    form.addEventListener('submit', event => {
        username.value = username.value.trim().replace(/^@/, '');
        if (submitting || !update(true)) {
            event.preventDefault();
            form.querySelector('[aria-invalid=true]')?.focus();
            return;
        }
        submitting = true;
        form.setAttribute('aria-busy', 'true');
        button.disabled = true;
        button.textContent = 'Завершаем регистрацию…';
    });
    // The back button may restore a page from the browser's navigation cache.
    window.addEventListener('pageshow', () => {
        submitting = false;
        form.removeAttribute('aria-busy');
        button.textContent = 'Завершить регистрацию';
        update();
    });
    update();
})();

(() => {
    const dialog = document.getElementById('license-dialog');
    if (!dialog) return;
    document.querySelectorAll('[data-open-license]').forEach(trigger => {
        trigger.addEventListener('click', event => {
            event.preventDefault();
            dialog.showModal();
        });
        if (trigger.getAttribute('role') === 'button') {
            trigger.addEventListener('keydown', event => {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    dialog.showModal();
                }
            });
        }
    });
    dialog.querySelector('[data-close-license]').addEventListener('click', () => dialog.close());
})();
