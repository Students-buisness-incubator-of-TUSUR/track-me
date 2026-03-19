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

export async function fetchTrackers({ page, size, sort, filters }) {
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
        filters: filters ?? [],
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

export async function fetchUserInfo({ username = null }) {
  const endpoint = username === null ?
    `${backendURLSSO}/api/v1/account/info` :
    `${backendURLSSO}/api/v1/users/${username}/info`;
  const response = await fetch(
    endpoint,
    {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...getCsrfConfigForFetch(),
      },
      credentials: "include",
    }
  );

  return response;
}

export async function fetchUserPhoto({ username = null }) {
  const endpoint = username === null ?
    `${backendURLSSO}/api/v1/account/photo` :
    `${backendURLSSO}/api/v1/users/${username}/photo`;
  const response = await fetch(
    endpoint,
    {
      method: "GET",
      headers: {
        ...getCsrfConfigForFetch(),
      },
      credentials: "include",
    }
  );

  return response;
}

export async function updateUserInfo({ newUserData }) {
  const response = await fetch(
    `${backendURLSSO}/api/v1/account/update`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...getCsrfConfigForFetch(),
      },
      credentials: "include",
      body: JSON.stringify(newUserData),
    }
  );

  return response;
}

export async function updateUserPhoto({ newUserPhotoFile }) {
  const formData = new FormData();
  formData.append("file", newUserPhotoFile);
  const response = await fetch(
    `${backendURLSSO}/api/v1/account/photo`,
    {
      method: "POST",
      headers: {
        ...getCsrfConfigForFetch(),
      },
      credentials: "include",
      body: formData,
    }
  );

  return response;
}
