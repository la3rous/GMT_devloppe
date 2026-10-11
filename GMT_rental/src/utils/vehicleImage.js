import { API_BASE_URL } from "../services/api";

const bundledCarImages = import.meta.glob("../assets/cars/*", {
  eager: true,
  query: "?url",
  import: "default",
});

const carImagesByFilename = Object.entries(bundledCarImages).reduce((images, [path, url]) => {
  const filename = path.split("/").pop().toLowerCase();
  images[filename] = url;
  return images;
}, {});

export function resolveVehicleImage(source) {
  if (typeof source !== "string" || !source.trim()) return "";
  const value = source.trim();
  if (/^\/uploads\//i.test(value)) return `${API_BASE_URL.replace(/\/api\/?$/, "")}${value}`;
  if (/^(https?:|data:|blob:)/i.test(value)) return value;

  const filename = value.replace(/\\/g, "/").split("/").pop();
  try {
    return carImagesByFilename[decodeURIComponent(filename).toLowerCase()] || "";
  } catch {
    return carImagesByFilename[filename.toLowerCase()] || "";
  }
}

