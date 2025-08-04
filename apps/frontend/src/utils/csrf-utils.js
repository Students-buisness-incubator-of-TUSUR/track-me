export const getCsrfConfig = () => {
    const token = localStorage.getItem('csrfToken');
    const headerName = localStorage.getItem('csrfHeaderName');
    
    if (!token || !headerName) {
        console.warn('CSRF token not found');
        return {};
    }
    
    return {
        headers: {
            [headerName]: token
        }
    };
};

export const getCsrfConfigForFetch = () => {
    const token = localStorage.getItem('csrfToken');
    const headerName = localStorage.getItem('csrfHeaderName');
    
    if (!token || !headerName) {
        console.warn('CSRF token not found');
        return {};
    }
    
    return {
        [headerName]: token
    };
};