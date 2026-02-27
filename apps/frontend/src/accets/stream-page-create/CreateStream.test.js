import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { useParams } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { useStreamForm } from '../stream-page-hooks/useStreamForm';
import CreateStream from './create-stream-page';
import '@testing-library/jest-dom'; // Import jest-dom for custom matchers

import { fetchTeams } from '../../services/requests';

jest.mock('react-router-dom', () => ({
  useNavigate: jest.fn(),
  useParams: jest.fn(),
  useNavigate: jest.fn(),
}));

jest.mock('../stream-page-hooks/useStreamForm', () => ({
  useStreamForm: jest.fn(),
}));

jest.mock('../../services/requests', () => ({
  fetchTeams: jest.fn(),
}));

describe('CreateStream Component', () => {
  const mockNavigate = jest.fn();
  const mockUseStreamForm = {
    name: '',
    startDate: '',
    endDate: '',
    showCheckboxes2: false,
    error: null,
    setError: jest.fn(),
    checkboxesData2: [
      { id: '1', name: 'Market 1', displayName: 'Market One' },
      { id: '2', name: 'Market 2', displayName: 'Market Two' },
    ],
    selectedCheckboxes: [],
    image: null,
    checkboxesRef: { current: null },
    errorRef: { current: null },
    handleNameChange: jest.fn(),
    handleStartDateChange: jest.fn(),
    handleEndDateChange: jest.fn(),
    handleShowCheckboxes2: jest.fn(),
    handleCheckboxChange: jest.fn(),
    handleImageUpload: jest.fn(),
    handleSubmit: jest.fn(),
  };

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
    useNavigate.mockReturnValue(mockNavigate);
    useParams.mockReturnValue({});
    useStreamForm.mockReturnValue(mockUseStreamForm);
  });

  // Test line 7: useNavigate hook
  test('should initialize useNavigate hook', () => {
    render(<CreateStream />);
    expect(useNavigate).toHaveBeenCalled();
  });

  // Test lines 27, 29, 31: Error message rendering and close button
  

  // Test line 35: Close button navigation
  test('should navigate back when close button is clicked', () => {
    render(<CreateStream />);
    const closeButton = screen.getByRole('button', { name: /×/i, classes: /create-stream-close/i });
    fireEvent.click(closeButton);
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  // Test line 42: Stream title rendering
  test('should render stream title', () => {
    render(<CreateStream />);
    expect(screen.getByText('Создание потока')).toBeInTheDocument();
  });

  // Test lines 80-82: Market dropdown button interaction
  test('should handle market dropdown button click and keydown', () => {
    render(<CreateStream />);
    const marketButton = screen.getByRole('button', { name: /Выбрать рынок/i });

    // Test click event
    fireEvent.click(marketButton);
    expect(mockUseStreamForm.handleShowCheckboxes2).toHaveBeenCalled();

    // Test keydown event (Enter key)
    fireEvent.keyDown(marketButton, { key: 'Enter' });
    expect(mockUseStreamForm.handleShowCheckboxes2).toHaveBeenCalledTimes(2);

    // Test keydown event (Space key)
    fireEvent.keyDown(marketButton, { key: ' ' });
    expect(mockUseStreamForm.handleShowCheckboxes2).toHaveBeenCalledTimes(3);
  });

  // Test lines 92-105: Checkbox rendering and interaction
  
  // Test lines 117-130: Image upload interaction
  test('should handle image upload click and keydown', async () => {
    render(<CreateStream />);
    const imageUploadArea = screen.getByRole('button', { name: /Загрузить изображение/i });

    // Test click event
    fireEvent.click(imageUploadArea);
    expect(document.getElementById('image-upload')).toBeTruthy();

    // Test keydown event (Enter key)
    fireEvent.keyDown(imageUploadArea, { key: 'Enter' });
    expect(document.getElementById('image-upload')).toBeTruthy();

    // Test image upload
    const file = new File(['dummy content'], 'test.png', { type: 'image/png' });
    const input = screen.getByLabelText('Загрузить изображение').querySelector('input[type="file"]');
    fireEvent.change(input, { target: { files: [file] } });
    expect(mockUseStreamForm.handleImageUpload).toHaveBeenCalled();
  });

  // Test form inputs
  test('should handle input changes for name, start date, and end date', () => {
    const { container } = render(<CreateStream />);

    // Test name input
    const nameInput = screen.getByPlaceholderText('Текст названия');
    fireEvent.change(nameInput, { target: { value: 'Test Stream' } });
    expect(mockUseStreamForm.handleNameChange).toHaveBeenCalled();

    // Test start date input
    const startDateInput = container.querySelector('input[name="startDate"]');
    fireEvent.change(startDateInput, { target: { value: '2025-01-01' } });
    expect(mockUseStreamForm.handleStartDateChange).toHaveBeenCalled();

    // Test end date input
    const endDateInput = container.querySelector('input[name="endDate"]');
    fireEvent.change(endDateInput, { target: { value: '2025-01-02' } });
    expect(mockUseStreamForm.handleEndDateChange).toHaveBeenCalled();
  });

  // Test form submission
  // Проверка закрытия ошибки при нажатии на кнопку закрытия
test('should clear error when close error button is clicked', () => {
  mockUseStreamForm.error = 'Произошла ошибка';
  useStreamForm.mockReturnValue(mockUseStreamForm);

  render(<CreateStream />);
  
  const buttons = screen.getAllByRole('button');
  const closeErrorButton = buttons.find(btn => btn.id == "create-stream_error-close");

  fireEvent.click(closeErrorButton);
  expect(mockUseStreamForm.setError).toHaveBeenCalledWith(null);
});


// Проверка вызова handleSubmit при нажатии на кнопку "Создать"
test('should call handleSubmit when create button is clicked', () => {
  render(<CreateStream />);
  
  const createButton = screen.getByRole('button', { name: /Создать/i });
  fireEvent.click(createButton);
  
  expect(mockUseStreamForm.handleSubmit).toHaveBeenCalled();
});
test('should render checkboxes and handle checkbox interactions', () => {
  // Активируем отображение чекбоксов
  mockUseStreamForm.showCheckboxes2 = true;
  useStreamForm.mockReturnValue(mockUseStreamForm);

  render(<CreateStream />);

  // Проверка наличия чекбоксов
  const checkbox1 = screen.getByLabelText(/Market One/i);
  const checkbox2 = screen.getByLabelText(/Market Two/i);

  expect(checkbox1).toBeInTheDocument();
  expect(checkbox2).toBeInTheDocument();

  // Клик по чекбоксу
  fireEvent.click(checkbox1);
  expect(mockUseStreamForm.handleCheckboxChange).toHaveBeenCalledWith('1');

  fireEvent.click(checkbox2);
  expect(mockUseStreamForm.handleCheckboxChange).toHaveBeenCalledWith('2');
});

  describe('CreateStream Component in edit mode (id passed)', () => {
    const mockId = '123';
    const mockStreamName = 'Test Stream';
    const mockTeams = [
      { id: 1, name: 'Team Alpha', streams: [{ name: 'Test Stream' }] },
      { id: 2, name: 'Team Beta', streams: [{ name: 'Other Stream' }] },
      { id: 3, name: 'Team Gamma', streams: [{ name: 'Test Stream' }] },
    ];

    beforeEach(() => {
      jest.clearAllMocks();
      useNavigate.mockReturnValue(mockNavigate);
      useParams.mockReturnValue({ id: mockId });

      fetchTeams.mockResolvedValue({
        ok: true,
        json: async () => ({ content: mockTeams }),
      });

      useStreamForm.mockReturnValue({
        ...mockUseStreamForm,
        name: mockStreamName,
        deleteStream: jest.fn(),
      });
    });

    test('should render and delete button', () => {
      render(<CreateStream />);
      expect(screen.getByTestId('button-delete')).toBeInTheDocument();
    });

    test('should fetch teams on mount', async () => {
      render(<CreateStream />);
      await waitFor(() => {
        expect(fetchTeams).toHaveBeenCalledWith(0, 1000);
      });
    });

    test('should filter attached teams correctly based on stream name', async () => {
      render(<CreateStream />);
      await waitFor(() => {
        const deleteButton = screen.getByTestId('button-delete');
        fireEvent.click(deleteButton);

        expect(screen.getByTestId("delete-teams-modal")).toBeInTheDocument();
        expect(screen.getByText('Team Alpha')).toBeInTheDocument();
        expect(screen.getByText('Team Gamma')).toBeInTheDocument();
        expect(screen.queryByText('Team Beta')).not.toBeInTheDocument();
      });
    });

    test('should show delete confirmation modal when no attached teams', async () => {
      fetchTeams.mockResolvedValue({
        ok: true,
        json: async () => ({ content: [] }),
      });

      render(<CreateStream />);
      await waitFor(() => {
        const deleteButton = screen.getByTestId('button-delete');
        fireEvent.click(deleteButton);
        expect(screen.getByTestId("delete-confirm-modal")).toBeInTheDocument();
      });
    });

    test('should call deleteStream when delete is confirmed', async () => {
      const mockDeleteStream = jest.fn();
      useStreamForm.mockReturnValue({
        ...mockUseStreamForm,
        name: mockStreamName,
        deleteStream: mockDeleteStream,
      });

      fetchTeams.mockResolvedValue({
        ok: true,
        json: async () => ({ content: [] }),
      });

      render(<CreateStream />);
      await waitFor(() => {
        const deleteButton = screen.getByTestId('button-delete');
        fireEvent.click(deleteButton);
      });

      const confirmYesButton = screen.getByTestId('delete-confirm-yes');
      fireEvent.click(confirmYesButton);

      expect(mockDeleteStream).toHaveBeenCalled();
    });

    test('should call handleSubmit with isEditMode=true when update button is clicked', () => {
      render(<CreateStream />);
      const updateButton = screen.getByTestId("action-button");
      fireEvent.click(updateButton);
      expect(mockUseStreamForm.handleSubmit).toHaveBeenCalledWith(true);
    });

  });
});
