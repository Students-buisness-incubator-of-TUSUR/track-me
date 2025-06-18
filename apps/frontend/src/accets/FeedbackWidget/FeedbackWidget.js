import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import './FeedbackWidget.css';

const FeedbackWidget = () => {
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [userData, setUserData] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);

  const backendHost = (process.env.REACT_APP_BACKEND_URI || 'http://localhost:8080') + '/sso';

  useEffect(() => {
    const fetchUserData = async () => {
      setIsLoading(true);
      try {
        const response = await fetch(`${backendHost}/api/v1/account/info`, {
          method: 'GET',
          credentials: 'include',
          headers: {
            'Accept': 'application/json',
          },
        });

        if (!response.ok) {
          throw new Error('Ошибка при получении данных пользователя');
        }

        const data = await response.json();
        setUserData(data);
      } catch (err) {
        console.error('Ошибка:', err);
        setError(err.message);
        
        // Fallback к localStorage, если API запрос не удался
        const savedUser = localStorage.getItem('user');
        if (savedUser) {
          setUserData(JSON.parse(savedUser));
        }
      } finally {
        setIsLoading(false);
      }
    };

    fetchUserData();
  }, [backendHost]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const form = e.target;
    try {
      const formData = new FormData(form);
      
      if (userData) {
        formData.append('Username', userData.username || '');
        formData.append('Почта', userData.email || '');
        formData.append('ФИО', userData.fullName || '');
      }

      const response = await fetch(form.action, {
        method: 'POST',
        body: formData,
        headers: { 'Accept': 'application/json' },
      });
      
      if (response.ok) {
        setIsSubmitted(true);
        form.reset();
        setTimeout(() => {
          setIsOpen(false);
          setIsSubmitted(false);
        }, 2000);
      } else {
        alert('Ошибка при отправке сообщения.');
      }
    } catch (error) {
      console.error('Ошибка:', error);
      alert('Произошла ошибка. Попробуйте позже.');
    }
  };

  if (['/admin', '/superadmin'].includes(location.pathname)) return null;

  return (
    <div className="feedback-widget">
      {!isOpen && (
        <button className="feedback-toggle" onClick={() => setIsOpen(true)}>
          Обратная связь
        </button>
      )}
      <div className={`feedback-form-container ${isOpen ? '' : 'hidden'}`}>
        <div className="feedback-form">
          <button className="feedback-close" onClick={() => setIsOpen(false)}>
            ×
          </button>
          {isSubmitted ? (
            <div className="success-message">Спасибо за ваш отзыв!</div>
          ) : isLoading ? (
            <div className="loading-message">Загрузка данных...</div>
          ) : error ? (
            <div className="error-message">{error}</div>
          ) : (
            <form action="https://formspree.io/f/xblyydjj" method="POST" onSubmit={handleSubmit}>
              <h3>Помогите нам стать лучше</h3>
              
              <div className="form-content">
                <label htmlFor="design-rating">1. Оцените дизайн сервиса (1 - очень плохо, 5 - отлично):</label>
                <select id="design-rating" name="Дизайн сервиса" className="feedback-select" required>
                  <option value="">Выберите оценку</option>
                  <option value="1">1</option>
                  <option value="2">2</option>
                  <option value="3">3</option>
                  <option value="4">4</option>
                  <option value="5">5</option>
                </select>
                
                <label htmlFor="usability-rating">2. Оцените удобство использования (1 - очень плохо, 5 - отлично):</label>
                <select id="usability-rating" name="Удобство использования" className="feedback-select" required>
                  <option value="">Выберите оценку</option>
                  <option value="1">1</option>
                  <option value="2">2</option>
                  <option value="3">3</option>
                  <option value="4">4</option>
                  <option value="5">5</option>
                </select>
                
                <label htmlFor="bugs">3. Укажите количество багов, с которыми вы столкнулись:</label>
                <input 
                  type="number" 
                  id="bugs" 
                  name="Количество багов" 
                  className="feedback-input" 
                  min="0" 
                  placeholder="0" 
                />
                
                <label htmlFor="liked">4. Что вам больше всего понравилось:</label>
                <textarea 
                  id="liked" 
                  name="Что понравилось" 
                  className="feedback-textarea" 
                  placeholder="Опишите, что вам понравилось..."
                />
                
                <label htmlFor="disliked">5. Что вам меньше всего понравилось:</label>
                <textarea 
                  id="disliked" 
                  name="Что не понравилось" 
                  className="feedback-textarea" 
                  placeholder="Опишите, что можно улучшить..."
                />
                
                <label htmlFor="improvements">6. Ваши рекомендации по улучшению:</label>
                <textarea 
                  id="improvements" 
                  name="Рекомендации" 
                  className="feedback-textarea" 
                  placeholder="Ваши идеи по улучшению сервиса..."
                />
              </div>
              
              <button type="submit" className="feedback-submit">Отправить отзыв</button>
            </form>
          )}
        </div>
      </div>
    </div>
  );
};

export default FeedbackWidget;