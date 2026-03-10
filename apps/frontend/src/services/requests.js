import { getCsrfConfigForFetch } from "../utils/csrf-utils";
import { backendURLBackend, backendURLSSO } from "./constants";

export async function fetchReports({ page, size, filters }) {
  const response = await fetch(
    `${backendURLBackend}/api/v1/team-cards/reports?page=${page}&size=${size}`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...getCsrfConfigForFetch(),
      },
      credentials: "include",
      body: JSON.stringify({
        filters: filters ?? []
      }),
    }
  );

  return response;
}

export async function fetchTrackers({ page, size, sort }) {
  let sortString;
  if (Array.isArray(sort) && sort.length !== 0) {
    sortString = "sort=" + sort.join("&sort=")
  } else {
    sortString = "sort=username,asc";
  }
  const response = await fetch(
    `${backendURLSSO}/api/v1/users/trackers?page=${page}&size=${size}&${sortString}`,
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

export async function fetchStreams({ page, size, sort }) {
  let sortString;
  if (Array.isArray(sort) && sort.length !== 0) {
    sortString = "sort=" + sort.join("&sort=")
  } else {
    sortString = "sort=name,asc";
  }
  const response = await fetch(
    `${backendURLBackend}/api/v1/streams?page=${page}&size=${size}&${sortString}`,
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

export async function fetchTeams({ page, size, filters, admin = false }) {
  const response = await fetch(
    `${backendURLBackend}/api/v1/${admin ? "admin/" : ""}team-cards?page=${page}&size=${size}`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...getCsrfConfigForFetch(),
      },
      credentials: "include",
      body: JSON.stringify({
        filters: filters ?? []
      }),
    }
  );

  return response;
}
