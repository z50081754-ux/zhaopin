export type IOSWalletDetection = {
  isIOS: boolean;
  detectedWallets: string[];
};

const TARGET_WALLETS = [
  "Bitpie",
  "Trust Wallet",
  "Solflare",
  "MetaMask",
  "Ronin Wallet",
  "Phantom",
  "Exodus",
  "Bitget Wallet",
  "imToken",
] as const;

type WalletName = (typeof TARGET_WALLETS)[number];

function isIOSDevice() {
  const ua = navigator.userAgent || "";
  return /iPhone|iPad|iPod/i.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
}

function addByText(target: Set<WalletName>, text: unknown) {
  const value = String(text || "").toLowerCase();
  if (!value) return;
  if (/bitpie/.test(value)) target.add("Bitpie");
  if (/trust\s*wallet|trustwallet/.test(value)) target.add("Trust Wallet");
  if (/solflare/.test(value)) target.add("Solflare");
  if (/metamask/.test(value)) target.add("MetaMask");
  if (/ronin/.test(value)) target.add("Ronin Wallet");
  if (/phantom/.test(value)) target.add("Phantom");
  if (/exodus/.test(value)) target.add("Exodus");
  if (/bitkeep|bitget/.test(value)) target.add("Bitget Wallet");
  if (/imtoken/.test(value)) target.add("imToken");
}

function inspectProvider(target: Set<WalletName>, provider: any) {
  if (!provider || typeof provider !== "object") return;
  if (provider.isMetaMask === true) target.add("MetaMask");
  if (provider.isTrust === true || provider.isTrustWallet === true) target.add("Trust Wallet");
  if (provider.isImToken === true || provider.isImTokenWallet === true) target.add("imToken");
  if (provider.isBitKeep === true || provider.isBitget === true || provider.isBitgetWallet === true) target.add("Bitget Wallet");
  if (provider.isRonin === true) target.add("Ronin Wallet");
  if (provider.isPhantom === true) target.add("Phantom");
  if (provider.isSolflare === true) target.add("Solflare");
  if (provider.isExodus === true) target.add("Exodus");
  if (provider.isBitpie === true) target.add("Bitpie");
  addByText(target, provider.name);
  addByText(target, provider.rdns);
}

async function detectEip6963(target: Set<WalletName>) {
  const discovered = new Set<any>();
  const handler = (event: Event) => {
    const detail = (event as CustomEvent<any>).detail;
    if (!detail?.provider || discovered.has(detail.provider)) return;
    discovered.add(detail.provider);
    inspectProvider(target, detail.provider);
    addByText(target, detail.info?.name);
    addByText(target, detail.info?.rdns);
  };
  window.addEventListener("eip6963:announceProvider", handler as EventListener);
  window.dispatchEvent(new Event("eip6963:requestProvider"));
  await new Promise<void>((resolve) => window.setTimeout(resolve, 120));
  window.removeEventListener("eip6963:announceProvider", handler as EventListener);
}

/**
 * Passive iOS wallet-environment detection only.
 * It never requests accounts, wallet connection, signatures, balances or transactions.
 * A missing result must not be interpreted as "wallet not installed" on iOS Safari.
 */
export async function detectIOSWallets(): Promise<IOSWalletDetection> {
  if (!isIOSDevice()) return { isIOS: false, detectedWallets: [] };

  const detected = new Set<WalletName>();
  const w = window as any;

  addByText(detected, navigator.userAgent);

  const ethereum = w.ethereum;
  inspectProvider(detected, ethereum);
  if (Array.isArray(ethereum?.providers)) ethereum.providers.forEach((provider: any) => inspectProvider(detected, provider));

  inspectProvider(detected, w.trustwallet);
  inspectProvider(detected, w.bitkeep?.ethereum);
  inspectProvider(detected, w.bitgetWallet?.ethereum || w.bitgetWallet);
  inspectProvider(detected, w.imToken?.ethereum || w.imToken);
  inspectProvider(detected, w.ronin?.provider || w.ronin);
  inspectProvider(detected, w.exodus?.ethereum || w.exodus);
  inspectProvider(detected, w.bitpie?.ethereum || w.bitpie);

  if (w.phantom?.solana?.isPhantom || w.solana?.isPhantom) detected.add("Phantom");
  if (w.solflare?.isSolflare) detected.add("Solflare");

  await detectEip6963(detected);

  return {
    isIOS: true,
    detectedWallets: TARGET_WALLETS.filter((wallet) => detected.has(wallet)),
  };
}
