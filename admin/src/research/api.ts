import type {
  ResearchBatchDeleteResponse,
  ResearchCampaign,
  ResearchCampaignStatus,
  ResearchDetail,
  ResearchFilters,
  ResearchListResponse,
  ResearchSummary
} from "./types";

const researchPath = (apiBase: string, path: string) =>
  `${apiBase.replace(/\/$/, "")}/api/admin/research${path}`;

async function request<T>(apiBase: string, path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(researchPath(apiBase, path), {
    ...options,
    credentials: "include"
  });
  if (!response.ok) {
    let message = "REQUEST_FAILED";
    try {
      const body = await response.json() as { message?: string; code?: string };
      message = body.message || body.code || message;
    } catch {
      // The protected API may return an empty response for an unauthorized request.
    }
    throw new Error(message);
  }
  return response.json() as Promise<T>;
}

function searchParams(filters: ResearchFilters, includePagination: boolean): URLSearchParams {
  const params = new URLSearchParams({
    number: filters.number || "",
    rating: String(filters.rating || 0),
    concern: filters.concern || "",
    source: filters.source || "",
    scene: filters.scene || "",
    from: filters.from || "",
    to: filters.to || ""
  });
  if (includePagination) {
    params.set("page", String(filters.page || 0));
    params.set("size", String(filters.size || 20));
  }
  return params;
}

export const loadResearchCampaign = (apiBase: string) =>
  request<ResearchCampaign>(apiBase, "/campaign");

export const updateResearchCampaign = (apiBase: string, status: ResearchCampaignStatus) =>
  request<ResearchCampaign>(apiBase, "/campaign", {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status })
  });

export const loadResearchSummary = (apiBase: string) =>
  request<ResearchSummary>(apiBase, "/summary");

export const loadResearchSubmissions = (apiBase: string, filters: ResearchFilters) =>
  request<ResearchListResponse>(apiBase, `/submissions?${searchParams(filters, true)}`);

export const loadResearchDetail = (apiBase: string, id: number) =>
  request<ResearchDetail>(apiBase, `/submissions/${id}`);

export const lookupResearchWallet = (apiBase: string, walletAddress: string) =>
  request<ResearchDetail>(apiBase, "/submissions/lookup", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ walletAddress })
  });

export const deleteResearchSubmission = (apiBase: string, id: number) =>
  request<{ ok: boolean }>(apiBase, `/submissions/${id}`, { method: "DELETE" });

export const deleteResearchBatch = (apiBase: string, ids: number[]) =>
  request<ResearchBatchDeleteResponse>(apiBase, "/submissions/batch", {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ ids })
  });

export async function downloadResearchCsv(apiBase: string, filters: ResearchFilters): Promise<void> {
  const response = await fetch(researchPath(apiBase, `/submissions/export?${searchParams(filters, false)}`), {
    credentials: "include"
  });
  if (!response.ok) {
    throw new Error("EXPORT_FAILED");
  }
  const url = URL.createObjectURL(await response.blob());
  try {
    const link = document.createElement("a");
    link.href = url;
    link.download = "web3-wallet-research.csv";
    link.click();
  } finally {
    URL.revokeObjectURL(url);
  }
}
