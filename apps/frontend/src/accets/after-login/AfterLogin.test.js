import React from "react";
import { render, waitFor } from "@testing-library/react";
import { useNavigate } from "react-router-dom";
import AfterLogin from "./AfterLogin";
import loginService from "../../services/login-service";
import api from "../../services/api";

jest.mock("../../services/api", () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn(),
    interceptors: {
      response: { use: jest.fn() },
    },
  },
}));

jest.mock("react-router-dom", () => ({
  useNavigate: jest.fn(),
}));

jest.mock("../../services/login-service");

describe("AfterLogin", () => {
  let mockNavigate;
  let mockGetUserInfo;

  beforeEach(() => {
    mockNavigate = jest.fn();
    useNavigate.mockReturnValue(mockNavigate);

    mockGetUserInfo = jest.fn();
    loginService.mockReturnValue({ getUserInfo: mockGetUserInfo });

    // Мок для CSRF-запроса
    api.get.mockResolvedValue({
      data: {
        token: "test-csrf-token",
        headerName: "X-CSRF-TOKEN",
      },
    });

    localStorage.clear();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  const scenarios = [
    { roles: ["ADMIN"], expectedPath: "/streams" },
    { roles: ["TRACKER"], expectedPath: "/team-cards" },
    { roles: ["SUPER_ADMIN"], expectedPath: "/streams" },
    { roles: ["USER"], expectedPath: "/home" },
  ];

  scenarios.forEach(({ roles, expectedPath }) => {
    it(`navigates to ${expectedPath} for roles ${roles}`, async () => {
      mockGetUserInfo.mockResolvedValue({ roles });

      render(<AfterLogin />);

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith(expectedPath);
      });
    });
  });

  it("logs an error if CSRF token fetch fails", async () => {
    const error = new Error("CSRF fetch failed");
    api.get.mockRejectedValueOnce(error);
    console.error = jest.fn();

    render(<AfterLogin />);

    await waitFor(() => {
      expect(console.error).toHaveBeenCalledWith("Error fetching CSRF token:", error);
    });
  });
});
