// src/accets/teamcard/TrackerPage.test.js
import React from 'react';
import {render, screen, waitFor, fireEvent} from '@testing-library/react';
import {Provider} from 'react-redux';
import {MemoryRouter} from 'react-router-dom';
import configureStore from 'redux-mock-store';
import TrackerPage from './TrackerPage';

const mockStore = configureStore([]);

describe('TrackerPage component', () => {
    let store;

    beforeEach(() => {
        store = mockStore({
            user: {
                user: {
                    roles: ['TRACKER'],
                    username: 'testuser',
                }
            }
        });

        // Мокаем fetch
        global.fetch = jest.fn((url) => {
            if (url.includes('/streams/nti-markets')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve([]),
                });
            }

            if (url.includes('/streams')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({content: []}),
                });
            }

            if (url.includes('/team-cards')) {
                return Promise.resolve({
                    ok: true,
                    json: () => Promise.resolve({content: []}),
                });
            }

            return Promise.reject(new Error('Unknown URL'));
        });
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    test('renders without crashing and shows default title for TRACKER', async () => {
        render(
            <Provider store={store}>
                <MemoryRouter>
                    <TrackerPage/>
                </MemoryRouter>
            </Provider>
        );

        await waitFor(() => {
            expect(screen.getByText('Track-me')).toBeInTheDocument();
        });
    });

    test('shows search input and handles search', async () => {
        render(
            <Provider store={store}>
                <MemoryRouter>
                    <TrackerPage/>
                </MemoryRouter>
            </Provider>
        );

        const input = screen.getByPlaceholderText('Найти');
        fireEvent.change(input, {target: {value: 'тест'}});
        expect(input.value).toBe('тест');
    });

    test('displays empty message when no cards found', async () => {
        render(
            <Provider store={store}>
                <MemoryRouter>
                    <TrackerPage/>
                </MemoryRouter>
            </Provider>
        );

        await waitFor(() => {
            expect(screen.getByText(/Ничего не найдено по запросу/)).toBeInTheDocument();
        });
    });
});
