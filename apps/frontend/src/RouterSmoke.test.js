import {MemoryRouter, Route} from "react-router-dom"

test("can import router", () => {
    expect(MemoryRouter).toBeDefined();
    expect(Route).toBeDefined();
});