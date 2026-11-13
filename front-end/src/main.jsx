import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App.jsx";
import LocationSearchAnchor from "./components/LocationSearchAnchor.jsx";
import "./index.css";
import "./motion.css";
import "./responsive.css";
import AppAtmosphere from "./components/recommendations/DiscoverLights.jsx";

ReactDOM.createRoot(document.getElementById("root")).render(
  <BrowserRouter>
    <AppAtmosphere>
      <App />
      <LocationSearchAnchor />
    </AppAtmosphere>
  </BrowserRouter>,
);
