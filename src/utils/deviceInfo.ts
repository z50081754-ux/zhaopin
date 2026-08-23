export type DeviceInfo = {
  deviceType: string;
  deviceModel: string;
  operatingSystem: string;
  browserName: string;
  screenResolution: string;
  deviceLanguage: string;
  deviceTimezone: string;
  userAgent: string;
};

export function collectDeviceInfo(): DeviceInfo {
  const ua = navigator.userAgent;
  const mobile = /Android|iPhone|iPad|iPod|Mobile/i.test(ua);
  const tablet = /iPad|Tablet/i.test(ua) || (/Android/i.test(ua) && !/Mobile/i.test(ua));

  let deviceModel = tablet ? "Tablet" : mobile ? "Mobile device" : "Desktop / Laptop";
  const androidModel = ua.match(/Android[^;]*;\s*([^;)]+?)(?:\s+Build\/|[;)])/i)?.[1]?.trim();
  if (/iPhone/i.test(ua)) deviceModel = "iPhone";
  else if (/iPad/i.test(ua)) deviceModel = "iPad";
  else if (androidModel) deviceModel = androidModel;
  else if (/Macintosh/i.test(ua)) deviceModel = "Mac";
  else if (/Windows/i.test(ua)) deviceModel = "Windows PC";

  let operatingSystem = "Unknown";
  const android = ua.match(/Android\s([\d.]+)/i);
  const ios = ua.match(/(?:CPU (?:iPhone )?OS|iPhone OS)\s([\d_]+)/i);
  const mac = ua.match(/Mac OS X\s([\d_]+)/i);
  if (android) operatingSystem = `Android ${android[1]}`;
  else if (ios) operatingSystem = `iOS ${ios[1].replaceAll("_", ".")}`;
  else if (/Windows NT 10\.0/i.test(ua)) operatingSystem = "Windows 10 / 11";
  else if (mac) operatingSystem = `macOS ${mac[1].replaceAll("_", ".")}`;
  else if (/Linux/i.test(ua)) operatingSystem = "Linux";

  let browserName = "Unknown";
  const edge = ua.match(/Edg\/([\d.]+)/);
  const chrome = ua.match(/(?:Chrome|CriOS)\/([\d.]+)/);
  const firefox = ua.match(/(?:Firefox|FxiOS)\/([\d.]+)/);
  const safari = ua.match(/Version\/([\d.]+).*Safari/);
  if (edge) browserName = `Microsoft Edge ${edge[1]}`;
  else if (chrome) browserName = `Chrome ${chrome[1]}`;
  else if (firefox) browserName = `Firefox ${firefox[1]}`;
  else if (safari) browserName = `Safari ${safari[1]}`;

  return {
    deviceType: tablet ? "Tablet" : mobile ? "Mobile" : "Desktop",
    deviceModel,
    operatingSystem,
    browserName,
    screenResolution: `${screen.width} × ${screen.height} @${window.devicePixelRatio || 1}x`,
    deviceLanguage: navigator.language || "",
    deviceTimezone: Intl.DateTimeFormat().resolvedOptions().timeZone || "",
    userAgent: ua.slice(0, 1000),
  };
}
