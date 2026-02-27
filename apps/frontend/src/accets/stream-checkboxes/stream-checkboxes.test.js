import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import StreamCheckboxes from './stream-checkboxes';

const mockCheckboxesData = [
  { id: '1', name: 'Market 1', displayName: 'Market One' },
  { id: '2', name: 'Market 2' },
  { id: '3', name: 'Market 3' },
];

const defaultProps = {
  checkboxesRef: React.createRef(),
  handleShowCheckboxes: jest.fn(),
  showCheckboxes: false,
  checkboxesData: mockCheckboxesData,
  selectedCheckboxes: [],
  handleCheckboxChange: jest.fn(),
};

describe('StreamCheckboxes', () => {
  it('renders checkboxes list when showCheckboxes is true', () => {
    render(<StreamCheckboxes {...defaultProps} showCheckboxes={true} />);
    expect(screen.getAllByRole('checkbox')).toHaveLength(3);
  });

  it('calls handleShowCheckboxes on button click', async () => {
    render(<StreamCheckboxes {...defaultProps} />);
    const button = screen.getByRole('button');
    fireEvent.click(button);
    expect(defaultProps.handleShowCheckboxes).toHaveBeenCalledTimes(1);
  });

  it('calls handleShowCheckboxes on Enter key press', async () => {
    render(<StreamCheckboxes {...defaultProps} />);
    const button = screen.getByRole('button');
    fireEvent.keyDown(button, { key: 'Enter' });
    expect(defaultProps.handleShowCheckboxes).toHaveBeenCalledTimes(1);
  });

  it('calls handleShowCheckboxes on Space key press', async () => {
    render(<StreamCheckboxes {...defaultProps} />);
    const button = screen.getByRole('button');
    fireEvent.keyDown(button, { key: ' ' });
    expect(defaultProps.handleShowCheckboxes).toHaveBeenCalledTimes(1);
  });

  it('renders checkboxes as checked if id in selectedCheckboxes', () => {
    render(
      <StreamCheckboxes
        {...defaultProps}
        showCheckboxes={true}
        selectedCheckboxes={['1']}
      />
    );
    expect(screen.getAllByRole('checkbox')[0]).toBeChecked();
    expect(screen.getAllByRole('checkbox')[1]).not.toBeChecked();
  });

  it('calls handleCheckboxChange when checkbox is clicked', async () => {
    render(<StreamCheckboxes {...defaultProps} showCheckboxes={true} />);
    const checkbox = screen.getAllByRole('checkbox')[0];
    fireEvent.click(checkbox);
    expect(defaultProps.handleCheckboxChange).toHaveBeenCalledWith('1');
  });
});
