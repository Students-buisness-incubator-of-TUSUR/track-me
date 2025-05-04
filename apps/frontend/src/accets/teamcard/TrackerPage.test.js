import '@testing-library/jest-dom';
import React from 'react';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import configureStore from 'redux-mock-store';
import TrackerPage from './TrackerPage';
import { MemoryRouter } from 'react-router-dom';

const mockStore = configureStore([]);
const store = mockStore({
  user: {
    user: {
      roles: ['TRACKER'],
      username: 'testuser',
    },
  },
});

test('рендерит заголовок Track-me', () => {
  render(
    <Provider store={store}>
      <MemoryRouter>
        <TrackerPage />
      </MemoryRouter>
    </Provider>
  );

  const heading = screen.getByText(/track-me/i);
  expect(heading).toBeInTheDocument();
});
