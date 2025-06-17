import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import './FeedbackWidget.css';

const FeedbackWidget = () => {
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);
  const [isSubmitted, setIsSubmitted] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    const form = e.target;
    try {
      const response = await fetch(form.action, {
        method: 'POST',
        body: new FormData(form),
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
      <div className={`feedback-form ${isOpen ? '' : 'hidden'}`}>
        <button className="feedback-close" onClick={() => setIsOpen(false)}>
          ×
        </button>
        {isSubmitted ? (
          <div className="success-message">Сообщение отправлено!</div>
        ) : (
          <form action="https://formspree.io/f/xblyydjj" method="POST" onSubmit={handleSubmit}>
            <h3>Напишите нам</h3>
            <label htmlFor="name">Имя:</label>
            <input type="text" id="name" name="name" className="feedback-input" required />
            <label htmlFor="email">Email:</label>
            <input type="email" id="email" name="email" className="feedback-input" required />
            <label htmlFor="message">Сообщение:</label>
            <textarea id="message" name="message" className="feedback-textarea" required />
            <button type="submit" className="feedback-submit">Отправить</button>
          </form>
        )}
      </div>
    </div>
  );
};

export default FeedbackWidget;