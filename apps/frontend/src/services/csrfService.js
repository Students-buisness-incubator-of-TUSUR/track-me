class CsrfService {
    constructor() {
        this.token = null;
        this.headerName = 'X-CSRF-TOKEN';
    }

    async fetchCsrfToken() {
        try {
            const response = await fetch('/csrf', {
                credentials: 'include',
                method: 'GET'
            });

            if (response.ok) {
                const data = await response.json();
                this.token = data.token;
                this.headerName = data.headerName;
                return data;
            }
        } catch (error) {
            console.error('Failed to fetch CSRF token:', error);
        }
        return null;
    }

    async getHeaders() {
        if (!this.token) {
            await this.fetchCsrfToken();
        }

        return this.token ? {
            [this.headerName]: this.token
        } : {};
    }

    async secureRequest(url, options = {}) {
        const headers = await this.getHeaders();

        return fetch(url, {
            ...options,
            credentials: 'include',
            headers: {
                ...options.headers,
                ...headers
            }
        });
    }
}

// Использование
const csrfService = new CsrfService();

// Для logout
const logout = async () => {
    try {
        const response = await csrfService.secureRequest('/logout', {
            method: 'POST'
        });

        if (response.ok) {
            window.location.href = '/';
        }
    } catch (error) {
        console.error('Logout failed:', error);
    }
};

export default csrfService;
