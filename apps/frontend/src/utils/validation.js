export const isValidUsername = (username) => {
  if (!username || typeof username !== 'string') return false;
  const usernameRegex = /^[a-zA-Z0-9._@\-\s]+$/;
  return username.length >= 1 && username.length <= 255 && usernameRegex.test(username);
};