import React, { useState, useRef, useEffect } from 'react';
// import './create-stream-page.css';
import './CustomSelect.css';

const CustomSelect = ({
  value,
  onChange,
  options,
  customValue,
  onCustomChange,
  showCustomInput,
  setShowCustomInput,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const selectRef = useRef(null);

  const toggleDropdown = () => setIsOpen(!isOpen);

  const handleOptionClick = (option) => {
    if (option === 'custom') {
      setShowCustomInput(true);
      onChange({ target: { value: 'custom' } });
    } else {
      setShowCustomInput(false);
      onChange({ target: { value: option } });
    }
    setIsOpen(false);
  };

  const handleCustomInputChange = (e) => {
    const value = e.target.value;
    if (value === '' || (/^\d+$/.test(value) && Number(value) >= 1 && Number(value) <= 100)) {
      onCustomChange({ target: { value } });
    }
  };

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (selectRef.current && !selectRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <div className="custom-select_select-cont" ref={selectRef}>
      {!showCustomInput ? (
        <div
          className={ `custom-select_select ${isOpen ? 'custom-select_remove-above-border-radius' : '' }` }
          onClick={toggleDropdown}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              toggleDropdown();
            }
          }}
          tabIndex={0}
          role="button"
          aria-label="Выбрать количество встреч"
        >
          <span>
            {value === '' ? 'Выберите количество' : value === 'custom' ? 'Свое значение' : value}
          </span>
          <div className="custom-select_select-arrow"></div>
        </div>
      ) : (
        <input
          type="number"
          className="custom-select_custom-input"
          value={customValue}
          onChange={handleCustomInputChange}
          min="1"
          max="100"
          autoFocus
          placeholder="Введите число"
        />
      )}
      {isOpen && !showCustomInput && (
        <div className="custom-select_select-dropdown">
          <div
  className="custom-select_select-option"
  onClick={() => handleOptionClick('')}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleOptionClick('');
    }
  }}
  role="option"
  tabIndex={0}
  aria-selected={value === ''}  
>
  Выберите количество
</div>

{options.map((option) => (
  <div
    key={option}
    className="custom-select_select-option"
    onClick={() => handleOptionClick(option)}
    onKeyDown={(e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleOptionClick(option);
      }
    }}
    role="option"
    tabIndex={0}
    aria-selected={value === option}
  >
    {option}
  </div>
))}

<div
  className="custom-select_select-option"
  onClick={() => handleOptionClick('custom')}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleOptionClick('custom');
    }
  }}
  role="option"
  tabIndex={0}
  aria-selected={value === 'custom'}
>
  Свое значение
</div>
        </div>
      )}
    </div>
  );
};

export default CustomSelect;
