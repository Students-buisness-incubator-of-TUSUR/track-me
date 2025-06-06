import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ProfilePage from './ProfilePage';
import { BrowserRouter } from 'react-router-dom';

global.fetch = jest.fn();

const mockUser = {
  username: 'testuser',
  roles: ['TRACKER']
};

const mockTeams = {
  content: [{ id: 1 }, { id: 2 }]
};

const renderComponent = () =>
  render(
    <BrowserRouter>
      <ProfilePage />
    </BrowserRouter>
  );

describe('ProfilePage — подсказка с количеством команд', () => {
  beforeEach(() => {
    global.fetch.mockImplementation((url) => {
      if (url.includes('/account/info')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(mockUser) });
      }
      if (url.includes('/team-cards')) {
        return Promise.resolve({ ok: true, json: () => Promise.resolve(mockTeams) });
      }
      return Promise.resolve({ ok: true, json: () => Promise.resolve({}) });
    });
    jest.clearAllMocks();
  });

  test('показывает количество команд и подсказку', async () => {
    renderComponent();
    const count = await screen.findByText('(2)');
    expect(count).toBeInTheDocument();

    fireEvent.mouseEnter(count);
    expect(screen.getByText('Количество моих команд')).toBeInTheDocument();

    fireEvent.mouseLeave(count);
    expect(screen.queryByText('Количество моих команд')).not.toBeInTheDocument();
  });
});
