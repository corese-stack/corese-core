function setFavicon(e) {
  const dark = e.matches;
  const favicon = document.getElementById("favicon");
  if (favicon) {
    favicon.href = dark
      ? "_static/logo/corese-core_doc_fav_dark.svg"
      : "_static/logo/corese-core_doc_fav_light.svg";
  }
}

const mql = window.matchMedia("(prefers-color-scheme: dark)");
mql.addEventListener("change", setFavicon);
