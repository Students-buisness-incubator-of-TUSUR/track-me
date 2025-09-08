import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { useParams, useNavigate } from 'react-router-dom';
import { useStreamForm } from '../stream-page-hooks/useStreamForm';
import EditStream from './redo-stream-page';
import '@testing-library/jest-dom';

// Mock the useParams and useNavigate hooks
jest.mock('react-router-dom', () => ({
  useParams: jest.fn(),
  useNavigate: jest.fn(),
}));

// Mock the useStreamForm hook
jest.mock('../stream-page-hooks/useStreamForm', () => ({
  useStreamForm: jest.fn(),
}));

describe('EditStream Component', () => {
  const mockNavigate = jest.fn();
  const mockUseParams = { id: '123' };
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
    useParams.mockReturnValue(mockUseParams);
    useNavigate.mockReturnValue(mockNavigate);
    useStreamForm.mockReturnValue(mockUseStreamForm);
  });

  // Test lines 7-9: useParams, useNavigate, and useStreamForm hooks
  test('should initialize useParams, useNavigate, and useStreamForm hooks', () => {
    render(<EditStream />);
    expect(useParams).toHaveBeenCalled();
    expect(useNavigate).toHaveBeenCalled();
    expect(useStreamForm).toHaveBeenCalledWith('123', mockNavigate);
  });

  

  // Test line 38: Close button navigation
  test('should navigate back when close button is clicked', () => {
    render(<EditStream />);
    const closeButton = screen.getByRole('button', { name: /×/i, classes: /create-stream-close/i });
    fireEvent.click(closeButton);
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  // Test line 42: Stream title rendering
  test('should render stream title', () => {
    render(<EditStream />);
    expect(screen.getByText('Редактирование потока')).toBeInTheDocument();
  });

  

  // Test line 122: Form submission
  test('should handle form submission', () => {
    render(<EditStream />);
    const submitButton = screen.getByRole('button', { name: /Обновить/i });
    fireEvent.click(submitButton);
    expect(mockUseStreamForm.handleSubmit).toHaveBeenCalledWith(true);
  });

  // Test form inputs
  test('should handle input changes for name, start date, and end date', () => {
    render(<EditStream />);

    // Test name input
    const nameInput = screen.getByPlaceholderText('Текст названия');
    fireEvent.change(nameInput, { target: { value: 'Test Stream' } });
    expect(mockUseStreamForm.handleNameChange).toHaveBeenCalled();

    // Test start date input
    const dateInputs = screen.getAllByPlaceholderText('__.__.____');
    const startDateInput = dateInputs[0]; // First input is start date
    fireEvent.change(startDateInput, { target: { value: '01.01.2025' } });
    expect(mockUseStreamForm.handleStartDateChange).toHaveBeenCalled();

    // Test end date input
    const endDateInput = dateInputs[1]; // Second input is end date
    fireEvent.change(endDateInput, { target: { value: '02.01.2025' } });
    expect(mockUseStreamForm.handleEndDateChange).toHaveBeenCalled();
  });
  test('should render checkboxes and handle checkbox interactions', () => {
  // Включаем отображение чекбоксов
  mockUseStreamForm.showCheckboxes2 = true;
  useStreamForm.mockReturnValue(mockUseStreamForm);

  render(<EditStream />);

  // Проверяем наличие чекбоксов
  const checkbox1 = screen.getByLabelText(/Market One/i);
  const checkbox2 = screen.getByLabelText(/Market Two/i);

  expect(checkbox1).toBeInTheDocument();
  expect(checkbox2).toBeInTheDocument();

  // Кликаем по чекбоксам
  fireEvent.click(checkbox1);
  expect(mockUseStreamForm.handleCheckboxChange).toHaveBeenCalledWith('1');

  fireEvent.click(checkbox2);
  expect(mockUseStreamForm.handleCheckboxChange).toHaveBeenCalledWith('2');
});
test('should clear error when close error button is clicked', () => {
  mockUseStreamForm.error = 'Произошла ошибка';
  useStreamForm.mockReturnValue(mockUseStreamForm);

  render(<EditStream />);
  
  const buttons = screen.getAllByRole('button', { name: '×' });
  const closeErrorButton = buttons.find(btn => btn.className.includes('stream-error-close'));

  fireEvent.click(closeErrorButton);
  expect(mockUseStreamForm.setError).toHaveBeenCalledWith(null);
});
test('should toggle checkboxes on Enter and Space key press', () => {
  render(<EditStream />);
  const button = screen.getByRole('button', { name: 'Выбрать рынок' });

  fireEvent.keyDown(button, { key: 'Enter' });
  expect(mockUseStreamForm.handleShowCheckboxes2).toHaveBeenCalled();

  fireEvent.keyDown(button, { key: ' ' });
  expect(mockUseStreamForm.handleShowCheckboxes2).toHaveBeenCalledTimes(2);
});

test('should trigger image upload on Enter and Space key press', () => {
  // Мокаем document.getElementById
  const mockClick = jest.fn();
  document.getElementById = jest.fn().mockReturnValue({ click: mockClick });

  render(<EditStream />);
  const uploadButton = screen.getByRole('button', { name: 'Загрузить изображение' });

  fireEvent.keyDown(uploadButton, { key: 'Enter' });
  expect(mockClick).toHaveBeenCalledTimes(1);

  fireEvent.keyDown(uploadButton, { key: ' ' });
  expect(mockClick).toHaveBeenCalledTimes(2);
});
});

