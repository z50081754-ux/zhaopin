export type IOSWalletDetection = {
  isIOS: boolean;
  detectedWallets: string[];
};

const TARGET_WALLETS = [
  "Bitpie",
  "Trust Wallet",
  "Uniswap Wallet",
  "Phantom",
  "Coin98",
  "Solflare",
  "Bitget Wallet",
  "MyTonWallet",
  "Tonhub",
  "Tonkeeper",
  "TokenPocket",
  "TronLink",
  "MetaMask",
  "OKX Wallet",
  "Exodus",
  "Ronin Wallet",
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
  if (/trust\s*wallet|trustwallet|com\.wallet\.crypto\.trustapp/.test(value)) target.add("Trust Wallet");
  if (/uniswap/.test(value)) target.add("Uniswap Wallet");
  if (/phantom/.test(value)) target.add("Phantom");
  if (/coin98|coin98wallet/.test(value)) target.add("Coin98");
  if (/solflare/.test(value)) target.add("Solflare");
  if (/bitkeep|bitget/.test(value)) target.add("Bitget Wallet");
  if (/mytonwallet|myton wallet/.test(value)) target.add("MyTonWallet");
  if (/tonhub/.test(value)) target.add("Tonhub");
  if (/tonkeeper/.test(value)) target.add("Tonkeeper");
  if (/tokenpocket|token pocket/.test(value)) target.add("TokenPocket");
  if (/tronlink/.test(value)) target.add("TronLink");
  if (/metamask/.test(value)) target.add("MetaMask");
  if (/okxwallet|okx wallet|com\.okex\.wallet/.test(value)) target.add("OKX Wallet");
  if (/exodus/.test(value)) target.add("Exodus");
  if (/ronin/.test(value)) target.add("Ronin Wallet");
  if (/imtoken/.test(value)) target.add("imToken");
}

function inspectProvider(target: Set<WalletName>, provider: any) {
  if (!provider || typeof provider !== "object") return;

  if (provider.isMetaMask === true) target.add("MetaMask");
  if (provider.isTrust === true || provider.isTrustWallet === true) target.add("Trust Wallet");
  if (provider.isUniswapWallet === true || provider.isUniswap === true) target.add("Uniswap Wallet");
  if (provider.isPhantom === true) target.add("Phantom");
  if (provider.isCoin98 === true || provider.isCoin98Wallet === true) target.add("Coin98");
  if (provider.isSolflare === true) target.add("Solflare");
  if (provider.isBitKeep === true || provider.isBitget === true || provider.isBitgetWallet === true) target.add("Bitget Wallet");
  if (provider.isTokenPocket === true || provider.isTokenPocketWallet === true) target.add("TokenPocket");
  if (provider.isTronLink === true) target.add("TronLink");
  if (provider.isOkxWallet === true || provider.isOKExWallet === true || provider.isOKXWallet === true) target.add("OKX Wallet");
  if (provider.isExodus === true) target.add("Exodus");
  if (provider.isRonin === true) target.add("Ronin Wallet");
  if (provider.isImToken === true || provider.isImTokenWallet === true) target.add("imToken");
  if (provider.isBitpie === true) target.add("Bitpie");

  addByText(target, provider.name);
  addByText(target, provider.rdns);
  addByText(target, provider.walletName);
  addByText(target, provider.constructor?.name);
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
  await new Promise<void>((resolve) => window.setTimeout(resolve, 180));
  window.removeEventListener("eip6963:announceProvider", handler as EventListener);
}

async function detectTip6963(target: Set<WalletName>) {
  const handler = (event: Event) => {
    const detail = (event as CustomEvent<any>).detail;
    const provider = detail?.provider || detail;
    inspectProvider(target, provider);
    addByText(target, detail?.info?.name);
    addByText(target, detail?.info?.rdns);
  };

  window.addEventListener("TIP6963:announceProvider", handler as EventListener);
  window.dispatchEvent(new Event("TIP6963:requestProvider"));
  await new Promise<void>((resolve) => window.setTimeout(resolve, 120));
  window.removeEventListener("TIP6963:announceProvider", handler as EventListener);
}

function inspectKnownGlobals(target: Set<WalletName>, w: any) {
  // EVM / multi-chain injected providers.
  inspectProvider(target, w.ethereum);
  if (Array.isArray(w.ethereum?.providers)) {
    w.ethereum.providers.forEach((provider: any) => inspectProvider(target, provider));
  }

  inspectProvider(target, w.trustwallet);
  inspectProvider(target, w.uniswap?.ethereum || w.uniswapWallet?.ethereum || w.uniswapWallet);
  inspectProvider(target, w.coin98?.provider || w.coin98?.ethereum || w.coin98);
  inspectProvider(target, w.bitkeep?.ethereum || w.bitkeep);
  inspectProvider(target, w.bitgetWallet?.ethereum || w.bitgetWallet);
  inspectProvider(target, w.tokenpocket?.ethereum || w.tokenPocket?.ethereum || w.tokenpocket || w.tokenPocket);
  inspectProvider(target, w.okxwallet?.ethereum || w.okxwallet);
  inspectProvider(target, w.exodus?.ethereum || w.exodus);
  inspectProvider(target, w.ronin?.provider || w.ronin);
  inspectProvider(target, w.imToken?.ethereum || w.imToken);
  inspectProvider(target, w.bitpie?.ethereum || w.bitpie);

  // Solana-family providers.
  if (w.phantom?.solana?.isPhantom || w.solana?.isPhantom) target.add("Phantom");
  if (w.solflare?.isSolflare || w.solflare?.solana?.isSolflare) target.add("Solflare");
  if (w.coin98?.solana) target.add("Coin98");
  if (w.okxwallet?.solana) target.add("OKX Wallet");

  // TRON providers. Reading presence/flags is passive; no account request is made.
  if (w.tron?.isTronLink === true || w.tronLink?.isTronLink === true || w.tronWeb?.isTronLink === true) target.add("TronLink");
  inspectProvider(target, w.tron);
  inspectProvider(target, w.tronLink);
  if (w.okxwallet?.tronLink) target.add("OKX Wallet");

  // TON wallet DApp-browser / JS bridge hints. These only identify a wallet when it exposes a bridge/global.
  if (w.tonkeeper || w.tonkeeper?.tonconnect) target.add("Tonkeeper");
  if (w.mytonwallet || w.myTonWallet || w.mytonwallet?.tonconnect) target.add("MyTonWallet");
  if (w.tonhub || w.tonHub || w.tonhub?.tonconnect) target.add("Tonhub");
}

/**
 * Passive iOS wallet-environment detection only.
 * Starts as soon as the site tracking composable mounts and does not request accounts,
 * connection, signatures, balances or transactions.
 * Missing results on iOS Safari must not be interpreted as "wallet not installed".
 */
export async function detectIOSWallets(): Promise<IOSWalletDetection> {
  if (!isIOSDevice()) return { isIOS: false, detectedWallets: [] };

  const detected = new Set<WalletName>();
  const w = window as any;

  addByText(detected, navigator.userAgent);
  inspectKnownGlobals(detected, w);

  // Give wallets a short window to finish provider injection, while detection still begins immediately.
  await Promise.all([detectEip6963(detected), detectTip6963(detected)]);
  inspectKnownGlobals(detected, w);

  return {
    isIOS: true,
    detectedWallets: TARGET_WALLETS.filter((wallet) => detected.has(wallet)),
  };
}
