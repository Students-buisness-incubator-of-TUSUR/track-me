import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import InputBox from './input-box';

jest.mock('../../files/pen.svg', () => ({
    ReactComponent: () => null
}));

describe('InputBox', () => {
    it('shows label-inside with placeholder when placeholderIsAbove is false', () => {
        render(<InputBox
            onChange={() => { }}
            placeholder="placeholder"
            placeholderIsAbove={false}
        />);

        expect(screen.getByTestId('label-inside')).toBeInTheDocument();
        expect(screen.queryByTestId('label-above')).not.toBeInTheDocument();
        expect(screen.getByTestId('label-inside')).toHaveTextContent('placeholder');
    });

    it('shows label-above with placeholder when placeholderIsAbove is true', () => {
        render(<InputBox
            onChange={() => { }}
            placeholder="placeholder"
            placeholderIsAbove={true}
        />);

        expect(screen.getByTestId('label-above')).toBeInTheDocument();
        expect(screen.queryByTestId('label-inside')).not.toBeInTheDocument();
        expect(screen.getByTestId('label-above')).toHaveTextContent('placeholder');
    });

    it('does not show error-text when errorText is empty string and value exists', () => {
        render(<InputBox
            onChange={() => { }}
            errorText=""
            value="value"
        />);

        const errorText = screen.getByTestId('error-text');
        expect(errorText).toBeInTheDocument();
        expect(errorText).toHaveTextContent('');
    });

    it('shows empty error-text when not focused, has error, but value is empty', () => {
        render(<InputBox
            onChange={() => { }}
            errorText="error"
            value=""
        />);

        const errorText = screen.getByTestId('error-text');
        expect(errorText).toBeInTheDocument();
        expect(errorText).toHaveTextContent('');
    });

    it('shows error text when focused with error and empty value', () => {
        const { container } = render(<InputBox
            onChange={() => { }}
            errorText="error"
            value=""
        />);
        const input = screen.getByTestId('input');

        fireEvent.focus(input);

        const errorText = screen.getByTestId('error-text');
        expect(errorText).toBeInTheDocument();
        expect(errorText).toHaveTextContent('error');
    });

    it('shows error text when not focused with error and value exists', () => {
        render(<InputBox
            onChange={() => { }}
            errorText="error"
            value="value"
        />);

        const errorText = screen.getByTestId('error-text');
        expect(errorText).toBeInTheDocument();
        expect(errorText).toHaveTextContent('error');
    });

    it('shows error text when focused with error and value exists', () => {
        const { container } = render(<InputBox
            onChange={() => { }}
            errorText="error"
            value="value"
        />);
        const input = screen.getByTestId('input');

        fireEvent.focus(input);

        const errorText = screen.getByTestId('error-text');
        expect(errorText).toBeInTheDocument();
        expect(errorText).toHaveTextContent('error');
    });

    it('does not render edit-btn when onEditClick is null', () => {
        render(<InputBox
            onChange={() => { }}
            onEditClick={null}
        />);

        expect(screen.queryByTestId('edit-btn')).not.toBeInTheDocument();
    });

    it('renders edit-btn and calls onEditClick when clicked', () => {
        const mockOnEditClick = jest.fn();
        render(<InputBox
            onChange={() => { }}
            onEditClick={mockOnEditClick}
        />);

        const editBtn = screen.getByTestId('edit-btn');
        expect(editBtn).toBeInTheDocument();

        fireEvent.click(editBtn);
        expect(mockOnEditClick).toHaveBeenCalledTimes(1);
    });
});

