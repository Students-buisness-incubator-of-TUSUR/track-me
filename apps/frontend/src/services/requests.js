import { getCsrfConfigForFetch } from "../utils/csrf-utils";

export async function fetchReports(page, size) {
  const response = await fetch(
    `https://api.trackme.test.startup-poligon.com/backend/api/v1/team-cards/reports?page=${page}&size=${size}`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...getCsrfConfigForFetch(),
      },
      credentials: "include",
      body: JSON.stringify({
        filters: []
        // TODO: add filters to request
        // filters: selectedFilters 
      }),
    }
  );

  return response;
}
