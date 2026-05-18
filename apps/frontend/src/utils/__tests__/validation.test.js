// src/utils/__tests__/validation.test.js
import { isValidUsername } from '../validation';

describe('isValidUsername', () => {
  // null/undefined — 2 условия
  test('returns false for null', () => {
    expect(isValidUsername(null)).toBe(false);
  });
  test('returns false for undefined', () => {
    expect(isValidUsername(undefined)).toBe(false);
  });

  // не строка — 1 условие
  test('returns false for non-string', () => {
    expect(isValidUsername(123)).toBe(false);
  });

  // length < 1 — 1 условие
  test('returns false for empty string', () => {
    expect(isValidUsername('')).toBe(false);
  });

  // length > 255 — 1 условие
  test('returns false for too long string', () => {
    expect(isValidUsername('a'.repeat(256))).toBe(false);
  });

  // regex не подходит — 1 условие
  test('returns false for invalid characters', () => {
    // Valid characters
    expect(isValidUsername('user@name')).toBe(true);
    expect(isValidUsername('user.name')).toBe(true);
    expect(isValidUsername('user-name')).toBe(true);
    expect(isValidUsername('user name')).toBe(true);
    
    // Invalid characters - should return false
    expect(isValidUsername('user$name')).toBe(false);
    expect(isValidUsername('user/name')).toBe(false);
    expect(isValidUsername('user#name')).toBe(false);
    expect(isValidUsername('user&name')).toBe(false);
  });

  // успешный — 1 условие
  test('returns true for valid username', () => {
    expect(isValidUsername('tracker')).toBe(true);
    expect(isValidUsername('admin')).toBe(true);
  });
});
