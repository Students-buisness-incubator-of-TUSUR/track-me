import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import CustomSelect from "./CustomSelect";
import "@testing-library/jest-dom";

describe("CustomSelect component", () => {
  let onChangeMock, onCustomChangeMock, setShowCustomInputMock;

  beforeEach(() => {
    onChangeMock = jest.fn();
    onCustomChangeMock = jest.fn();
    setShowCustomInputMock = jest.fn();
  });

  const renderComponent = (props = {}) =>
    render(
      <CustomSelect
        value=""
        onChange={onChangeMock}
        options={["5", "10"]}
        customValue=""
        onCustomChange={onCustomChangeMock}
        showCustomInput={false}
        setShowCustomInput={setShowCustomInputMock}
        {...props}
      />
    );

  test("toggles dropdown on click (lines 19–26)", () => {
    renderComponent();
    const select = screen.getByRole("button");
    fireEvent.click(select);
    expect(screen.getByText("Свое значение")).toBeInTheDocument();
  });

  test("toggles dropdown on Enter key (lines 19–26)", () => {
    renderComponent();
    const select = screen.getByRole("button");
    fireEvent.keyDown(select, { key: "Enter" });
    expect(screen.getByText("Свое значение")).toBeInTheDocument();
  });

  test("handleOptionClick with normal option (lines 30–32)", () => {
    renderComponent();
    fireEvent.click(screen.getByRole("button"));
    fireEvent.click(screen.getByText("5"));
    expect(onChangeMock).toHaveBeenCalledWith({ target: { value: "5" } });
    expect(setShowCustomInputMock).toHaveBeenCalledWith(false);
  });

  test("handleOptionClick with custom option (lines 30–32)", () => {
    renderComponent();
    fireEvent.click(screen.getByRole("button"));
    fireEvent.click(screen.getByText("Свое значение"));
    expect(onChangeMock).toHaveBeenCalledWith({ target: { value: "custom" } });
    expect(setShowCustomInputMock).toHaveBeenCalledWith(true);
  });

  test("renders custom input when showCustomInput is true (lines 53–122)", () => {
    renderComponent({ showCustomInput: true, customValue: "7" });
    // теперь проверяем число, а не строку
    expect(screen.getByPlaceholderText("Введите число")).toHaveValue(7);
  });

  test("handleCustomInputChange allows valid number (lines 38–39)", () => {
    renderComponent({ showCustomInput: true });
    const input = screen.getByPlaceholderText("Введите число");
    fireEvent.change(input, { target: { value: "10" } });
    expect(onCustomChangeMock).toHaveBeenCalledWith({ target: { value: "10" } });
  });

  test("handleCustomInputChange ignores invalid number (lines 38–39)", () => {
    renderComponent({ showCustomInput: true });
    const input = screen.getByPlaceholderText("Введите число");
    fireEvent.change(input, { target: { value: "abc" } });
    expect(onCustomChangeMock).not.toHaveBeenCalled();
  });

  test("renders dropdown options correctly (lines 53–122)", () => {
    renderComponent();
    fireEvent.click(screen.getByRole("button"));
    const options = screen.getAllByRole("option");
    expect(options.map((o) => o.textContent)).toEqual([
      "Выберите количество",
      "5",
      "10",
      "Свое значение",
    ]);
  });

  test("option is selectable via keyboard (lines 53–122)", () => {
    renderComponent();
    fireEvent.click(screen.getByRole("button"));
    const option = screen.getByText("10");
    fireEvent.keyDown(option, { key: "Enter" });
    expect(onChangeMock).toHaveBeenCalledWith({ target: { value: "10" } });
  });

  test("closes dropdown on outside click (lines 53–122)", () => {
    renderComponent();
    fireEvent.click(screen.getByRole("button"));
    expect(screen.getByText("Свое значение")).toBeInTheDocument();
    fireEvent.mouseDown(document.body);
    expect(screen.queryByText("Свое значение")).not.toBeInTheDocument();
  });
});
describe("CustomSelect keyboard navigation", () => {
  const options = ["5", "10", "15"];
  let onChange, onCustomChange, setShowCustomInput;

  beforeEach(() => {
    onChange = jest.fn();
    onCustomChange = jest.fn();
    setShowCustomInput = jest.fn();
  });

  it("выбирает пустое значение по Enter", () => {
    render(
      <CustomSelect
        value="10"
        onChange={onChange}
        options={options}
        customValue=""
        onCustomChange={onCustomChange}
        showCustomInput={false}
        setShowCustomInput={setShowCustomInput}
      />
    );

    // открыть дропдаун
    fireEvent.click(screen.getByRole("button", { name: /выбрать количество встреч/i }));

    const emptyOption = screen.getByRole("option", { name: "Выберите количество" });

    fireEvent.keyDown(emptyOption, { key: "Enter" });

    expect(onChange).toHaveBeenCalledWith({ target: { value: "" } });
  });

  it("выбирает кастомное значение по пробелу", () => {
    render(
      <CustomSelect
        value=""
        onChange={onChange}
        options={options}
        customValue=""
        onCustomChange={onCustomChange}
        showCustomInput={false}
        setShowCustomInput={setShowCustomInput}
      />
    );

    fireEvent.click(screen.getByRole("button", { name: /выбрать количество встреч/i }));

    const customOption = screen.getByRole("option", { name: "Свое значение" });

    fireEvent.keyDown(customOption, { key: " " });

    expect(onChange).toHaveBeenCalledWith({ target: { value: "custom" } });
    expect(setShowCustomInput).toHaveBeenCalledWith(true);
  });
});