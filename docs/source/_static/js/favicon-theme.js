function setFaviconForSystemTheme(e) {
  const prefersDark = e.matches;
  const favicon = document.getElementById("favicon");

  if (favicon) {
    favicon.href = prefersDark
      ? "_static/logo/corese-core_doc_fav_dark.svg"
      : "_static/logo/corese-core_doc_fav_light.svg";
  }
}

// Initialisation
const matcher = window.matchMedia("(prefers-color-scheme: dark)");
setFaviconForSystemTheme(matcher);

// Écoute des changements de thème système
matcher.addEventListener("change", setFaviconForSystemTheme);
