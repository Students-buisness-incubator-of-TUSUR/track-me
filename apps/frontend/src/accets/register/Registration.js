import React, { useState } from 'react';
import './Registration.css';
import axios from 'axios';

const Registration = () => {
  const [form, setForm] = useState({
    fullName: '',
    email: '',
    phoneNumber: '',
    telegramId: '',
    password: '',
    confirmPassword: '',
    role: '',
  });

  const handleChange = (e) => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (form.password !== form.confirmPassword) {
      alert('Пароли не совпадают!');
      return;
    }

    // Разделяем ФИО
    const [lastName = '', firstName = '', middleName = ''] = form.fullName.split(' ');

    const userData = {
      firstName,
      lastName,
      middleName,
      phoneNumber: form.phoneNumber,
      telegramId: form.telegramId,
      email: form.email,
      password: form.password,
      role: form.role,
    };

    try {
      const response = await axios.post(`${process.env.REACT_APP_BACKEND_HOST}/api/v1/auth/sing-up`, userData, {
        headers: {
          'Content-Type': 'application/json',
        },
      });

      console.log('User registered successfully:', response.data);
      alert('Регистрация прошла успешно!');
    } catch (error) {
      console.error('Registration failed:', error.response?.data || error.message);
      if (error.response) {
        alert(`Ошибка при регистрации: ${error.response.data.message || error.response.statusText}`);
      } else {
        alert('Ошибка при отправке запроса');
      }
    }
  };

  return (
    <div className="registration-container">
      <h2 className="registration-title">Регистрация</h2>
      <form className="registration-form" onSubmit={handleSubmit}>
        <div className="input-group fio-input">
          <label>ФИО</label>
          <input type="text" name="fullName" value={form.fullName} onChange={handleChange} className="input-field" required />
        </div>

        <div className="input-group email-input">
          <label>E-mail</label>
          <input type="email" name="email" value={form.email} onChange={handleChange} className="input-field" required />
        </div>

        <div className="input-group telephone-input">
          <label>Телефон</label>
          <input type="tel" name="phoneNumber" value={form.phoneNumber} onChange={handleChange} className="input-field" required />
        </div>

        <div className="input-group telegram-input">
          <label>Имя пользователя Telegram</label>
          <input type="text" name="telegramId" value={form.telegramId} onChange={handleChange} className="input-field" />
        </div>

        <div className="input-group password-input">
          <label>Пароль</label>
          <input type="password" name="password" value={form.password} onChange={handleChange} className="input-field" required />
        </div>

        <div className="input-group confirm-password-input">
          <label>Повторите пароль</label>
          <input type="password" name="confirmPassword" value={form.confirmPassword} onChange={handleChange} className="input-field" required />
        </div>

        <div className="input-group role-select">
          <select name="role" value={form.role} onChange={handleChange} className="input-field" required>
            <option value="">Выберите роль</option>
            <option value="TRACKER">Трекер</option>
            <option value="ADMIN">Администратор</option>
          </select>
        </div>

        <button type="submit" className="register-button">Зарегистрироваться</button>
      </form>
    </div>
  );
};

export default Registration;
