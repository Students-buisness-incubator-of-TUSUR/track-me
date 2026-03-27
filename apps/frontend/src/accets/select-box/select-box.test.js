import React from "react";
import { render, screen } from "@testing-library/react";
import '@testing-library/jest-dom';
import SelectBox from "./select-box";

describe("SelectBox", () => {
  it("renders a select element", () => {
    render(<SelectBox />);
    expect(screen.getByRole("combobox")).toBeInTheDocument();
  });

  it("applies custom className", () => {
    const { container } = render(<SelectBox className="custom" />);
    expect(container.firstChild).toHaveClass("select-box_container", "custom");
  });

  it("renders children as options", () => {
    render(
      <SelectBox>
        <option value="a">A</option>
        <option value="b">B</option>
      </SelectBox>
    );
    expect(screen.getByRole("option", { name: "A" })).toBeInTheDocument();
    expect(screen.getByRole("option", { name: "B" })).toBeInTheDocument();
  });

  it("passes additional props to select", () => {
    render(<SelectBox data-testid="sel" disabled />);
    const select = screen.getByTestId("sel");
    expect(select).toBeDisabled();
  });
});
