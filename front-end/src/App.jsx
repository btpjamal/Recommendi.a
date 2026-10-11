import { Routes, Route, Navigate } from "react-router";
import Login from "./pages/Login";
import Register from "./pages/Register";
import Profile from "./pages/Profile";
import Interests from "./pages/Interests";
import Recommendations from "./pages/Recommendations";
import Feedbacks from "./pages/Feedbacks";
import RecommendationDetails from "./pages/RecommendationDetails.jsx";
import Onboarding from "./pages/Onboarding";
import GeneralPlan from "./pages/GeneralPlan.jsx";
import MyProfile from "./pages/MyProfile";
import Community from "./pages/Community";
import CommunityDetails from "./pages/CommunityDetails";
import AISettings from "./pages/AISettings";
import AdminRoute from "./components/AdminRoute";
import AdminDashboard from "./pages/AdminDashboard";
import AdminLayout from "./components/AdminLayout";
import AdminSuggestions from "./pages/AdminSuggestions";
import SuggestHobby from "./pages/SuggestHobby";

export default function App() {
  return (
      <Routes>
        <Route path="/" element={<Navigate to="/login" />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/interests" element={<Interests />} />
        <Route path="/recommendations" element={<Recommendations />} />
        <Route path="/feedbacks" element={<Feedbacks />} />
        <Route path="/recommendations/:hobbyId" element={<RecommendationDetails />} />
        <Route path="/onboarding" element={<Onboarding />} />
        <Route path="/general-plan" element={<GeneralPlan />} />
        <Route path="/myProfile" element={<MyProfile />} />
        <Route path="/community" element={<Community />} />
        <Route path="/community/communities/:communityId" element={<CommunityDetails />} />
        <Route path="/ai-settings" element={<AISettings />} />
        <Route path="/suggest-hobby" element={<SuggestHobby />} />

            <Route element={<AdminRoute />}>
                  <Route path="/admin" element={<AdminLayout />}>
                        <Route index element={<AdminDashboard />} />

                        <Route
                            path="suggestions"
                            element={<AdminSuggestions />}
                        />

                        <Route
                            path="hobbies"
                            element={<h2>Catálogo de hobbies em desenvolvimento</h2>}
                        />

                        <Route
                            path="location-rules"
                            element={<h2>Regras geográficas em desenvolvimento</h2>}
                        />
                  </Route>
            </Route>


      </Routes>
  );
}