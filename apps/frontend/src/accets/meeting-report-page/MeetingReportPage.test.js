 import React from 'react';
 import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
 import '@testing-library/jest-dom';
 import { MemoryRouter, Route, Routes } from 'react-router-dom';
 import MeetingReportPage from './MeetingReportPage';
 import * as requests from '../../services/requests';
 import * as util from '../../services/util';

 // Моки
 jest.mock('../../services/requests', () => ({
   fetchMeetingReport: jest.fn(),
   fetchMeetingReportExcel: jest.fn(),
 }));

 jest.mock('../../services/util', () => ({
   useGetUserInfo: jest.fn(),
 }));

 jest.mock('../header/header', () => () => <div data-testid="mock-header">Header</div>);

 const mockNavigate = jest.fn();
 jest.mock('react-router-dom', () => ({
   ...jest.requireActual('react-router-dom'),
   useParams: () => ({ streamId: 'test-stream-id' }),
   useNavigate: () => mockNavigate,
 }));

 describe('MeetingReportPage - Hyperlink тесты', () => {
   const mockData = {
     content: [
       {
         teamId: 'team-123',
         teamName: 'Команда Альфа',
         startDate: '2024-01-15T10:00:00Z',
         trackerName: 'tracker1',
         trackerFullName: 'Иван Трекеров',
         status: 'COMPLETED',
         teamStatus: 'OK',
         tasksNextMeeting: 'Подготовить отчет',
         tasksCurrentMeeting: 'Провели встречу',
       },
       {
         teamId: 'team-456',
         teamName: 'Команда Бета',
         startDate: '2024-01-20T10:00:00Z',
         trackerName: 'tracker2',
         trackerFullName: 'Петр Трекеров',
         status: 'SCHEDULED',
         teamStatus: null,
         tasksNextMeeting: '',
         tasksCurrentMeeting: '',
       },
     ],
   };

   beforeEach(() => {
     jest.clearAllMocks();
     util.useGetUserInfo.mockReturnValue({ roles: ['ADMIN'] });
     requests.fetchMeetingReport.mockResolvedValue({
       ok: true,
       json: async () => mockData,
     });
     mockNavigate.mockClear();
   });

   const renderComponent = () => {
     return render(
       <MemoryRouter initialEntries={['/report/test-stream-id']}>
         <Routes>
           <Route path="/report/:streamId" element={<MeetingReportPage />} />
           <Route path="/teamcard/:teamId" element={<div data-testid="teamcard-page" />} />
         </Routes>
       </MemoryRouter>
     );
   };

   test('отображает название команды как гиперссылку', async () => {
     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Команда Альфа')).toBeInTheDocument();
     });

     const teamLink = screen.getByText('Команда Альфа');
     expect(teamLink).toHaveStyle({ cursor: 'pointer', color: '#843AEB', textDecoration: 'underline' });
   });

   test('клик по названию команды вызывает navigate с правильным teamId', async () => {
     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Команда Альфа')).toBeInTheDocument();
     });

     const teamLink = screen.getByText('Команда Альфа');
     fireEvent.click(teamLink);

     expect(mockNavigate).toHaveBeenCalledWith('/teamcard/team-123');
   });

   test('клик по второй команде вызывает navigate с правильным teamId', async () => {
     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Команда Бета')).toBeInTheDocument();
     });

     const teamLink = screen.getByText('Команда Бета');
     fireEvent.click(teamLink);

     expect(mockNavigate).toHaveBeenCalledWith('/teamcard/team-456');
   });

   test('отображает все команды с корректными teamId', async () => {
     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Команда Альфа')).toBeInTheDocument();
       expect(screen.getByText('Команда Бета')).toBeInTheDocument();
     });

     const teamLinks = screen.getAllByRole('cell', { name: /Команда/ });
     expect(teamLinks).toHaveLength(2);
   });

   test('обрабатывает отсутствие teamId в данных', async () => {
     const dataWithoutTeamId = {
       content: [
         {
           teamName: 'Команда без ID',
           startDate: '2024-01-15T10:00:00Z',
           trackerName: 'tracker1',
           trackerFullName: 'Иван Трекеров',
           status: 'COMPLETED',
           teamStatus: 'OK',
         },
       ],
     };
     requests.fetchMeetingReport.mockResolvedValue({
       ok: true,
       json: async () => dataWithoutTeamId,
     });

     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Команда без ID')).toBeInTheDocument();
     });

     const teamLink = screen.getByText('Команда без ID');
     fireEvent.click(teamLink);

     expect(mockNavigate).toHaveBeenCalledWith('/teamcard/undefined');
   });

   test('загрузка данных показывает индикатор загрузки', async () => {
     requests.fetchMeetingReport.mockImplementation(() => new Promise(() => {}));

     renderComponent();

     expect(screen.getByText('Загрузка...')).toBeInTheDocument();
   });

   test('пустые данные показывают сообщение "Нет данных"', async () => {
     requests.fetchMeetingReport.mockResolvedValue({
       ok: true,
       json: async () => ({ content: [] }),
     });

     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Нет данных')).toBeInTheDocument();
     });
   });
 });

 describe('MeetingReportPage - Фильтры и сортировка', () => {
   const mockData = {
     content: [
       {
         teamId: 'team-1',
         teamName: 'Команда А',
         startDate: '2024-01-15T10:00:00Z',
         trackerName: 'tracker1',
         trackerFullName: 'Иван Трекеров',
         status: 'COMPLETED',
         teamStatus: 'OK',
       },
       {
         teamId: 'team-2',
         teamName: 'Команда Б',
         startDate: '2024-01-20T10:00:00Z',
         trackerName: 'tracker2',
         trackerFullName: 'Петр Трекеров',
         status: 'COMPLETED',
         teamStatus: 'WITH_ISSUES',
       },
     ],
   };

   beforeEach(() => {
     jest.clearAllMocks();
     util.useGetUserInfo.mockReturnValue({ roles: ['ADMIN'] });
     requests.fetchMeetingReport.mockResolvedValue({
       ok: true,
       json: async () => mockData,
     });
   });

   const renderComponent = () => {
     return render(
       <MemoryRouter initialEntries={['/report/test-stream-id']}>
         <Routes>
           <Route path="/report/:streamId" element={<MeetingReportPage />} />
         </Routes>
       </MemoryRouter>
     );
   };

   test('кнопка "Назад" вызывает navigate(-1)', async () => {
     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Команда А')).toBeInTheDocument();
     });

     const backButton = screen.getByText('← Назад');
     fireEvent.click(backButton);

     expect(mockNavigate).toHaveBeenCalledWith(-1);
   });

   test('фильтр по команде работает', async () => {
     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Команда А')).toBeInTheDocument();
     });

     const teamFilterBtn = screen.getByText('Команда');
     fireEvent.click(teamFilterBtn);

     const teamOption = await screen.findByText('Команда А');
     fireEvent.click(teamOption);

     await waitFor(() => {
       expect(requests.fetchMeetingReport).toHaveBeenCalledWith(
         expect.objectContaining({
           filters: expect.arrayContaining([
             { fieldName: 'teamName', type: 'EQ', value: 'Команда А' },
           ]),
         })
       );
     });
   });

   test('фильтр по трекеру работает', async () => {
     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Команда А')).toBeInTheDocument();
     });

     const trackerFilterBtn = screen.getByText('Трекеры');
     fireEvent.click(trackerFilterBtn);

     const trackerOption = await screen.findByText('Иван Трекеров (@tracker1)');
     fireEvent.click(trackerOption);

     await waitFor(() => {
       expect(requests.fetchMeetingReport).toHaveBeenCalledWith(
         expect.objectContaining({
           filters: expect.arrayContaining([
             { fieldName: 'trackerFullName', type: 'EQ', value: 'Иван Трекеров' },
           ]),
         })
       );
     });
   });

   test('сортировка по названию команды работает', async () => {
     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Команда А')).toBeInTheDocument();
     });

     const teamHeader = screen.getByText(/Название команды/);
     fireEvent.click(teamHeader);

     await waitFor(() => {
       expect(requests.fetchMeetingReport).toHaveBeenCalledWith(
         expect.objectContaining({
           sort: ['teamName,desc', 'startDate,desc'],
         })
       );
     });
   });
 });

 describe('MeetingReportPage - Статусы', () => {
   beforeEach(() => {
     jest.clearAllMocks();
     util.useGetUserInfo.mockReturnValue({ roles: ['ADMIN'] });
   });

   const renderComponent = () => {
     return render(
       <MemoryRouter initialEntries={['/report/test-stream-id']}>
         <Routes>
           <Route path="/report/:streamId" element={<MeetingReportPage />} />
         </Routes>
       </MemoryRouter>
     );
   };

   test('отображает статус "Запланирована" для SCHEDULED встреч', async () => {
     const mockScheduled = {
       content: [
         {
           teamId: 'team-1',
           teamName: 'Команда',
           startDate: '2024-01-15T10:00:00Z',
           trackerName: 'tracker1',
           trackerFullName: 'Трекер',
           status: 'SCHEDULED',
           teamStatus: null,
         },
       ],
     };
     requests.fetchMeetingReport.mockResolvedValue({
       ok: true,
       json: async () => mockScheduled,
     });

     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Запланирована')).toBeInTheDocument();
     });
   });

   test('отображает статус "Не состоялась" для COMPLETED_AS_NOT_HAPPENED', async () => {
     const mockNotHappened = {
       content: [
         {
           teamId: 'team-1',
           teamName: 'Команда',
           startDate: '2024-01-15T10:00:00Z',
           trackerName: 'tracker1',
           trackerFullName: 'Трекер',
           status: 'COMPLETED_AS_NOT_HAPPENED',
           teamStatus: null,
         },
       ],
     };
     requests.fetchMeetingReport.mockResolvedValue({
       ok: true,
       json: async () => mockNotHappened,
     });

     renderComponent();

     await waitFor(() => {
       expect(screen.getByText('Не состоялась')).toBeInTheDocument();
     });
   });

   test('отображает статус "Всё ок" с зеленым классом', async () => {
     const mockOk = {
       content: [
         {
           teamId: 'team-1',
           teamName: 'Команда',
           startDate: '2024-01-15T10:00:00Z',
           trackerName: 'tracker1',
           trackerFullName: 'Трекер',
           status: 'COMPLETED',
           teamStatus: 'OK',
         },
       ],
     };
     requests.fetchMeetingReport.mockResolvedValue({
       ok: true,
       json: async () => mockOk,
     });

     renderComponent();

     await waitFor(() => {
       const statusCell = screen.getByText('Всё ок');
       expect(statusCell).toHaveClass('mrep-status-green');
     });
   });
 });