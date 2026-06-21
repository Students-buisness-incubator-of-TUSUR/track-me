import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import InputBox from './InputBox';

describe('InputBox', () => {
    const mockOnChange = jest.fn();

    beforeEach(() => {
        mockOnChange.mockClear();
    });

    test('passes props correctly to input element', () => {
        const placeholder = "errortext";
        const type = "email";
        const name = "email";
        const value = "test@example.com";
        const autoComplete = "email";
        const pattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

        render(
            <InputBox
                placeholder={placeholder}
                type={type}
                name={name}
                value={value}
                autoComplete={autoComplete}
                required
                pattern={pattern}
                onChange={mockOnChange}
            />
        );

        const input = screen.getByRole('textbox');
        expect(screen.getByLabelText(placeholder)).toBeInTheDocument();
        expect(input).toHaveAttribute('type', type);
        expect(input).toHaveAttribute('name', name);
        expect(input).toHaveAttribute('value', value);
        expect(input).toHaveAttribute('autoComplete', autoComplete);
        expect(input).toHaveAttribute('required');
        expect(input).toHaveAttribute('pattern', pattern);
    });

    test('calls onChange handler when input changes', () => {
        render(
            <InputBox
                placeholder="Test"
                type="text"
                name="test"
                value=""
                onChange={mockOnChange}
            />
        );

        const input = screen.getByRole('textbox');
        fireEvent.change(input, { target: { value: 'new value' } });

        expect(mockOnChange).toHaveBeenCalledTimes(1);
    });

    test('shows error text when focused', () => {
        const errorText = 
        render(
            <InputBox
                placeholder="Test"
                type="text"
                name="test"
                value=""
                onChange={mockOnChange}
                errorText="errortext"
            />
        );

        const input = screen.getByRole('textbox');
        expect(screen.queryByText('errortext')).not.toBeInTheDocument();

        fireEvent.focus(input);
        expect(screen.queryByText('errortext')).toBeInTheDocument();

        fireEvent.blur(input);
        expect(screen.queryByText('errortext')).not.toBeInTheDocument();
    });

    test('shows error text when has value', () => {
        render(
            <InputBox
                placeholder="Test"
                type="text"
                name="test"
                value="some value"
                onChange={mockOnChange}
                errorText="errortext"
            />
        );

        const errorSpan = screen.getByText("errortext");
        expect(errorSpan).toBeVisible();
    });

    test('hides error text when empty and not focused', () => {
        render(
            <InputBox
                placeholder="Test"
                type="text"
                name="test"
                value=""
                onChange={mockOnChange}
                errorText="errortext"
            />
        );

        expect(screen.queryByText("errortext")).not.toBeInTheDocument();
    });
});

