import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import Stream from "./stream-page";
import { MemoryRouter } from 'react-router-dom';

jest.mock('axios', () => ({
  post: jest.fn(),
  get: jest.fn(),
}));

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  Link: ({ children, to, ...rest }) => <a href={to} {...rest}>{children}</a>,
}));

describe('Stream Component', () => {
  const mockData = {
    content: [
      {
        id: 1,
        name: 'Test Stream',
        description: 'Test Description',
        startDate: '2023-01-01',
        endDate: '2023-12-31',
        readinessLevel: '5',
      },
    ],
    page: {
      totalPages: 3,
    },
  };

  const mockCheckboxesData2 = [
    { id: 1, name: 'Market 1', description: 'Market Desc 1' },
    { id: 2, name: 'Market 2', description: 'Market Desc 2' },
  ];

  beforeEach(() => {
    require('axios').post.mockResolvedValue({ data: mockData });
    require('axios').get.mockImplementation((url) => {
      if (url.includes('nti-markets')) {
        return Promise.resolve({ data: mockCheckboxesData2 });
      }
      return Promise.resolve({ data: new Blob() });
    });

    URL.createObjectURL = jest.fn(() => 'mock-url');

    Storage.prototype.removeItem = jest.fn();
    Storage.prototype.setItem = jest.fn();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should import all required dependencies (line 5)', () => {
    expect(Stream).toBeDefined();
  });

  it('should toggle profile menu (line 35)', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => screen.getByText("TrackMe"));

    const profileButton = document.querySelector(".Stream-pic");
    fireEvent.click(profileButton);
    expect(screen.getByText('Личный кабинет')).toBeInTheDocument();

    fireEvent.click(profileButton);
    await waitFor(() => {
      expect(screen.queryByText('Личный кабинет')).not.toBeInTheDocument();
    });
  });

  it('should clear localStorage on logout (lines 37–40)', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => screen.getByText("TrackMe"));

    const profileButton = document.querySelector(".Stream-pic");
    fireEvent.click(profileButton);

    const logoutLink = screen.getByText('Выход');
    fireEvent.click(logoutLink);

    expect(localStorage.removeItem).toHaveBeenCalledWith("user");
    expect(localStorage.removeItem).toHaveBeenCalledWith("userRole");
    expect(localStorage.removeItem).toHaveBeenCalledWith("streamName");
    expect(localStorage.removeItem).toHaveBeenCalledWith("streamId");
    expect(localStorage.removeItem).toHaveBeenCalledWith("streamSDate");
    expect(localStorage.removeItem).toHaveBeenCalledWith("streamEDate");
  });

  it('should display stream cards (lines 273–282)', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText("Test Stream")).toBeInTheDocument();
      expect(screen.getByText("редактировать")).toBeInTheDocument();
    });
  });

  it('should handle pagination buttons (lines 367–381)', async () => {
    render(
      <MemoryRouter>
        <Stream />
      </MemoryRouter>
    );

    await waitFor(() => {
      const buttons = document.querySelectorAll(".Stream-footer-button-4, .Stream-footer-button-5");
      expect(buttons.length).toBeGreaterThan(0);
    });
  });

  
});
