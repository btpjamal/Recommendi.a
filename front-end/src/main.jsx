import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App.jsx";
import "./index.css";
import "./motion.css";
import AppAtmosphere from "./components/recommendations/DiscoverLights.jsx";

ReactDOM.createRoot(document.getElementById("root")).render(
  <BrowserRouter>
    <AppAtmosphere>
      <App />
    </AppAtmosphere>
  </BrowserRouter>,
);
