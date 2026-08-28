export type ResearchCampaignStatus = "ACTIVE" | "PAUSED";
export type ResearchCampaignEffectiveStatus = ResearchCampaignStatus | "DISABLED" | "UNAVAILABLE";

export type ResearchSubmission = {
  id: number;
  submissionNumber: string;
  source: string;
  rating: number;
  scenes: string[];
  concern: string;
  feedback: string;
  maskedWalletAddress: string;
  createdAt: string;
};

export type ResearchSummary = {
  total: number;
  averageRating: number;
  ratingDistribution: Record<string, number>;
  sceneDistribution: Record<string, number>;
  concernDistribution: Record<string, number>;
  sourceDistribution: Record<string, number>;
};

export type ResearchCampaign = {
  status: ResearchCampaignStatus;
  effectiveStatus: ResearchCampaignEffectiveStatus;
  intakeEnabled: boolean;
  dataAvailable: boolean;
  termsVersion: string;
  updatedAt: string;
};

export type ResearchDetail = Omit<ResearchSubmission, "maskedWalletAddress"> & {
  walletNetwork: string;
  walletAddress: string;
  termsVersion: string;
  consentedAt: string;
};

export type ResearchFilters = {
  number?: string;
  rating?: number;
  concern?: string;
  source?: string;
  scene?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
};

export type ResearchListResponse = {
  submissions: ResearchSubmission[];
  total: number;
  pages: number;
};

export type ResearchBatchDeleteResponse = {
  ok: boolean;
  deleted: number;
};
