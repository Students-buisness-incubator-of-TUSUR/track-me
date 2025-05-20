import React from "react";
import {render, waitFor} from "@testing-library/react";
import {useNavigate} from "react-router-dom";
import AfterLogin from "./AfterLogin";
import loginService from "../../services/login-service";

jest.mock("axios", () => ({
    __esModule: true,
    default: {
        get: jest.fn(),
        create: jest.fn().mockReturnValue({get: jest.fn()}),
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
        loginService.mockReturnValue({getUserInfo: mockGetUserInfo});
    });

    const scenarios = [
        {roles: ["ADMIN"], expectedPath: "/streams"},
        {roles: ["TRACKER"], expectedPath: "/team-cards"},
        {roles: ["SUPER_ADMIN"], expectedPath: "/streams"},
        {roles: ["USER"], expectedPath: "/home"},
    ];

    test.each(scenarios)(
        "when roles=%j navigates to %s",
        async ({roles, expectedPath}) => {
            mockGetUserInfo.mockResolvedValueOnce({roles});
            render(<AfterLogin/>);
            await waitFor(() =>
                expect(mockNavigate).toHaveBeenCalledWith(expectedPath)
            );
        }
    );

    it("logs an error if getUserInfo fails", async () => {
        const error = new Error("Login failed");
        mockGetUserInfo.mockRejectedValueOnce(error);
        console.error = jest.fn();

        render(<AfterLogin/>);
        await waitFor(() =>
            expect(console.error).toHaveBeenCalledWith("Error during login:", error)
        );
    });
});